package discordforrad.translation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import discordforrad.models.language.LanguageCode;

public interface ResultOfTranslationAttempt {
	public enum Origin{WORD_REFERENCE, BAB_LA, GOOGLE}

	public Origin getOriginOfTranslation();
	
	public String toParsableString();
	
	public static List<ResultOfTranslationAttempt> parseList(String s)
	{
		if(s.equals("[]"))return new ArrayList<ResultOfTranslationAttempt>();
		return Arrays.asList(s.split(";")).stream().map(x->parse(x)).collect(Collectors.toList());
	}
	
	public static Set<ResultOfTranslationAttempt> parseSet(String s)
	{
		return parseList(s).stream().collect(Collectors.toSet());
	}
	
	public static ResultOfTranslationAttempt parse(String s)
	{
		if(s.endsWith("FAILED_TO_TRANSLATE"))
		{
			return TranslationOutcomeFailure.newInstance(Origin.valueOf(s.split(":")[0]));
		}
		if(s.endsWith("NO_TRANSLATION_IN_DB"))
			return NoTranslationForTheRequest.newInstance(Origin.valueOf(s.split(":")[0]));
		return SuccessfulTranslationDescription.parse(s);	
	}

	public static String toParsableString(Set<ResultOfTranslationAttempt> s) {
		return s.stream().map(x->x.toParsableString()).reduce( (x,y)->x+";"+y).get();
	}

	public LanguageCode getResultLanguageCode();


}
