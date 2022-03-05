package discordforrad.models.learning.session;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import discordforrad.Main;
import discordforrad.discordmanagement.DiscordManager;
import discordforrad.discordmanagement.audio.LocalAudioDatabase;
import discordforrad.inputUtils.UserLearningTextManager;
import discordforrad.models.LanguageCode;
import discordforrad.models.LearningModel;
import discordforrad.models.VocabularyLearningStatus;
import discordforrad.models.language.Dictionnary;
import discordforrad.models.language.LanguageWord;
import discordforrad.models.language.SuccessfulTranslationDescription;
import discordforrad.models.language.ResultOfTranslationAttempt;
import discordforrad.models.language.WordDescription;
import discordforrad.models.language.ResultOfTranslationAttempt.Origin;
import discordforrad.models.language.wordnetwork.WordNetwork;
import discordforrad.models.learning.focus.ReadThroughFocus;
import discordforrad.translation.Translator;

public class Session {
	private final List<LanguageWord> shortTermWordsToTeachInThisSession=new LinkedList<>();
	private final List<LanguageWord> midTermWordsToTeachInThisSession;
	private final List<LanguageWord> longTermWordsToTeachInThisSession;
	private final boolean isFocusFullyCompleted;

	private final int nbShortToExplore;
	private final int nbMidToExplore;
	private final int nbLongToExplore;

	private int nbSuccessShort = 0;
	private int nbSuccessMid = 0;
	private int nbSuccessLong = 0;

	private LanguageWord current = null;
	private enum SML{SHORT, MIDDLE,LONG}
	private SML temporalityLastAttempt=null;
	private int nbDecreasesShort;
	private int nbDecreasesMid;
	private int nbDecreasesLong;

	private Session(ReadThroughFocus currentFocus, 
			VocabularyLearningStatus vls) {

		shortTermWordsToTeachInThisSession.clear();

		Set<LanguageWord> mandatoryWordsToConsider = new HashSet<>();
		mandatoryWordsToConsider.addAll(vls.getAlreadyExposedWordsFrom(vls.getAllExposableShortTermWords()));
		
		

		List<LanguageWord> newWordsToLearnFromCurrentFocus = currentFocus.getAllValidWordsSortedByTheirOrderOfOccurrenceInFocusTexts().stream().filter(x->vls.isEarlyPhaseWord(x)&&vls.isExposableForLearning(x))
				.collect(Collectors.toList()).subList(0, 5);
		mandatoryWordsToConsider.addAll(newWordsToLearnFromCurrentFocus);

		//List<LanguageWord> translationsOfMostFrequentWords = getTranslationsOfMostFrequentlyUsedWords(5,vls);		  
		//	mandatoryWordsToConsider.addAll(translationsOfMostFrequentWords);

		shortTermWordsToTeachInThisSession.addAll(getOptimalShortTermWords(mandatoryWordsToConsider, vls, currentFocus));

		if(Main.PRELOAD_PURGE_MODE)
			preloadPossibleWords(vls);

		//		Predicate<LanguageWord> balancingMeasureInFavorOfEn = x->x.getCode().equals(LanguageCode.EN);

		midTermWordsToTeachInThisSession = vls.getStandardSessionMidTermWordsToLearn();
		longTermWordsToTeachInThisSession = vls.getStandardSessionLongTermWordsToLearn().subList(0, 10);

		Collections.shuffle(midTermWordsToTeachInThisSession);
		Collections.shuffle(longTermWordsToTeachInThisSession);

		isFocusFullyCompleted = shortTermWordsToTeachInThisSession.isEmpty() &&
				midTermWordsToTeachInThisSession.isEmpty() &&
				longTermWordsToTeachInThisSession.isEmpty();

		nbShortToExplore = shortTermWordsToTeachInThisSession.size();
		nbMidToExplore = midTermWordsToTeachInThisSession.size();
		nbLongToExplore = longTermWordsToTeachInThisSession.size();

		fillCacheForSession(currentFocus);

		/*	Map<String, Integer> numberOfOccurrenceInUserTextPerString = ;
		return (x,y)->{
			if(numberOfOccurrenceInUserTextPerString.get(x.getWord())>numberOfOccurrenceInUserTextPerString.get(y.getWord())) return -1;
			if(numberOfOccurrenceInUserTextPerString.get(x.getWord())<numberOfOccurrenceInUserTextPerString.get(y.getWord())) return 1;
			return x.toString().compareTo(y.toString());
		};*/


	}

