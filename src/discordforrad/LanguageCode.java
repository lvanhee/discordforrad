package discordforrad;

public enum LanguageCode {
	EN,
	SV;

	public static LanguageCode otherLanguage(LanguageCode languageCode) {
		if(languageCode==EN)return SV;
		if(languageCode==SV)return EN;
		throw new Error();
	}
}
