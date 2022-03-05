package discordforrad.models.language;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import discordforrad.translation.TranslationOutcomeFailure;

public interface ResultOfTranslationAttempt {
	public enum Origin{WORD_REFERENCE, BAB_LA, GOOGLE}

	public Origin getOriginOfTranslation();
	
	public String toParsableString();
	
	public static List<ResultOfTranslationAttempt> parseList(String s)
	{
		return Arrays.asList(s.split(";")).stream().map(x->parse(x)).collect(Collectors.toList());
	}
	
	public static ResultOfTranslationAttempt parse(String s)
	{
		if(s.endsWith("FAILED_TO_TRANSLATE"))
		{
			return TranslationOutcomeFailure.newInstance(Origin.valueOf(s.split(":")[0]));
		}
		return SuccessfulTranslationDescription.parse(s);	
	}


}
