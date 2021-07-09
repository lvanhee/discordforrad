package discordforrad.inputUtils;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
				.replaceAll("ï¿½", "ö")
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
		
//		Set<Character> allowedChar = new HashSet<>();
		
		
		/*char c = (char)160;
		string = string.replaceAll(c+"", "");
		string = string.replaceAll("\t", "");
		string = string.replaceAll(",", " ");
		string = string.replaceAll("\r", " ");
		string = string.replaceAll("%", " ");
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
		string = string.replaceAll("\\*", " ");
		string = string.replaceAll("!", " ");
		string = string.replaceAll(">", " ");
		string = string.replaceAll("<", " ");
		string = string.replaceAll("\\{", " ");
		string = string.replaceAll("\\}", " ");
		string = string.replaceAll("\\|", " ");
		string = string.replaceAll(":", " ");
		string = string.replaceAll("\\?", " ");
		string = string.toLowerCase();
		string = string.replaceAll("\n", " ");
		string = string.replaceAll("=", " ");
		string = string.replaceAll("'", " ");
		string = string.replaceAll("–", " ");
		string = string.replaceAll("&", " ");
		string = string.replaceAll("[0-9]", "");
		*/

		String res = string.replaceAll("[^\\p{L}]", " ");
		
		while(res.contains("  "))
			res = res.replaceAll("  ", " ");
		return res;
	}

}
