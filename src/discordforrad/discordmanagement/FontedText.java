package discordforrad.discordmanagement;

import java.awt.Font;

public class FontedText {
	
	private final String text;
	private final DiscordFont font;
	
	private FontedText(String text, DiscordFont font)
	{
		this.text = text;
		this.font = font;
	}

	public DiscordFont getFont() {
		return font;
	}

	public String getText() {
		return text;
	}

	public static boolean isSensitiveToFonting(String text2) {
		for(int i = 0 ; i < text2.length();i++)
		{
			if(isCharacterSensitiveToFonting(text2.charAt(i)))
				return true;
		}
		return false;
	}

	private static boolean isCharacterSensitiveToFonting(char c) {
		return !(c==' ' || c=='\n'|| c=='\t'|| c=='\r');
	}

	public static FontedText newInstance(String text, DiscordFont font) {
		return new FontedText(text, font);
	}
	
	public String toString()
	{
		return font+":"+text;
	}

}
