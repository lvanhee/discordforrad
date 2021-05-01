package discordforrad;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;


public class LanguageText {
	private final String text;
	private final LanguageCode lc;

	public LanguageText(String txt, LanguageCode l)
	{
		this.text = txt;
		this.lc = l;
	}
	
	public String toString() {return lc+"|"+text;}

	public static Set<LanguageText> parse(Path recordFilepath) {
		Set<LanguageText> res = new HashSet<>();

		try {
			String[] input = Files.readString(recordFilepath,Charset.forName("ISO-8859-1")).split("\\|\n");

			for(String s:input)
			{
				if(s.isEmpty())continue;
				LanguageCode lc = LanguageCode.valueOf(s.substring(0,s.indexOf("|")));
				String txt = s.substring(s.indexOf("|")+1);
				res.add(new LanguageText(txt, lc));
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error();
		}
		return res;
	}

	public LanguageCode getLanguageCode() {
		return lc;
	}

	public String getText() {
		return text;
	}
	
	public boolean equals(Object o) {return o.toString().equals(toString());};
	public int hashCode() {return text.hashCode()+lc.hashCode();}
	
}
