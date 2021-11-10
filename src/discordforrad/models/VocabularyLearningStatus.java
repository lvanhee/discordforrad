package discordforrad.models;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import cachingutils.Cache;
import cachingutils.FileBasedStringSetCache;
import discordforrad.AddStringResultContext;
import discordforrad.LanguageCode;
import discordforrad.Main;
import discordforrad.Translator;
import discordforrad.inputUtils.TextInputUtils;
import discordforrad.models.language.Dictionnary;
import discordforrad.models.language.LanguageText;
import discordforrad.models.language.LanguageWord;
import discordforrad.models.language.SuccessfulTranslationDescription;
import discordforrad.models.language.TranslationDescription;

public class VocabularyLearningStatus {
	private static final Path FILEPATH = Paths.get(Main.ROOT_DATABASE+"learned_words.txt");
	private static final Path FORBIDDEN_FILEPATH = Paths.get(Main.ROOT_DATABASE+"disregarded_words.txt");
	
	private static final int MAX_LEARNED = 10;
	private static final int SHORT_TERM_NUMBER_OF_REPEAT = 2;
	private static final int MID_TERM_NUMBER_OF_REPEAT = 7;
	private static final int DEFAULT_MID_TERM_WORDS_TO_LEARN_EVERY_SESSION = 10;
	private static final int DEFAULT_LONG_TERM_WORDS_TO_LEARN_EVERY_SESSION = 10;
	private final Map<LanguageWord, Integer> successfulLearningPerWord;
	private final Map<LanguageWord, LocalDateTime> timeLastAttempt;
	
	//These words correspond to oddities that are not worth being learned about
	private final Set<LanguageWord> forbiddenWords = 
			FileBasedStringSetCache.loadCache(FORBIDDEN_FILEPATH,
			LanguageWord::parse,
			x->x.toString());

	public enum LearningStatus {IN_ACQUISITION, IN_CONSOLIDATION, LEARNED}


	public VocabularyLearningStatus(
			Map<LanguageWord, Integer> m, 
			Map<LanguageWord, LocalDateTime> m2)
	{
		//assert(isAllWordsInInputCorrect(m));
		
		this.successfulLearningPerWord = m;
		this.timeLastAttempt = m2;
		
		updateFile();
	}


	private boolean isAllWordsInInputCorrect(Map<LanguageWord, Integer> m) {
		Set<LanguageWord>toRemove =
		m.keySet().parallelStream().sorted((x,y)->x.toString().compareTo(y.toString()))
		.filter(lw->!Dictionnary.isInDictionnariesWithCrosscheck(lw)).collect(Collectors.toSet());
		toRemove.addAll(forbiddenWords);
		return toRemove.stream().anyMatch(x->m.containsKey(x));
	}


	public static VocabularyLearningStatus loadFromFile() throws IOException {
		Map<LanguageWord, Integer> m = new HashMap<>();
		Map<LanguageWord, LocalDateTime> m2 = new HashMap<>();

		Charset cs = Charset.forName("ISO-8859-1");
		for(String line: Files.lines(FILEPATH,cs).collect(Collectors.toSet()))
		{
			if(line.isEmpty())continue;
			if(line.contains("ã"))continue;
			if(line.contains("¬"))continue;
			if(line.contains("£"))continue;

			String[] split = line.split(";");
			String word = split[0];
			LanguageCode lc = LanguageCode.valueOf(split[1]);
			int number = Integer.parseInt(split[2]);
			LocalDateTime time = LocalDateTime.parse(split[3]);
			LanguageWord lw = new LanguageWord(lc, word);

			m.put(lw, number);
			m2.put(lw, time);
		}
		return new VocabularyLearningStatus(m, m2);
	}

	private void addWordToSearch(LanguageWord lw, AddStringResultContext c, int recursivityDepth) {
		if(successfulLearningPerWord.containsKey(lw))return;

		if(!Dictionnary.isInDictionnaries(lw)) { 
			System.out.println("Not in dictionnaries:"+lw);
			return;
		}

		c.addResult(lw);
		addNewWordInDatabase(lw);

		if(recursivityDepth>0)
		{
			Set<TranslationDescription> allTranslations = 
					Translator.getAllTranslations(
							LanguageWord.newInstance(
									lw.getWord().replaceAll("_", " "), 
									lw.getCode()), LanguageCode.otherLanguage(lw.getCode()));
			
			
			
			for(TranslationDescription translation: allTranslations)
				addFreeString(
						((SuccessfulTranslationDescription)translation).getTranslatedText().getText(),
						c, false,recursivityDepth-1);
		}
	}


