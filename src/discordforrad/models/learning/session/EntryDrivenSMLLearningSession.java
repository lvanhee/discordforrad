package discordforrad.models.learning.session;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import discordforrad.LanguageCode;
import discordforrad.Translator;
import discordforrad.discordmanagement.OrdforrAIListener;
import discordforrad.models.LearningModel;
import discordforrad.models.VocabularyLearningStatus;
import discordforrad.models.language.Dictionnary;
import discordforrad.models.language.LanguageWord;
import discordforrad.models.language.WordDescription;
import discordforrad.models.learning.focus.ReadThroughFocus;

public class EntryDrivenSMLLearningSession {

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

	private EntryDrivenSMLLearningSession(ReadThroughFocus currentFocus, 
			VocabularyLearningStatus vls) {
		
		/*vls.getAllWords().stream()
		.sorted((x,y)->x.toString().compareTo(y.toString()))
		//.collect(Collectors.toSet())
		.forEach(x->Dictionnary.isInBabLaDisctionnary(x));*/

		List<LanguageWord> allWords = currentFocus.getAllValidSortedWords();
		
		List<LanguageWord> shortList = new ArrayList<>();
		List<LanguageWord> midList = new ArrayList<>();
		List<LanguageWord> longList = new ArrayList<>();

		for(LanguageWord lw:allWords.stream().filter(x->vls.isReadyToBeExposedAgain(x)).collect(Collectors.toList()))
			if(vls.isShortTermWord(lw))
				shortList.add(lw);
			else if(vls.isMidTermWord(lw))
				midList.add(lw);
			else if(vls.isLongTermWord(lw))
				longList.add(lw);
			else throw new Error();
		
		if(allWords.stream().anyMatch(x->!Dictionnary.isInDictionnaries(x)))throw new Error();
		
		shortList = new ArrayList<>(new LinkedHashSet<>(shortList));
		midList = new ArrayList<>(new LinkedHashSet<>(midList));
		longList = new ArrayList<>(new LinkedHashSet<>(longList));
		
		Collections.shuffle(longList);
		Collections.shuffle(midList);
		
		

		shortTermWordsToTeachInThisSession = shortList.subList(0, Math.min(20, shortList.size()));
		midTermWordsToTeachInThisSession = vls.getStandardSessionMidTermWordsToLearn();
		longTermWordsToTeachInThisSession = vls.getStandardSessionLongTermWordsToLearn().subList(0, 10);
		
		isFocusFullyCompleted = shortTermWordsToTeachInThisSession.isEmpty() &&
				midTermWordsToTeachInThisSession.isEmpty() &&
				longTermWordsToTeachInThisSession.isEmpty();
		
		nbShortToExplore = shortTermWordsToTeachInThisSession.size();
		nbMidToExplore = midTermWordsToTeachInThisSession.size();
		nbLongToExplore = longTermWordsToTeachInThisSession.size();
		
		for(LanguageWord lw:shortTermWordsToTeachInThisSession)
			System.out.println(lw+" "+Dictionnary.isInDictionnaries(lw));
		
		for(LanguageWord lw:midTermWordsToTeachInThisSession)
			System.out.println(lw+" "+Dictionnary.isInDictionnaries(lw));
		
		for(LanguageWord lw:longTermWordsToTeachInThisSession)
			System.out.println(lw+" "+Dictionnary.isInDictionnaries(lw));
		/*FORMER PROCEDURE FOR THE FULL RANDOM LEARNING
		 * 
		 * shortTermWordsToTeachInThisSession.clear();
		List<LanguageWord> allShortTermWordsReadyToBeAsked = vls.getAllShortTermWords().stream().filter(x->LearningModel.isTimeForLearning(x, vls)).collect(Collectors.toList());
		List<LanguageWord> allMidTermWordsReadyToBeAsked = vls.getAllMidTermWords().stream().filter(x->LearningModel.isTimeForLearning(x, vls)).collect(Collectors.toList());
		List<LanguageWord> allLongTermWordsReadyToBeAsked = vls.getAllLongTermWords().stream().filter(x->LearningModel.isTimeForLearning(x, vls)).collect(Collectors.toList());
		Random r = new Random();
		List<LanguageWord> allShortTermWordsPreviouslyFailed = vls.getAllShortTermWords().stream()
				.filter(x->vls.getLastSuccessOf(x).isAfter(LocalDateTime.MIN))
				.filter(x->LearningModel.isTimeForLearning(x, vls)).collect(Collectors.toList());

		for(int i = 0 ; i < 10 ; i++)
		{
			if(allShortTermWordsPreviouslyFailed.isEmpty())break;
			int index = r.nextInt(allShortTermWordsPreviouslyFailed.size());
			shortTermWordsToTeachInThisSession.add(allShortTermWordsPreviouslyFailed.get(index));
			allShortTermWordsPreviouslyFailed.remove(index);
		}

		while(shortTermWordsToTeachInThisSession.size() < 10)
		{
			if(allShortTermWordsReadyToBeAsked.isEmpty())break;
			int index = r.nextInt(allShortTermWordsReadyToBeAsked.size());
			shortTermWordsToTeachInThisSession.add(allShortTermWordsReadyToBeAsked.get(index));
			allShortTermWordsReadyToBeAsked.remove(index);
		}
		allMidTermWordsReadyToBeAsked.removeAll(shortTermWordsToTeachInThisSession);
		midTermWordsToTeachInThisSession.clear();
		for(int i = 0 ; i < 10 ; i++)
		{
			if(allMidTermWordsReadyToBeAsked.isEmpty())break;
			int index = r.nextInt(allMidTermWordsReadyToBeAsked.size());
			midTermWordsToTeachInThisSession.add(allMidTermWordsReadyToBeAsked.get(index));
			allMidTermWordsReadyToBeAsked.remove(index);
		}

		longTermWordsToTeachInThisSession.clear();
		for(int i = 0 ; i < 10 ; i++)
		{
			if(allLongTermWordsReadyToBeAsked.isEmpty())break;
			int index = r.nextInt(allLongTermWordsReadyToBeAsked.size());
			midTermWordsToTeachInThisSession.add(allLongTermWordsReadyToBeAsked.get(index));
			allLongTermWordsReadyToBeAsked.remove(index);
		}*/
	}

	public static EntryDrivenSMLLearningSession default3x3LearningSession(ReadThroughFocus currentFocus, 
			VocabularyLearningStatus vls) {
		return new EntryDrivenSMLLearningSession(currentFocus,vls);

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
