package discordforrad.models.language;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import discordforrad.LanguageCode;
import discordforrad.inputUtils.TextInputUtils;


public class LanguageText implements Serializable{
	private final String text;
	private final LanguageCode lc;

	public LanguageText(String txt, LanguageCode l)
	{
		if(txt==null)
			throw new Error();
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

	public List<LanguageWord> getListOfValidWords() {
		
		return TextInputUtils.toListOfWords(text).stream()
				.map(x->LanguageWord.newInstance(x, lc))
				.filter(x->Dictionnary.isInDictionnaries(x))
				.collect(Collectors.toList());
	}

	public Set<LanguageWord> getSetOfValidWords() {
		return getListOfValidWords().stream().collect(Collectors.toSet());
	}

	public static LanguageText newInstance(LanguageWord lw) {
		return newInstance(lw.getCode(),lw.getWord());
	}
	
}
