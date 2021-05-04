package discordforrad.models.learning.session;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import discordforrad.LanguageCode;
import discordforrad.Translator;
import discordforrad.discordmanagement.OrdforrAIListener;
import discordforrad.languageModel.LanguageWord;
import discordforrad.models.LearningModel;
import discordforrad.models.VocabularyLearningStatus;
import discordforrad.models.learning.focus.ReadThroughFocus;

public class EntryDrivenSMLLearningSession {

	private final List<LanguageWord> shortTermWordsToTeachInThisSession;
	private final List<LanguageWord> midTermWordsToTeachInThisSession;
	private final List<LanguageWord> longTermWordsToTeachInThisSession;
	private final boolean isFocusFullyCompleted;
	
	private final int nbShortToExplore;
	private final int nbMidToExplore;
	private final int nbLongToExplore;

	private EntryDrivenSMLLearningSession(ReadThroughFocus currentFocus, 
			VocabularyLearningStatus vls) {

		List<LanguageWord> allWords = currentFocus.getAllSortedWords();
		
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
		

		shortTermWordsToTeachInThisSession = shortList.subList(0, Math.min(10, shortList.size()));
		midTermWordsToTeachInThisSession = midList.subList(0, Math.min(10, midList.size()));
		longTermWordsToTeachInThisSession = longList.subList(0, Math.min(10, longList.size()));
		isFocusFullyCompleted = shortTermWordsToTeachInThisSession.isEmpty() &&
				midTermWordsToTeachInThisSession.isEmpty() &&
				longTermWordsToTeachInThisSession.isEmpty();
		
		nbShortToExplore = shortList.size();
		nbMidToExplore = midList.size();
		nbLongToExplore = longList.size();
		
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
		{word = shortTermWordsToTeachInThisSession.get(0); shortTermWordsToTeachInThisSession.remove(0);}
		else if(!midTermWordsToTeachInThisSession.isEmpty())
		{word = midTermWordsToTeachInThisSession.get(0); midTermWordsToTeachInThisSession.remove(0);}
		else if(!longTermWordsToTeachInThisSession.isEmpty())
		{word = longTermWordsToTeachInThisSession.get(0); longTermWordsToTeachInThisSession.remove(0);}
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

}
