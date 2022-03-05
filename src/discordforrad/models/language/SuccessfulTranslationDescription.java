package discordforrad.models.language;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import discordforrad.models.LanguageCode;
import discordforrad.models.language.WordDescription.WordType;
import discordforrad.translation.Translator;

public class SuccessfulTranslationDescription implements ResultOfTranslationAttempt, Serializable{
	
	private final LanguageText translation;
	private final WordType wordType;
	private final String additionalExplanation;
	private final ResultOfTranslationAttempt.Origin translationOrigins;
	private SuccessfulTranslationDescription(LanguageText translation, WordType type, String additionalExplanation, ResultOfTranslationAttempt.Origin origin)
	{
		if(additionalExplanation.contains("<"))
			throw new Error();
		if(translation.getText().contains("<"))
			throw new Error();
		if(translation.getText().isBlank())
			throw new Error();
		this.translation = LanguageText.newInstance(translation.getLanguageCode(),
				translation.getText().replaceAll(":", ",").replaceAll("-", "").replaceAll(";", "").toLowerCase().trim());
		this.wordType = type;
		this.translationOrigins = origin;
		this.additionalExplanation = 
				additionalExplanation.replaceAll(";", ",")
				.trim();
	}
	public static SuccessfulTranslationDescription newInstance(LanguageText translation, String additionalExplanation, WordType type, 
			ResultOfTranslationAttempt.Origin origin) {
		return new SuccessfulTranslationDescription(translation, type, additionalExplanation, origin);
	}
	
	public String toString()
	{
		return translation+" "+additionalExplanation+" "+wordType+" "+translationOrigins;
	}
	
	public String toParsableString() {
		
		LanguageText clearedTranslation = LanguageText.newInstance(translation.getLanguageCode(),
				translation.getText().replaceAll(":", ",").replaceAll("-", ""));
		String res = clearedTranslation+"|"+wordType+"|"+additionalExplanation+"|"+translationOrigins;
		
		ResultOfTranslationAttempt parsed = parse(res);
		if(!parsed.equals(this))
			throw new Error();
		if(res.contains(";"))
			throw new Error();
		return res;
	}
	
	
	public LanguageText getTranslatedText() {
		return translation;
	}
	public Origin getOriginOfTranslation() {
		return translationOrigins;
	}
	public String getAdvancedDescription() {
		return additionalExplanation;
	}
	public WordType getWordType() {
		return wordType;
	}
	
	public int hashCode()
	{
		return translation.hashCode()+wordType.hashCode()+additionalExplanation.hashCode()+translationOrigins.hashCode();
	}
	
	public boolean equals(Object o)
	{
		if(!(o instanceof SuccessfulTranslationDescription))return false;
		SuccessfulTranslationDescription td = (SuccessfulTranslationDescription)o;
		return td.translation.equals(translation)&&
				td.wordType.equals(wordType)&&
				td.additionalExplanation.equals(additionalExplanation)&&
				td.translationOrigins.equals(translationOrigins);
	}
	public static SuccessfulTranslationDescription newInstance(LanguageWord word) {
		/*
		if(WITH_GOOGLE_TRANSLATE)
			translations.add(
					SuccessfulTranslationDescription.newInstance(
							Translator.getGoogleTranslation(lw,translateTo),
							"",
							WordDescription.WordType.UNDEFINED,
							Origin.GOOGLE));
		
		translations.addAll(Translator.getWordReferenceTranslationsFrom(lw,translateTo));

		return translations;*/
		throw new Error();
		
		
	}
	public Origin getOrigin() {
		return translationOrigins;
	}
	static ResultOfTranslationAttempt parse(String x) {
		String[] result = x.split(""
				+ "\\|");
		if(result.length!=4)throw new Error();
		LanguageWord lw = LanguageWord.parse(result[0].trim());
		WordType type = WordType.parse(result[1]);
		String additionalExplanation = result[2];
		Origin origin = Origin.valueOf(result[3]);
		
		return newInstance(LanguageText.newInstance(lw), additionalExplanation, type, origin);
	}

}
