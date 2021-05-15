package discordforrad.models;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.stream.Collectors;

import discordforrad.languageModel.LanguageWord;

public class LearningModel {

	public static LanguageWord getNextWordToTranslate(VocabularyLearningStatus vls) {
		return vls.getAllWords().stream().filter(x->isTimeForLearning(x,vls)).collect(Collectors.toSet()).iterator().next();
	}
	
	public static boolean anyWordToTranslate(VocabularyLearningStatus vls)
	{
		return 
				vls.getAllWords().stream().anyMatch(x->isTimeForLearning(x,vls));
	}
	public static boolean isTimeForLearning(LanguageWord s,VocabularyLearningStatus vls) {
		TemporalAmount durationToWaitBeforeNextIncrement = getTimeBetweenIncrementsForLearning(vls.getNumberOfSuccessLearning(s));
		LocalDateTime timeLastSuccess = vls.getLastSuccessOf(s);
		LocalDateTime timeNextAttemps = timeLastSuccess.plus(durationToWaitBeforeNextIncrement);
		return timeNextAttemps.isBefore(LocalDateTime.now());
		
	}

	private static TemporalAmount getTimeBetweenIncrementsForLearning(int numberOfSuccessLearning) {
		if(numberOfSuccessLearning==0) return Duration.ZERO;
		if(numberOfSuccessLearning==1) return Duration.ofHours(6);
		if(numberOfSuccessLearning<7)
			return Duration.ofDays(numberOfSuccessLearning-1);
		else
			return Duration.ofDays(numberOfSuccessLearning*numberOfSuccessLearning);
		
		
	}

}
