package discordforrad.inputUtils;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class TextInputUtils {
	
	public static final Charset ISO_CHARSET = Charset.forName("ISO-8859-1");
	public static final Charset UTF8 = Charset.forName("UTF-8");

	public static String Utf8ToIso(String line) {
		String res = line.replaceAll("Ã¥", "å")
				.replaceAll("Ã¤", "ä")
				.replaceAll("Ã¶", "ö")
				.replaceAll("Ã–", "ö")
				.replaceAll("ã¥", "å")
				.replaceAll("ã…", "Å")
				.replaceAll("&#39;", "'")
				.replaceAll("ã©", "é")
				.replaceAll("Ã©", "é")
				.replaceAll("ã\\?", "Å")
				.replaceAll("ï¿½", "ö")
				.replaceAll("ã¶", "ö");
		
	/*	if(!res.equals(line))
			throw new Error();*/
		return res;
	}

	public static List<String> toListOfWordsWithoutSymbols(String text) {
		text = clearOfSymbols(text);
		/*text = TextInputUtils.Utf8ToIso(text);*/
		text = text.toLowerCase().trim();
		while(text.contains("  "))text = text.replaceAll("  ", " ");
		if(text.isBlank())return new ArrayList<>();
		
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

	public static List<String> splitAlong(String s, char opener, char closer) {
		int nbTotal = 0;
		
		boolean noFound = true;
		int startFound = -1;
		
		for(int i = 0 ; i < s.length(); i++)
		{
			if(s.charAt(i)==opener) {nbTotal++; if(noFound) {noFound = false; startFound = i;}}
			
			if(s.charAt(i)==closer)
			{
				nbTotal--;
				if(nbTotal==0)
				{
					String start = s.substring(startFound+1,i);
					List<String> end = splitAlong(s.substring(i+1), opener, closer);
					List<String> res = new ArrayList<>();
					res.add(start); res.addAll(end);
					return res;
				}
			}
		}
		
		if(noFound) return new ArrayList<>();
		
		List<String> res = new ArrayList<>();
		res.add(s);
		return res;
	}

}
