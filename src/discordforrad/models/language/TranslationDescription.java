package discordforrad.models.language;

import java.util.List;
import java.util.Set;

import discordforrad.LanguageCode;
import discordforrad.models.language.WordDescription.WordType;

public class TranslationDescription {
	public enum Origin{WORD_REFERENCE, BAB_LA, GOOGLE}
	
	private final LanguageText translation;
	private final WordType wordType;
	private final String additionalExplanation;
	private final Origin translationOrigins;
	private TranslationDescription(LanguageText translation2, WordType type, String additionalExplanation, Origin origin)
	{
		this.translation = translation2;
		this.wordType = type;
		this.translationOrigins = origin;
		this.additionalExplanation = additionalExplanation;
	}
	public static TranslationDescription newInstance(LanguageText translation, String additionalExplanation, WordType type, Origin origin) {
		return new TranslationDescription(translation, type, additionalExplanation, origin);
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
		TranslationDescription td = (TranslationDescription)o;
		return td.translation.equals(translation)&&
				td.wordType.equals(wordType)&&
				td.additionalExplanation.equals(additionalExplanation)&&
				td.translationOrigins.equals(translationOrigins);
	}

}
