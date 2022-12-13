package discordforrad.translation;

import discordforrad.models.language.LanguageCode;
import discordforrad.models.language.LanguageWord;

public class TranslationQuery {
	private final LanguageWord originWord;
	private final LanguageCode target;
	
	public TranslationQuery(LanguageWord singleWord, LanguageCode translateTo) {
		this.originWord = singleWord;
		this.target = translateTo;
	}

	public static TranslationQuery parse(String s)
	{
		throw new Error();
	}

	public static TranslationQuery newInstance(LanguageWord singleWord, LanguageCode translateTo) {
		return new TranslationQuery(singleWord, translateTo);
	}

	public LanguageWord getWord() {
		return originWord;
	}

	public LanguageCode getTargetLanguage() {
		return target;
	}

}
