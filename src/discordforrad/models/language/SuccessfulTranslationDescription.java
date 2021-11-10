package discordforrad.models.language;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import discordforrad.LanguageCode;
import discordforrad.Translator;
import discordforrad.models.language.WordDescription.WordType;

public class SuccessfulTranslationDescription implements TranslationDescription, Serializable{
	
	private final LanguageText translation;
	private final WordType wordType;
	private final String additionalExplanation;
	private final TranslationDescription.Origin translationOrigins;
	private SuccessfulTranslationDescription(LanguageText translation2, WordType type, String additionalExplanation, TranslationDescription.Origin origin)
	{
		if(additionalExplanation.contains("<"))
			throw new Error();
		if(translation2.getText().contains("<"))
			throw new Error();
		this.translation = translation2;
		this.wordType = type;
		this.translationOrigins = origin;
		this.additionalExplanation = additionalExplanation;
	}
	public static SuccessfulTranslationDescription newInstance(LanguageText translation, String additionalExplanation, WordType type, 
			TranslationDescription.Origin origin) {
		return new SuccessfulTranslationDescription(translation, type, additionalExplanation, origin);
	}
	
	public String toString()
	{
		return translation+" "+additionalExplanation+" "+wordType+" "+translationOrigins;
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
		SuccessfulTranslationDescription td = (SuccessfulTranslationDescription)o;
		return td.translation.equals(translation)&&
				td.wordType.equals(wordType)&&
				td.additionalExplanation.equals(additionalExplanation)&&
				td.translationOrigins.equals(translationOrigins);
	}
	public static SuccessfulTranslationDescription newInstance(LanguageWord word) {
		
		if(WITH_GOOGLE_TRANSLATE)
			translations.add(
					SuccessfulTranslationDescription.newInstance(
							Translator.getGoogleTranslation(lw,translateTo),
							"",
							WordDescription.WordType.UNDEFINED,
							Origin.GOOGLE));
		
		translations.addAll(Translator.getWordReferenceTranslationsFrom(lw,translateTo));

		return translations;
		
		
	}
	public Origin getOrigin() {
		return translationOrigins;
	}

}
