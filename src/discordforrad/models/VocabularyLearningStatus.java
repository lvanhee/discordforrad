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
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import cachingutils.Cache;
import cachingutils.FileBasedStringSetCache;
import discordforrad.AddStringResultContext;
import discordforrad.Main;
import discordforrad.inputUtils.TextInputUtils;
import discordforrad.inputUtils.UserLearningTextManager;
import discordforrad.models.language.Dictionnary;
import discordforrad.models.language.LanguageText;
import discordforrad.models.language.LanguageWord;
import discordforrad.models.language.SuccessfulTranslationDescription;
import discordforrad.models.language.WordDescription;
import discordforrad.models.language.ResultOfTranslationAttempt;
import discordforrad.translation.Translator;

public class VocabularyLearningStatus {
	private static final Path FILEPATH = Paths.get(Main.ROOT_DATABASE+"learned_words.txt");
	private static final Path FORBIDDEN_FILEPATH = 
			Paths.get(Main.ROOT_DATABASE+"disregarded_words.txt");
	
	private static final int MAX_LEARNED = 10;
	private static final int SHORT_TERM_NUMBER_OF_REPEAT = 2;
	public static final int NB_SUCCESSES_FOR_A_WORD_TO_BE_CONSIDERED_AS_LEARNED = 7;
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
	/*	if(successfulLearningPerWord.containsKey(lw))return;

		if(!Dictionnary.isInDictionnaries(lw)) { 
			System.out.println("Not in dictionnaries:"+lw);
			return;
		}

		c.addResult(lw);
		addNewWordInDatabase(lw);

		if(recursivityDepth>0)
		{
			Set<ResultOfTranslationAttempt> allTranslations = 
					Translator.getResultOfTranslationAttempts(
							LanguageWord.newInstance(
									lw.getWord().replaceAll("_", " "), 
									lw.getCode()), LanguageCode.otherLanguage(lw.getCode()));
			
			
			
			for(ResultOfTranslationAttempt translation: allTranslations)
				addFreeString(
						((SuccessfulTranslationDescription)translation).getTranslatedText().getText(),
						c, false,recursivityDepth-1);
		}*/
		throw new Error();
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
		if(!Dictionnary.isInDictionnariesWithCrosscheck(lastWordAsked))
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
		assert(Dictionnary.isInDictionnariesWithCrosscheck(lastWordAsked));
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
//		System.out.println(successfulLearningPerWord.get();
		Set<LanguageWord> res = successfulLearningPerWord.keySet().stream().filter(x->
		isMidTermWord(x) ).collect(Collectors.toSet()); 
		return res;
	}
	
	private  Set<LanguageWord> allLongTermWordsCache = null;
	public Set<LanguageWord> getAllLongTermWords() {
		if(allLongTermWordsCache!=null)
			return allLongTermWordsCache;
		
		allLongTermWordsCache = successfulLearningPerWord.keySet().stream()
				.filter(x->isLearnedWordWord(x))
				.filter(x->!isForbiddenWord(x))
				.collect(Collectors.toSet()); 
		return allLongTermWordsCache;
	}


	public boolean isEarlyPhaseWord(LanguageWord newInstance) {
		return Dictionnary.isInDictionnariesWithCrosscheck(newInstance)&&
				getNumberOfSuccessLearning(newInstance)<SHORT_TERM_NUMBER_OF_REPEAT;
	}

	public boolean isMidTermWord(LanguageWord newInstance) {
	/*	if(newInstance.equals(LanguageWord.newInstance("verkligen", LanguageCode.SV)))
			System.out.println();*/
		boolean isInDictionnaries = Dictionnary.isInDictionnariesWithCrosscheck(newInstance);
		boolean res = isInDictionnaries&&
				getNumberOfSuccessLearning(newInstance)<NB_SUCCESSES_FOR_A_WORD_TO_BE_CONSIDERED_AS_LEARNED &&
				! isEarlyPhaseWord(newInstance); 
		return res;
	}

	public boolean isLearnedWordWord(LanguageWord newInstance) {
		return Dictionnary.isInDictionnariesWithCrosscheck(newInstance)&&!isEarlyPhaseWord(newInstance)&&!isMidTermWord(newInstance);
	}


	public void strongIncrement(LanguageWord lastWordAsked) {
		while(!isLearnedWordWord(lastWordAsked)) incrementSuccess(lastWordAsked);
	}


	public boolean isExposableForLearning(LanguageWord s) {
		return LearningModel.isTimeForLearning(s, this) &&!isForbiddenWord(s)&&Dictionnary.isInDictionnariesWithCrosscheck(s);
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
		if(isLearnedWordWord(x))
			return LearningStatus.LEARNED;

		throw new Error();
	}

	public static Map<LanguageText, Integer> getTranslationsOfWordsIKnowAndTheirFrequencyIn(
			VocabularyLearningStatus vls,
			LanguageCode originalLanguageCode)
	{
		/*Map<LanguageText, Integer> res = new HashMap<>();
		Set<LanguageWord> originalLanguageWords = 
				new TreeSet<>((x,y)->x.toString().compareTo(y.toString()));
		originalLanguageWords.addAll(
				VocabularyLearningStatus.getWordsKnownIn(vls,originalLanguageCode));
		//.stream().sorted(
		//		(x,y)->x.toString().compareTo(y.toString())).collect(Collectors.toSet());
		//	System.out.println(originalLanguageWords);
		for(LanguageWord lw:originalLanguageWords)
		{
			//System.out.println(lw);
			for(LanguageText translationOfLw: Translator.getResultOfTranslationAttempts(lw,LanguageCode.otherLanguage(originalLanguageCode)))
			{
				//	System.out.println(lw+":"+translationOfLw);
				if(!res.containsKey(translationOfLw))
					res.put(translationOfLw, 1);
				else res.put(translationOfLw, res.get(translationOfLw)+1);
			}
		}

		return res;*/
		throw new Error();
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


	public boolean isWordAboutToBeRemovedOutOfLearnedWordsIfNotRecalledCorrectly(LanguageWord lw) {
		return getNumberOfSuccessLearning(lw)==NB_SUCCESSES_FOR_A_WORD_TO_BE_CONSIDERED_AS_LEARNED;
	}


	public int getIncrementScoreIfSetAsLearned(LanguageWord lw) {
		if(isLearnedWordWord(lw)) return 0;
		else return NB_SUCCESSES_FOR_A_WORD_TO_BE_CONSIDERED_AS_LEARNED-getNumberOfSuccessLearning(lw);
	}


	public boolean isLanguageDominatingTheOther(LanguageCode sv) {
		long currentLanguageProportion = getAllLongTermWords().stream().filter(x->x.getCode().equals(sv)).count();
		long otherLanguageProportion = getAllLongTermWords().stream().filter(x->x.getCode().equals(LanguageCode.otherLanguage(sv))).count(); 
		
		return otherLanguageProportion < currentLanguageProportion*0.75;
	}
	
	
	private Map<LanguageCode, Double> proportionPerLanguageCodeCache = null;
	
	public Map<LanguageCode, Double> getProportionOfMasteryPerLanguageCode() {
		if(proportionPerLanguageCodeCache!=null)
			return proportionPerLanguageCodeCache;
		Set<LanguageCode> lc = getAllLongTermWords().stream().map(x->x.getCode()).collect(Collectors.toSet());
		
		Map<LanguageCode, Long> countPerLanguageCode =
				lc.stream().collect(Collectors.toMap(Function.identity(), x->getAllLongTermWords().stream().filter(y->y.getCode().equals(x)).count()));
		
		long total = countPerLanguageCode.values().stream().reduce(0l, (x,y)->x+y);
		
		Map<LanguageCode, Double> res = 
				countPerLanguageCode.keySet().stream().collect(Collectors.toMap(Function.identity(), x->(double)countPerLanguageCode.get(x)/(double)total));
		
		proportionPerLanguageCodeCache = res;
		return res;
	}


	public Set<LanguageWord> getAlreadyExposedWordsFrom(Set<LanguageWord> s) {
		return s.stream().filter(x->isAlreadyBeenShown(x)).collect(Collectors.toSet());
	}


	private boolean isAlreadyBeenShown(LanguageWord x) {
		return getNumberOfSuccessLearning(x)>0;
	}


	public Set<LanguageWord> getAllExposableNewLanguageWords() {
		Set<LanguageWord> allConsideredWords = 
				UserLearningTextManager.getAllPossibleLanguageWords();
		Set<LanguageWord> alternatives = allConsideredWords.stream()
				.sorted((x,y)->x.toString().compareTo(y.toString()))
				.map(x->Translator.getTranslationsOf(x))
				.reduce(new HashSet<>(), (x,y)->{x.addAll(y); return x;});
		allConsideredWords.addAll(alternatives);
		allConsideredWords.addAll(
				allConsideredWords
				.stream()
				.map(x->WordDescription.getGrundforms(x))
				.reduce(new HashSet<>(),(y,z)->{y.addAll(z); return y;})
				);
		Set<LanguageWord> allExposableNewLanguageWords =
				allConsideredWords.stream()
				.filter(x->!isForbiddenWord(x)&&isEarlyPhaseWord(x)&&isExposableForLearning(x))
				.collect(Collectors.toSet());
		
		return allExposableNewLanguageWords;
	}



}
