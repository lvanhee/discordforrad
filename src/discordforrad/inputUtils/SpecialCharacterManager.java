package discordforrad.inputUtils;

import java.nio.charset.Charset;

public class SpecialCharacterManager {
	
	public static final Charset ISO_CHARSET = Charset.forName("ISO-8859-1");
	public static final Charset UTF8 = Charset.forName("UTF-8");

	public static String Utf8ToIso(String line) {
		return line.replaceAll("Ã¥", "å")
				.replaceAll("Ã¤", "ä")
				.replaceAll("Ã¶", "ö")
				.replaceAll("Ã–", "ö");
	}

}
