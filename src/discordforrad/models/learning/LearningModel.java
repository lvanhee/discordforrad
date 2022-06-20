package discordforrad.models.learning;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.stream.Collectors;

import discordforrad.models.language.LanguageWord;

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
		if(s.getWord().equals("anst�lld"))
			System.out.print("");
		TemporalAmount durationToWaitBeforeNextIncrement = getTimeBetweenIncrementsForLearning(vls.getNumberOfSuccessLearning(s));
		LocalDateTime timeLastSuccess = vls.getLastSuccessOf(s);
		LocalDateTime timeNextAttemps = timeLastSuccess.plus(durationToWaitBeforeNextIncrement);
		LocalDateTime now = LocalDateTime.now();
		boolean result = timeNextAttemps.isBefore(now);
		return result;
		
	}

	private static TemporalAmount getTimeBetweenIncrementsForLearning(int numberOfSuccessLearning) {
		if(numberOfSuccessLearning==0) return Duration.ZERO;
		if(numberOfSuccessLearning==1) return Duration.ofHours(6);
		if(numberOfSuccessLearning<=7)
			return Duration.ofDays(numberOfSuccessLearning-1);
		else
			return Duration.ofDays(numberOfSuccessLearning*numberOfSuccessLearning);
		
		
	}

}
