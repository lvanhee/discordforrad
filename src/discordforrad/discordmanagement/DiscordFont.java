package discordforrad.discordmanagement;

import java.awt.Font;

public enum DiscordFont {
	NO_FONT, BOLD, ITALICS, BOLD_ITALICS_INVISIBLE, BOLD_ITALICS_VISIBLE, INVISIBLE;

	public static String getDiscordStringSymbol(DiscordFont df) {
		switch(df)
		{
		case INVISIBLE: return "||";
		case NO_FONT:return "";
		case ITALICS: return "*";
		case BOLD: return "**";
		case BOLD_ITALICS_VISIBLE: return "***";
		}
		throw new Error();
	}

	public static DiscordFont applyModificator(DiscordFont d1, DiscordFont d2) {
		if(d1.equals(NO_FONT))return d2;
		if(d2.equals(NO_FONT))return d1;
		throw new Error();
	}
};

