package discordforrad.languageModel;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import discordforrad.LanguageCode;


public class LanguageText {
	private final String text;
	private final LanguageCode lc;

	public LanguageText(String txt, LanguageCode l)
	{
		this.text = txt;
		this.lc = l;
	}
	
	public String toString() {return lc+"|"+text;}


	public LanguageCode getLanguageCode() {
		return lc;
	}

	public String getText() {
		return text;
	}
	
	public boolean equals(Object o) {return o.toString().equals(toString());};
	public int hashCode() {return text.hashCode()+lc.hashCode();}

	public static LanguageText newInstance(LanguageCode lc, String text) {
		return new LanguageText(text, lc);
	}
	
}
