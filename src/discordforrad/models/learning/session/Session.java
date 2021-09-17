package discordforrad.models.learning.session;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import discordforrad.Translator;
import discordforrad.discordmanagement.OrdforrAIListener;
import discordforrad.inputUtils.UserLearningTextManager;
import discordforrad.models.LearningModel;
import discordforrad.models.VocabularyLearningStatus;
import discordforrad.models.language.Dictionnary;
import discordforrad.models.language.LanguageWord;
import discordforrad.models.language.SuccessfulTranslationDescription;
import discordforrad.models.language.TranslationDescription;
import discordforrad.models.language.WordDescription;
import discordforrad.models.learning.focus.ReadThroughFocus;

public class Session {
	
	private static boolean PRELOAD_PURGE_MODE = true;

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
		
		Map<String, Integer> allStrings = UserLearningTextManager.getAllOccurrencesOfEveryWord();
		Set<LanguageWord> allExposableWords = LanguageWord.toLanguageWordSet(allStrings.keySet())
				.stream()
				.filter(x->Dictionnary.isInDictionnaries(x))
				.filter(x->vls.isReadyToBeExposedAgain(x))
				.collect(Collectors.toSet());
		
		Set<LanguageWord> allEarlyPhaseWords = allExposableWords
				.stream()
				.filter(x->vls.isEarlyPhaseWord(x))
				.collect(Collectors.toSet());
		
		Comparator<LanguageWord> frequencyComparator = (x,y)->{
			if(allStrings.get(x.getWord())>allStrings.get(y.getWord())) return -1;
			if(allStrings.get(x.getWord())<allStrings.get(y.getWord())) return 1;
			return x.toString().compareTo(y.toString());
		};
		
		Set<LanguageWord> mostFrequentEarlyPhaseWords = new TreeSet<LanguageWord>(frequencyComparator);
		mostFrequentEarlyPhaseWords.addAll(allEarlyPhaseWords);
		
		Set<LanguageWord> mostFrequentWords = new TreeSet<LanguageWord>(frequencyComparator);
		mostFrequentWords.addAll(allExposableWords);
		
		List<LanguageWord> shortList = new ArrayList<>();
		
		for(LanguageWord lw:currentFocus.getAllValidWordsSortedByTheirOrderOfOccurrenceInFocusTexts().stream().collect(Collectors.toList()))
			if(vls.isEarlyPhaseWord(lw))
				shortList.add(lw);
		
		if(allExposableWords.stream().anyMatch(x->!Dictionnary.isInDictionnaries(x)))
			throw new Error();
		
		shortList = new ArrayList<>(new LinkedHashSet<>(shortList));
		midTermWordsToTeachInThisSession = vls.getStandardSessionMidTermWordsToLearn();
		longTermWordsToTeachInThisSession = vls.getStandardSessionLongTermWordsToLearn().subList(0, 10);
		
		Collections.shuffle(midTermWordsToTeachInThisSession);
		Collections.shuffle(longTermWordsToTeachInThisSession);
		
		

		shortTermWordsToTeachInThisSession = shortList.subList(0, Math.min(10, shortList.size()));
		
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
			Set<TranslationDescription> translations = 
					Translator.getAllTranslations(lw, LanguageCode.otherLanguage(lw.getCode()));
			Set<LanguageWord> unexploredAlternatives = 
					translations
					.stream()
					.map(x->((SuccessfulTranslationDescription)x))
					.map(x->x.getTranslatedText())
					.filter(x->x.getListOfValidWords().size()==1)
					.map(x->x.getListOfValidWords().get(0))
					.filter(x->vls.isEarlyPhaseWord(x))
					.collect(Collectors.toSet());
			if(!unexploredAlternatives.isEmpty())
			{
				LanguageWord unknownTranslation = unexploredAlternatives.stream()
						.filter(x->vls.isEarlyPhaseWord(x))
						.findFirst().get();
				shortTermWordsToTeachInThisSession.add(unknownTranslation);
				i++;
			}
			
			if(PRELOAD_PURGE_MODE)
				shortTermWordsToTeachInThisSession.addAll(allExposableWords);
		}
		
		
		isFocusFullyCompleted = shortTermWordsToTeachInThisSession.isEmpty() &&
				midTermWordsToTeachInThisSession.isEmpty() &&
				longTermWordsToTeachInThisSession.isEmpty();
		
		nbShortToExplore = shortTermWordsToTeachInThisSession.size();
		nbMidToExplore = midTermWordsToTeachInThisSession.size();
		nbLongToExplore = longTermWordsToTeachInThisSession.size();
		
		fillCacheForSession();
	}

	private void fillCacheForSession() {
		Set<LanguageWord> consideredWords = new HashSet<>();
		consideredWords.addAll(shortTermWordsToTeachInThisSession);
		consideredWords.addAll(midTermWordsToTeachInThisSession);
		consideredWords.addAll(longTermWordsToTeachInThisSession);
		
	
		
		AtomicInteger i = new AtomicInteger();
		consideredWords.parallelStream().forEach(lw->
		{
			System.out.println(i+"/"+consideredWords.size()+" :"+lw);
			OrdforrAIListener.getHiddenAnswerStringFor(WordDescription.getDescriptionFor(lw));
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