	private void addNewWordInDatabase(LanguageWord lw) {
		if(forbiddenWords.contains(lw))return;
		
		successfulLearningPerWord.put(lw, 0);
		timeLastAttempt.put(lw, LocalDateTime.MIN);
	}


	private synchronized void updateFile() {
		String res = "";
		for(LanguageWord s: successfulLearningPerWord.keySet())
			res+=s.getWord()+";"+s.getCode()+";"+successfulLearningPerWord.get(s)+";"+timeLastAttempt.get(s)+"\n";

		Charset cs = Charset.forName("ISO-8859-1");
		try {
			Files.writeString(FILEPATH, res,cs);
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error();
		}
	}


	public String toString() {return successfulLearningPerWord.toString();}



	/*public String overlay(String string) {
		String res = "";

		for(String s: string.split(" "))
		{
			String sCleared = clearOfSymbols(s);
			if(is)

		}

	}*/


	public Set<LanguageWord> getAllWords() {
		return successfulLearningPerWord.keySet();
	}


	public int getNumberOfSuccessLearning(LanguageWord s) {
		if(!successfulLearningPerWord.containsKey(s))return 0;
		return successfulLearningPerWord.get(s);
	}


	public LocalDateTime getLastSuccessOf(LanguageWord s) {
		if(!timeLastAttempt.containsKey(s))
			return LocalDateTime.MIN;
		return timeLastAttempt.get(s);
	}


	public void incrementSuccess(LanguageWord lastWordAsked) {
		if(!Dictionnary.isInDictionnaries(lastWordAsked))
			throw new Error();
		if(forbiddenWords.contains(lastWordAsked))
			throw new Error();
		timeLastAttempt.put(lastWordAsked, LocalDateTime.now());
		if(!successfulLearningPerWord.containsKey(lastWordAsked))successfulLearningPerWord.put(lastWordAsked, 0);
		successfulLearningPerWord.put(lastWordAsked,successfulLearningPerWord.get(lastWordAsked)+1);
		updateFile();
	}


	private boolean contains(LanguageWord lw) {
		return successfulLearningPerWord.containsKey(lw);
	}


	public void decrementSuccessUpToZero(LanguageWord lastWordAsked) throws IOException {
		if(!Dictionnary.isInDictionnaries(lastWordAsked))
			throw new Error();
		if(forbiddenWords.contains(lastWordAsked))
			throw new Error();
		timeLastAttempt.put(lastWordAsked, LocalDateTime.now());
		if(!successfulLearningPerWord.containsKey(lastWordAsked))
			successfulLearningPerWord.put(lastWordAsked, 0);
		successfulLearningPerWord.put(lastWordAsked,successfulLearningPerWord.get(lastWordAsked)-1);
		if(successfulLearningPerWord.get(lastWordAsked)<0)
			successfulLearningPerWord.put(lastWordAsked,0);
		updateFile();
	}


	public Set<LanguageWord> getAllValidShortTermWords() {
		return successfulLearningPerWord.keySet().stream()
				.filter(x->isEarlyPhaseWord(x))
				.filter(x->!isForbiddenWord(x))
				.collect(Collectors.toSet());
	}


	public Set<LanguageWord> getAllMidTermWords() {
		return successfulLearningPerWord.keySet().stream().filter(x->
		isMidTermWord(x) ).collect(Collectors.toSet());
	}


	public Set<LanguageWord> getAllLongTermWords() {
		Set<LanguageWord> res = successfulLearningPerWord.keySet().stream()
				.filter(x->isLongTermWord(x))
				.filter(x->!isForbiddenWord(x))
				.collect(Collectors.toSet()); 
		return res;
	}


	public boolean isEarlyPhaseWord(LanguageWord newInstance) {
		return Dictionnary.isInDictionnaries(newInstance)&&
				getNumberOfSuccessLearning(newInstance)<SHORT_TERM_NUMBER_OF_REPEAT;
	}

	public boolean isMidTermWord(LanguageWord newInstance) {
		return Dictionnary.isInDictionnaries(newInstance)&&
				getNumberOfSuccessLearning(newInstance)<MID_TERM_NUMBER_OF_REPEAT &&
				! isEarlyPhaseWord(newInstance);
	}

	public boolean isLongTermWord(LanguageWord newInstance) {
		return Dictionnary.isInDictionnaries(newInstance)&&!isEarlyPhaseWord(newInstance)&&!isMidTermWord(newInstance);
	}


	public void strongIncrement(LanguageWord lastWordAsked) {
		while(!isLongTermWord(lastWordAsked)) incrementSuccess(lastWordAsked);
	}