	private Set<LanguageWord> getOptimalShortTermWords(
			Set<LanguageWord> mandatoryWords,
			VocabularyLearningStatus vls, ReadThroughFocus currentFocus) {	
		Set<LanguageWord> allExposableNewLanguageWords = vls.getAllExposableNewLanguageWords();
		GreedyLanguageWordProposal proposal = GreedyLanguageWordProposal.newInstance();

		for(LanguageWord lw: 
			allExposableNewLanguageWords
				.stream().sorted((x,y)->x.toString().compareTo(y.toString()))
				.collect(Collectors.toList()))
		{
		//	System.out.println("\t\t\t\t\t"+lw);
			proposal.addForConsideration(lw, mandatoryWords, vls, currentFocus);
		}


		return proposal.getProposal();
	}

	private List<LanguageWord> getMostFrequentWordsFromCurrentFocus(
			VocabularyLearningStatus vls, ReadThroughFocus currentFocus, 
			SML s, int i) {
		List<LanguageWord>res = new ArrayList<LanguageWord>(i);
		int nbOfWordsFromCurrentFocus = 0;
		for(LanguageWord lw:currentFocus
				.getAllValidWordsSortedByTheirOrderOfOccurrenceInFocusTexts()
				.parallelStream()
				.filter(x->!vls.isForbiddenWord(x))
				.filter(x->vls.isEarlyPhaseWord(x))
				.filter(x->LearningModel.isTimeForLearning(x, vls))
				.collect(Collectors.toList()))
		{
			res.add(lw);
			nbOfWordsFromCurrentFocus++;
			if(nbOfWordsFromCurrentFocus>=5) break;
		}

		return res;
	}

	private Set<LanguageWord> getAllInDictionnaryWordsFromUserTexts(VocabularyLearningStatus vls) {
		return LanguageWord.toLanguageWordSet(UserLearningTextManager.getAllOccurrencesOfEveryWordEverIncludedInUserText().keySet())
				.parallelStream().filter(x->!vls.isForbiddenWord(x)&&Dictionnary.isInDictionnariesWithCrosscheck(x)).collect(Collectors.toSet());
	}

	private List<LanguageWord> getMostFrequentGrundFormOfLearnerWords(VocabularyLearningStatus vls, int numberOfWords) {
		List<LanguageWord> res = new ArrayList<LanguageWord>();

		Set<LanguageWord> allWordsFromUserTexts=getAllInDictionnaryWordsFromUserTexts(vls);

		Map<String, Integer> numberOfOccurrenceInUserTextPerString = UserLearningTextManager.getAllOccurrencesOfEveryWordEverIncludedInUserText();

		Map<LanguageWord, Integer> numberOfOccurrencesOfEveryGrundformInTextsOfConsideredWords = new HashMap<LanguageWord, Integer>();
		for(LanguageWord lw: vls.getAllLongTermWords().stream().filter(x->allWordsFromUserTexts.contains(x)).collect(Collectors.toSet()))
			for(LanguageWord grund:WordDescription.getGrundforms(lw))
			{
				if(!numberOfOccurrencesOfEveryGrundformInTextsOfConsideredWords.containsKey(grund))
					numberOfOccurrencesOfEveryGrundformInTextsOfConsideredWords.put(grund, 0);
				int nbOccurrencesInText = numberOfOccurrenceInUserTextPerString.get(lw.getWord());
				numberOfOccurrencesOfEveryGrundformInTextsOfConsideredWords.put(grund, numberOfOccurrencesOfEveryGrundformInTextsOfConsideredWords.get(grund)+nbOccurrencesInText);
			}


		Comparator<LanguageWord> mostFrequentGrundformComparator = (x,y)->{
			if(x.equals(y))return 0;
			if(!numberOfOccurrencesOfEveryGrundformInTextsOfConsideredWords.containsKey(x))return 1;
			if(!numberOfOccurrencesOfEveryGrundformInTextsOfConsideredWords.containsKey(y))return -1;
			if(numberOfOccurrencesOfEveryGrundformInTextsOfConsideredWords.get(x)>numberOfOccurrencesOfEveryGrundformInTextsOfConsideredWords.get(y)) return -1;
			if(numberOfOccurrencesOfEveryGrundformInTextsOfConsideredWords.get(x)<numberOfOccurrencesOfEveryGrundformInTextsOfConsideredWords.get(y)) return 1;
			return x.toString().compareTo(y.toString());
		};

		Set<LanguageWord> mostFrequentGrundformOfLearnedWords = new TreeSet<LanguageWord>(mostFrequentGrundformComparator);
		mostFrequentGrundformOfLearnedWords.addAll(vls.getAllExposableShortTermWords());

		int i = 0;
		for(LanguageWord lw: mostFrequentGrundformOfLearnedWords)
		{
			if(i>=numberOfWords) break;
			res.add(lw);
			i++;
		}

		return res;
	}

