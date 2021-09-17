package discordforrad.inputUtils.databases;

import discordforrad.inputUtils.DatabaseProcessingOutcome;
import discordforrad.inputUtils.WebScrapping.DataBaseEnum;
import discordforrad.models.language.LanguageWord;

public class WordReferenceDBManager {

	public static DatabaseProcessingOutcome getContentsFor(LanguageWord lw, DataBaseEnum db) {
		throw new Error();
	}

	public static boolean isContentOfSuccessfullyLoadedPage(String content) {
		return !content.contains("WordReference is receiving too many requests from your IP address");
	}

}
