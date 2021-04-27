package discordforrad;

import java.io.IOException;

public class DisOrdforrAId {
	public static void askForNextWord() throws IOException {
		String word = "ordförrådet";
		
		String translatedWord = Translator.getTranslation(word);
		
		String toAsk = "**"+word+"**\n||"+translatedWord+"||";
		
		Main.discussionChannel.sendMessage(toAsk).queue();
	}

	public static void confirm() throws IOException {
		// TODO INCREMENT LEARNING
		
		askForNextWord();
	}
}
