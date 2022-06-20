package discordforrad.inputUtils.databases;

import cachingutils.advanced.failable.AttemptOutcome;
import discordforrad.inputUtils.WebScrapping.DataBaseEnum;
import discordforrad.models.language.LanguageWord;

public class WordReferenceDBManager {

	public static AttemptOutcome getContentsFor(LanguageWord lw, DataBaseEnum db) {
		throw new Error();
	}

	public static boolean isDatabaseDenyingService(String content) {
		return content.contains("WordReference is receiving too many requests from your IP address");
	}

}
