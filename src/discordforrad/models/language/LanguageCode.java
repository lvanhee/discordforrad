package discordforrad.models.language;

import org.json.simple.JSONObject;

public enum LanguageCode {
	EN,
	SV,
	FR;

	public static LanguageCode otherLanguage(LanguageCode languageCode) {
		if(languageCode==EN)return SV;
		if(languageCode==SV)return EN;
		throw new Error();
	}

	public JSONObject toJsonObject() {
		JSONObject res = new JSONObject();
		res.put("code", this.toString());
		return res;
	}

	public static LanguageCode fromJsonObject(JSONObject lc) {
		if(lc==null)
			throw new Error();
		return LanguageCode.valueOf((String)lc.get("code"));
	}
}