	public boolean isExposableForLearning(LanguageWord s) {
		return LearningModel.isTimeForLearning(s, this) &&!isForbiddenWord(s)&&Dictionnary.isInDictionnaries(s);
	}


	public List<LanguageWord> getStandardSessionMidTermWordsToLearn() {
		int nbLearnableMidTerm = getAllMidTermWords().size();
		int nbToProcess = Math.max(DEFAULT_MID_TERM_WORDS_TO_LEARN_EVERY_SESSION, nbLearnableMidTerm/10);
		int nbToPick = 
				Math.min(nbToProcess,
						getLearnableMidTermWords().size());


		List<LanguageWord> learnableMidTerm = getLearnableMidTermWords().stream().collect(Collectors.toList());
		Collections.shuffle(learnableMidTerm);
		return learnableMidTerm.subList(0, nbToPick);
	}


	public Set<LanguageWord> getLearnableMidTermWords() {
		return getAllMidTermWords().stream().filter(x->LearningModel.isTimeForLearning(x, this)).collect(Collectors.toSet());
	}


	public List<LanguageWord> getStandardSessionLongTermWordsToLearn() {
		int nbToPick = 
				Math.min(Math.max(DEFAULT_LONG_TERM_WORDS_TO_LEARN_EVERY_SESSION, getLearnableLongTermWords().size()/10),
						getLearnableLongTermWords().size());


		List<LanguageWord> learnableMidTerm = getLearnableLongTermWords().stream().collect(Collectors.toList());
		Collections.shuffle(learnableMidTerm);
		return learnableMidTerm.subList(0, nbToPick);
	}


	private Set<LanguageWord> getLearnableLongTermWords() {
		Set<LanguageWord> res = getAllLongTermWords().stream().filter(x->LearningModel.isTimeForLearning(x, this)).collect(Collectors.toSet()); 
		return res;
	}


	public static long getNumberOfKnownWordsFromWordset(VocabularyLearningStatus vls, LanguageText newInstance) {
		return newInstance.getSetOfValidWords().stream().filter(x->vls.getAllLongTermWords().contains(x)).count();
	}


	public static long getNumberOfStudiedWordsFromWordset(VocabularyLearningStatus vls, LanguageText newInstance) {
		return newInstance.getSetOfValidWords().stream().filter(x->vls.getAllWords().contains(x)).count();
	}


	public LearningStatus getLearningStatus(LanguageWord x) {
		if(isEarlyPhaseWord(x))
			return LearningStatus.IN_ACQUISITION;
		if(isMidTermWord(x))
			return LearningStatus.IN_CONSOLIDATION;
		if(isLongTermWord(x))
			return LearningStatus.LEARNED;

		throw new Error();
	}

	public static Map<LanguageText, Integer> getTranslationsOfWordsIKnowAndTheirFrequencyIn(
			VocabularyLearningStatus vls,
			LanguageCode originalLanguageCode)
	{
		Map<LanguageText, Integer> res = new HashMap<>();
		Set<LanguageWord> originalLanguageWords = 
				new TreeSet<>((x,y)->x.toString().compareTo(y.toString()));
		originalLanguageWords.addAll(
				VocabularyLearningStatus.getWordsKnownIn(vls,originalLanguageCode));
		/*.stream().sorted(
				(x,y)->x.toString().compareTo(y.toString())).collect(Collectors.toSet());*/
		//	System.out.println(originalLanguageWords);
		for(LanguageWord lw:originalLanguageWords)
		{
			//System.out.println(lw);
			for(LanguageText translationOfLw: Translator.getAllTranslations(lw,LanguageCode.otherLanguage(originalLanguageCode)))
			{
				//	System.out.println(lw+":"+translationOfLw);
				if(!res.containsKey(translationOfLw))
					res.put(translationOfLw, 1);
				else res.put(translationOfLw, res.get(translationOfLw)+1);
			}
		}

		return res;
	}


	public static Set<LanguageWord> getWordsKnownIn(VocabularyLearningStatus vls, LanguageCode originalLanguageCode) {
		return vls.getAllLongTermWords().stream()
				.filter(x->x.getCode().equals(originalLanguageCode))
				.collect(Collectors.toSet());
	}


	public void forbidWord(LanguageWord lw) {
		forbiddenWords.add(lw);
		successfulLearningPerWord.remove(lw);
		timeLastAttempt.remove(lw);
	}


	public boolean isForbiddenWord(LanguageWord lw) {
		return forbiddenWords.contains(lw);
	}


	public Set<LanguageWord> getAllExposableShortTermWords() {
		return getAllValidShortTermWords().stream().filter(x->isExposableForLearning(x)).collect(Collectors.toSet());
	}



}
