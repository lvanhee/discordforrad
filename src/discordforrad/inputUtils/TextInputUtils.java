package discordforrad.inputUtils;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

public class TextInputUtils {
	
	public static final Charset ISO_CHARSET = Charset.forName("ISO-8859-1");
	public static final Charset UTF8 = Charset.forName("UTF-8");

	public static String Utf8ToIso(String line) {
		return line.replaceAll("Ã¥", "å")
				.replaceAll("Ã¤", "ä")
				.replaceAll("Ã¶", "ö")
				.replaceAll("Ã–", "ö")
				.replaceAll("ã¥", "å")
				.replaceAll("ã…", "Å")
				.replaceAll("ã\\?", "Å")
				.replaceAll("ã¶", "ö");
		
	}

	public static List<String> toListOfWords(String text) {
		text = clearOfSymbols(text);
		text = TextInputUtils.Utf8ToIso(text);
		text = text.toLowerCase();
		while(text.startsWith(" "))text = text.substring(1);
		while(text.contains("  "))text = text.replaceAll("  ", " ");
		
		return Arrays.asList(text.split(" "));
	}
	
	public static String clearOfSymbols(String string) {
		string = string.replaceAll(",", " ");
		string = string.replaceAll("/", " ");
		string = string.replaceAll("-", " ");
		string = string.replaceAll("^", " ");
		string = string.replaceAll("”", " ");
		string = string.replaceAll("\\.", " ");
		string = string.replaceAll("\"", " ");
		string = string.replaceAll(";", " ");
		string = string.replaceAll("\\(", " ");
		string = string.replaceAll("\\)", " ");
		string = string.replaceAll("\\]", " ");
		string = string.replaceAll("\\[", " ");
		string = string.replaceAll("!", " ");
		string = string.replaceAll("\\|", " ");
		string = string.replaceAll(":", " ");
		string = string.replaceAll("\\?", " ");
		string = string.toLowerCase();
		string = string.replaceAll("\n", " ");
		string = string.replaceAll("[0-9]", "");
		while(string.contains("  "))
			string = string.replaceAll("  ", " ");

		return string;
	}

}
