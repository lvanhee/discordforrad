package discordforrad.models.learning.session;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import discordforrad.LanguageCode;
import discordforrad.Main;
import discordforrad.Translator;
import discordforrad.discordmanagement.DiscordManager;
import discordforrad.inputUtils.UserLearningTextManager;
import discordforrad.models.LearningModel;
import discordforrad.models.VocabularyLearningStatus;
import discordforrad.models.language.Dictionnary;
import discordforrad.models.language.LanguageWord;
import discordforrad.models.language.SuccessfulTranslationDescription;
import discordforrad.models.language.TranslationDescription;
import discordforrad.models.language.WordDescription;
import discordforrad.models.language.TranslationDescription.Origin;
import discordforrad.models.language.wordnetwork.WordNetwork;
import discordforrad.models.learning.focus.ReadThroughFocus;

public class Session {
	private final List<LanguageWord> shortTermWordsToTeachInThisSession;
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

	private Session(ReadThroughFocus currentFocus, 
			VocabularyLearningStatus vls) {

		Map<String, Integer> numberOfOccurrenceInUserTextPerString = UserLearningTextManager.getAllOccurrencesOfEveryWordEverIncludedInUserText();
		
		Set<LanguageWord> allWordsFromUserTexts = LanguageWord.toLanguageWordSet(numberOfOccurrenceInUserTextPerString.keySet())
				.parallelStream().filter(x->!vls.isForbiddenWord(x)&&Dictionnary.isInDictionnaries(x)).collect(Collectors.toSet());
		
		Set<LanguageWord> allExposableWordsFromUserTexts = LanguageWord.toLanguageWordSet(numberOfOccurrenceInUserTextPerString.keySet())
				.parallelStream().filter(x->vls.isExposableForLearning(x)).collect(Collectors.toSet());
		
		if(Main.PRELOAD_PURGE_MODE)
			purgeWords(vls);
		
		Set<LanguageWord> allExposableWords = allExposableWordsFromUserTexts
				.parallelStream()
				.filter(x->!vls.isForbiddenWord(x))
				.filter(x->{
					return Translator.hasTranslationOrAGrundformRelatedTranslationThatIsNotFromGoogle(x);
				})
				.filter(x->vls.isExposableForLearning(x))
				.collect(Collectors.toSet());
		
		Set<LanguageWord> allExposableEarlyPhaseWords = allExposableWords
				.parallelStream()
				.filter(x->vls.isEarlyPhaseWord(x))
				.collect(Collectors.toSet());
		
		Comparator<LanguageWord> frequencyComparator = (x,y)->{
			if(numberOfOccurrenceInUserTextPerString.get(x.getWord())>numberOfOccurrenceInUserTextPerString.get(y.getWord())) return -1;
			if(numberOfOccurrenceInUserTextPerString.get(x.getWord())<numberOfOccurrenceInUserTextPerString.get(y.getWord())) return 1;
			return x.toString().compareTo(y.toString());
		};
		
		Set<LanguageWord> mostFrequentEarlyPhaseWords = new TreeSet<LanguageWord>(frequencyComparator);
		mostFrequentEarlyPhaseWords.addAll(allExposableEarlyPhaseWords);
		
		Set<LanguageWord> mostFrequentWords = new TreeSet<LanguageWord>(frequencyComparator);
		mostFrequentWords.addAll(allExposableWords);
		
		List<LanguageWord> shortList = new ArrayList<>();
		
		for(LanguageWord lw:currentFocus
				.getAllValidWordsSortedByTheirOrderOfOccurrenceInFocusTexts()
				.parallelStream()
				.filter(x->!vls.isForbiddenWord(x))
				.filter(x->LearningModel.isTimeForLearning(x, vls))
				.collect(Collectors.toList()))
			if(vls.isEarlyPhaseWord(lw))
				shortList.add(lw);
		
		if(allExposableWords.parallelStream().anyMatch(x->!Dictionnary.isInDictionnaries(x)))
			throw new Error();
		
		shortList = new ArrayList<>(new LinkedHashSet<>(shortList));
		midTermWordsToTeachInThisSession = vls.getStandardSessionMidTermWordsToLearn();
		longTermWordsToTeachInThisSession = vls.getStandardSessionLongTermWordsToLearn().subList(0, 10);
		
		Collections.shuffle(midTermWordsToTeachInThisSession);
		Collections.shuffle(longTermWordsToTeachInThisSession);
		
		
		
		shortTermWordsToTeachInThisSession = shortList.subList(0, Math.min(5, shortList.size()));
		
		int i = 0;
		for(LanguageWord lw: mostFrequentEarlyPhaseWords)
		{
			if(i>=5) break;
			shortTermWordsToTeachInThisSession.add(lw);
			i++;
		}
		
		i = 0;
		for(LanguageWord lw: mostFrequentWords)
		{
			if(i>=5) break;
			
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
				
				shortTermWordsToTeachInThisSession.add(unknownTranslation);
				i++;
			}
		}
		
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
		
		i = 0;
		for(LanguageWord lw: mostFrequentGrundformOfLearnedWords)
		{
			if(i>=5) break;
			shortTermWordsToTeachInThisSession.add(lw);
			i++;
		}
		
		

		
		isFocusFullyCompleted = shortTermWordsToTeachInThisSession.isEmpty() &&
				midTermWordsToTeachInThisSession.isEmpty() &&
				longTermWordsToTeachInThisSession.isEmpty();
		
		nbShortToExplore = shortTermWordsToTeachInThisSession.size();
		nbMidToExplore = midTermWordsToTeachInThisSession.size();
		nbLongToExplore = longTermWordsToTeachInThisSession.size();
		
		fillCacheForSession();
	}

	private void purgeWords(VocabularyLearningStatus vls) {
		Set<LanguageWord> allExposableWordsFromUserTexts = LanguageWord.toLanguageWordSet(UserLearningTextManager.getAllOccurrencesOfEveryWordEverIncludedInUserText().keySet())
				.parallelStream().filter(x->vls.isExposableForLearning(x)).collect(Collectors.toSet());
		AtomicInteger counter = new AtomicInteger();
		allExposableWordsFromUserTexts.parallelStream().forEach(x->{Translator.getTranslationsOfTranslations(x);
		 System.out.println(counter.incrementAndGet()+" "+allExposableWordsFromUserTexts.size());
		});
	}

	private void fillCacheForSession() {
		Set<LanguageWord> consideredWords = new HashSet<>();
		consideredWords.addAll(shortTermWordsToTeachInThisSession);
		consideredWords.addAll(midTermWordsToTeachInThisSession);
		consideredWords.addAll(longTermWordsToTeachInThisSession);
		
	
		DiscordManager.getHiddenAnswerStringFor(WordDescription.getDescriptionFor(LanguageWord.newInstance("hej", LanguageCode.SV)));
		AtomicInteger i = new AtomicInteger();
		consideredWords.parallelStream().forEach(lw->
		{
			System.out.println(i+"/"+consideredWords.size()+" :"+lw);
			DiscordManager.getHiddenAnswerStringFor(WordDescription.getDescriptionFor(lw));
			i.incrementAndGet();
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

	public void confirm(LanguageWord lastWordAsked) {
		if(temporalityLastAttempt == SML.SHORT)nbSuccessShort++;
		else if(temporalityLastAttempt == SML.MIDDLE)nbSuccessMid++;
		else if(temporalityLastAttempt == SML.LONG)nbSuccessLong++;
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

}