	private Set<LanguageWord> getAllExposableWords(VocabularyLearningStatus vls) {
		Map<String, Integer> numberOfOccurrenceInUserTextPerString = UserLearningTextManager.getAllOccurrencesOfEveryWordEverIncludedInUserText();

		Set<LanguageWord> allExposableWordsFromUserTexts = LanguageWord.toLanguageWordSet(numberOfOccurrenceInUserTextPerString.keySet())
				.parallelStream().filter(x->vls.isExposableForLearning(x)).collect(Collectors.toSet());

		return allExposableWordsFromUserTexts
				//.parallelStream()
				.stream()
				.sorted((x,y)->x.toString().compareTo(y.toString()))
				.filter(x->!vls.isForbiddenWord(x))
				.filter(x->{
					return Translator.hasTranslationOrAGrundformRelatedTranslationThatIsNotFromGoogle(x);
				})
				.filter(x->vls.isExposableForLearning(x))
				.collect(Collectors.toSet());


	}

	private Comparator<LanguageWord> getComparatorSortingWordsByNumberOfOccurrencesInTheText() {
		Map<String, Integer> numberOfOccurrenceInUserTextPerString = UserLearningTextManager.getAllOccurrencesOfEveryWordEverIncludedInUserText();
		return (x,y)->{
			if(numberOfOccurrenceInUserTextPerString.get(x.getWord())>numberOfOccurrenceInUserTextPerString.get(y.getWord())) return -1;
			if(numberOfOccurrenceInUserTextPerString.get(x.getWord())<numberOfOccurrenceInUserTextPerString.get(y.getWord())) return 1;
			return x.toString().compareTo(y.toString());
		};


	}

	private Set<LanguageWord> getWordsSortedByFrequency(VocabularyLearningStatus vls) {
		Set<LanguageWord> res = new TreeSet<LanguageWord>(getComparatorSortingWordsByNumberOfOccurrencesInTheText());
		res.addAll(getAllExposableWords(vls));
		return res;
	}

	private List<LanguageWord> getTranslationsOfMostFrequentlyUsedWords(int listSize, VocabularyLearningStatus vls) {
		List<LanguageWord> res = new ArrayList<>();
		int i = 0;
		for(LanguageWord lw: getWordsSortedByFrequency(vls))
		{			
			Set<LanguageWord> alternatives = Translator.getTranslationsOf(lw)
					.stream().filter(x->!vls.isForbiddenWord(x)).collect(Collectors.toSet());


			if(					
					alternatives.stream()
					.anyMatch(x->!vls.isEarlyPhaseWord(x)))
				continue;
			/*.map(x->((SuccessfulTranslationDescription)x))
					.map(x->x.getTranslatedText())
					.filter(x->x.getListOfValidWords().size()==1)
					.map(x->x.getListOfValidWords().get(0))
					.filter(x->!vls(x))
					.collect(Collectors.toSet());*/
			if(!alternatives.isEmpty())
			{
				LanguageWord unknownTranslation = alternatives.iterator().next();
				/*unexploredAlternatives.stream()
						.filter(x->vls.isEarlyPhaseWord(x))
						.findFirst().get();*/

				res.add(unknownTranslation);
				i++;
				if(i>=listSize) break;
			}
		}

		return res;
	}

	private void preloadPossibleWords(VocabularyLearningStatus vls) {
		Set<LanguageWord> allExposableWordsFromUserTexts = LanguageWord.toLanguageWordSet(UserLearningTextManager.getAllOccurrencesOfEveryWordEverIncludedInUserText().keySet())
				.parallelStream().filter(x->vls.isExposableForLearning(x)).collect(Collectors.toSet());
		
		allExposableWordsFromUserTexts.addAll(vls.getAllExposableNewLanguageWords());

		allExposableWordsFromUserTexts.addAll(
				allExposableWordsFromUserTexts
				.stream().map(
						x->
						WordDescription.getGrundforms(x))
						.reduce(new HashSet<LanguageWord>(), (z,y)->{z.addAll(y); return z;})
				);
		AtomicInteger counter = new AtomicInteger();
		List<LanguageWord>allWords = allExposableWordsFromUserTexts.stream().collect(Collectors.toList());
		Collections.shuffle(allWords);;
		allWords.stream()
		.sorted((x,y)->x.toString().compareTo(y.toString()))
		.forEach(x->{
			Translator.getTranslationsOfTranslations(x);
			DiscordManager.getHiddenAnswerStringFor(
					WordDescription.getDescriptionFor(x));
		System.out.println("Preload purge:"+counter.incrementAndGet()+"/"+allExposableWordsFromUserTexts.size());
		});
		
		

		getTranslationsOfMostFrequentlyUsedWords(Integer.MAX_VALUE, vls)
		.stream().forEach(x->WordDescription.getGrundforms(x));
	}

