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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import discordforrad.AddStringResultContext;
import discordforrad.LanguageCode;
import discordforrad.Translator;
import discordforrad.inputUtils.TextInputUtils;
import discordforrad.models.language.Dictionnary;
import discordforrad.models.language.LanguageText;
import discordforrad.models.language.LanguageWord;

public class VocabularyLearningStatus {
	private static final Path FILEPATH = Paths.get("data/learned_words.txt");
	private static final int MAX_LEARNED = 10;
	private static final int SHORT_TERM_NUMBER_OF_REPEAT = 2;
	private static final int MID_TERM_NUMBER_OF_REPEAT = 7;
	private static final int DEFAULT_MID_TERM_WORDS_TO_LEARN_EVERY_SESSION = 10;
	private static final int DEFAULT_LONG_TERM_WORDS_TO_LEARN_EVERY_SESSION = 10;
	private final Map<LanguageWord, Integer> successfulLearningPerWord;
	private final Map<LanguageWord, LocalDateTime> timeLastAttempt;


	public VocabularyLearningStatus(
			Map<LanguageWord, Integer> m, 
			Map<LanguageWord, LocalDateTime> m2)
	{
		this.successfulLearningPerWord = m;
		this.timeLastAttempt = m2;
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


	public void addFreeString(String lt, AddStringResultContext c, boolean isOriginalString, int recursivityDepth) {
		Set<String> consideredWords = new HashSet<>();
		if(!isOriginalString) consideredWords.add(lt);
		else consideredWords = TextInputUtils.toListOfWords(lt).stream().collect(Collectors.toSet());

		Set<LanguageWord> wordsToSearch = new HashSet<>();

		for(String word : consideredWords)
			for(LanguageCode lc: LanguageCode.values())
				if(!word.isEmpty())
					wordsToSearch.add(new LanguageWord(lc, word));
		
		AtomicInteger totalProcessed = new AtomicInteger();
		
		final Set<LanguageWord> finalWordsToSearch = wordsToSearch.stream().filter(x->!successfulLearningPerWord.containsKey(x)).collect(Collectors.toSet());

		finalWordsToSearch.stream()//.filter(x->!successfulLearningPerWord.containsKey(x))
		.forEach(lw ->
		{
			System.out.println(totalProcessed+"/"+finalWordsToSearch.size());
			totalProcessed.incrementAndGet();
			addWordToSearch(lw, c, recursivityDepth);
		}
				);

		//if(isOriginalString)
		updateFile();
	}




	private void addWordToSearch(LanguageWord lw, AddStringResultContext c, int recursivityDepth) {
		if(successfulLearningPerWord.containsKey(lw))return;

		if(!Dictionnary.isInDictionnaries(lw)) { 
			System.out.println("Not in dictionnaries:"+lw);
			return;
		}

		c.addResult(lw);
		successfulLearningPerWord.put(lw, 0);
		timeLastAttempt.put(lw, LocalDateTime.MIN);

		if(recursivityDepth>0)
		{
		for(String translation: Translator.getTranslation(lw.getWord().replaceAll("_", " "), 
				lw.getCode(), LanguageCode.otherLanguage(lw.getCode())))
			addFreeString(
					translation,
					c, false,recursivityDepth-1);
		}
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


	



	/*public double vocabularyKnownRatio() {
		return (double)successfulLearningPerWordEN.values().parallelStream().filter(x->x>MAX_LEARNED)
				.count()/successfulLearningPerWordEN.size();
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
		timeLastAttempt.put(lastWordAsked, LocalDateTime.now());
		if(!successfulLearningPerWord.containsKey(lastWordAsked))successfulLearningPerWord.put(lastWordAsked, 0);
		successfulLearningPerWord.put(lastWordAsked,successfulLearningPerWord.get(lastWordAsked)+1);
		updateFile();
	}


	public void decrementSuccessUpToZero(LanguageWord lastWordAsked) throws IOException {
		timeLastAttempt.put(lastWordAsked, LocalDateTime.now());
		if(!successfulLearningPerWord.containsKey(lastWordAsked))
			successfulLearningPerWord.put(lastWordAsked, 0);
		successfulLearningPerWord.put(lastWordAsked,successfulLearningPerWord.get(lastWordAsked)-1);
		if(successfulLearningPerWord.get(lastWordAsked)<0)
			successfulLearningPerWord.put(lastWordAsked,0);
		updateFile();
	}


	public Set<LanguageWord> getAllShortTermWords() {
		return successfulLearningPerWord.keySet().stream().filter(x->
		isShortTermWord(x)).collect(Collectors.toSet());
	}


	public Set<LanguageWord> getAllMidTermWords() {
		return successfulLearningPerWord.keySet().stream().filter(x->
		isMidTermWord(x) ).collect(Collectors.toSet());
	}


	public Set<LanguageWord> getAllLongTermWords() {
		Set<LanguageWord> res = successfulLearningPerWord.keySet().stream()
				.filter(x->isLongTermWord(x)).collect(Collectors.toSet()); 
		return res;
	}


	public boolean isShortTermWord(LanguageWord newInstance) {
		return getNumberOfSuccessLearning(newInstance)<SHORT_TERM_NUMBER_OF_REPEAT;
	}
	
	public boolean isMidTermWord(LanguageWord newInstance) {
		return getNumberOfSuccessLearning(newInstance)<MID_TERM_NUMBER_OF_REPEAT &&
				! isShortTermWord(newInstance);
	}
	
	public boolean isLongTermWord(LanguageWord newInstance) {
		return !isShortTermWord(newInstance)&&!isMidTermWord(newInstance);
	}


	public void strongIncrement(LanguageWord lastWordAsked) {
		while(!isLongTermWord(lastWordAsked)) incrementSuccess(lastWordAsked);
	}


	public boolean isReadyToBeExposedAgain(LanguageWord s) {
		return LearningModel.isTimeForLearning(s, this);
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






}
