package discordforrad.translation;

import discordforrad.models.language.LanguageText;

public class SuccessfulTranslationOutcome implements TranslationOutcome {
	
	private final LanguageText text;
	private SuccessfulTranslationOutcome(LanguageText t)
	{
		this.text = t;
	}
	
	public int hashCode() {return text.hashCode();}
	
	public String toString() { return text.toString();}
	
	public boolean equals(Object o) {
		return ((SuccessfulTranslationOutcome)o).text.equals(text);
	}

	public static TranslationOutcome newInstance(LanguageText newInstance) {
		return new SuccessfulTranslationOutcome(newInstance);
	}

	public LanguageText getText() {
		return text;
	}

}