	private void fillCacheForSession(ReadThroughFocus currentFocus) {
		Set<LanguageWord> consideredWords = new HashSet<>();
		consideredWords.addAll(shortTermWordsToTeachInThisSession);
		consideredWords.addAll(midTermWordsToTeachInThisSession);
		consideredWords.addAll(longTermWordsToTeachInThisSession);


		DiscordManager.getHiddenAnswerStringFor(
				WordDescription.getDescriptionFor(LanguageWord.newInstance("hej", LanguageCode.SV)));
		AtomicInteger i = new AtomicInteger();
		consideredWords.stream().forEach(lw->
		{
			i.incrementAndGet();
			System.out.println(i+"/"+consideredWords.size()+" :"+lw);
			DiscordManager.getHiddenAnswerStringFor(WordDescription.getDescriptionFor(lw));
			LocalAudioDatabase.getAudioFileFor(lw);			
		});
	}

	public static Session default3x3LearningSession(ReadThroughFocus currentFocus, 
			VocabularyLearningStatus vls) {
		return new Session(currentFocus,vls);

	}

	public LanguageWord getNextWord() {
		LanguageWord word = null;
		if(!shortTermWordsToTeachInThisSession.isEmpty())
		{
			temporalityLastAttempt = SML.SHORT;
			word = shortTermWordsToTeachInThisSession.get(0); shortTermWordsToTeachInThisSession.remove(0);}
		else if(!midTermWordsToTeachInThisSession.isEmpty())
		{
			temporalityLastAttempt = SML.MIDDLE;
			word = midTermWordsToTeachInThisSession.get(0); midTermWordsToTeachInThisSession.remove(0);}
		else if(!longTermWordsToTeachInThisSession.isEmpty())
		{
			temporalityLastAttempt = SML.LONG;
			word = longTermWordsToTeachInThisSession.get(0); longTermWordsToTeachInThisSession.remove(0);}
		current = word;
		return word;
	}

	public String getStatisticsOnRemainingToLearn() {
		return shortTermWordsToTeachInThisSession.size()+"/"+
				midTermWordsToTeachInThisSession.size()+"/"+
				longTermWordsToTeachInThisSession.size();
	}

	public boolean isSessionOver() {
		return shortTermWordsToTeachInThisSession.isEmpty()
				&&midTermWordsToTeachInThisSession.isEmpty() 
				&& longTermWordsToTeachInThisSession.isEmpty();
	}

	public void confirm(LanguageWord lastWordAsked, int learningIncrement) {
		if(temporalityLastAttempt == SML.SHORT)nbSuccessShort+=learningIncrement;
		else if(temporalityLastAttempt == SML.MIDDLE)nbSuccessMid+=learningIncrement;
		else if(temporalityLastAttempt == SML.LONG)nbSuccessLong+=learningIncrement;
		else
			throw new Error();
	}

	public void recordFailedToRecallLastWord(boolean decreasedLearningAbility, boolean isWordAboutToBeRemovedOutOfLearnedWordsIfNotRecalledCorrectly) {
		if(!decreasedLearningAbility)return;
		if(temporalityLastAttempt == SML.SHORT)nbDecreasesShort++;
		else if(temporalityLastAttempt == SML.MIDDLE)nbDecreasesMid++;
		else if(temporalityLastAttempt == SML.LONG)nbDecreasesLong++;
		else
			throw new Error();	
	}

	public int getNbSuccessShortTerm() {
		return nbSuccessShort;
	}

	public int getNbSuccessMidTerm() {
		return nbSuccessMid;
	}

	public int getNbSuccessLongTerm() {
		return nbSuccessLong;
	}

	public int getNbShortTermWordsAsked() {
		return nbShortToExplore;
	}

	public int getNbMidTermWordsAsked() {
		return nbMidToExplore;
	}

	public int getNbLongTermWordsAsked() {
		return nbLongToExplore;
	}

	public int getFinalBalance() {
		return nbSuccessShort+nbSuccessMid-nbDecreasesShort-nbDecreasesMid;
	}

	public Set<LanguageWord> getAllWords() {
		Set<LanguageWord> res = new HashSet<>();
		res.addAll(shortTermWordsToTeachInThisSession);
		res.addAll(midTermWordsToTeachInThisSession);
		res.addAll(longTermWordsToTeachInThisSession);
		return res;
	}

	public Set<LanguageWord> getAllUnknownWords() {
		return shortTermWordsToTeachInThisSession.stream().collect(Collectors.toSet());
	}


}
