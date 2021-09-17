package discordforrad.models.language;

public interface TranslationDescription {
	public enum Origin{WORD_REFERENCE, BAB_LA, GOOGLE}

	public Origin getOriginOfTranslation();


}
