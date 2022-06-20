package discordforrad.models.language;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
		
		String aux = TextInputUtils.Utf8ToIso(text);
		if(!aux.equals(text))
			throw new Error();
	}
	
	public String toString() {return lc+":"+text;}


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
		List<String> listOfWords = TextInputUtils.toListOfWordsWithoutSymbols(text); 
		return listOfWords.stream()
				.map(x->LanguageWord.newInstance(x, lc))
				.filter(x->Dictionnary.isInDictionnariesWithCrosscheck(x))
				.collect(Collectors.toList());
	}

	public Set<LanguageWord> getSetOfValidWords() {
		return getListOfValidWords().stream().collect(Collectors.toSet());
	}

	public static LanguageText newInstance(LanguageWord lw) {
		return newInstance(lw.getCode(),lw.getWord());
	}
	
	public static LanguageText parse(String s)
	{
		if(!s.contains(":"))
			throw new Error();
		String head = s.substring(0,s.indexOf(":"));
		String end = s.substring(s.indexOf(":")+1);
		return newInstance(LanguageCode.valueOf(head), end);
	}

	public boolean isSingleWord() {
		List<LanguageWord> words = getListOfValidWords();
		
		return words.size()==1;
	}

	public LanguageWord toSingleWord() {
		return getListOfValidWords().get(0);
	}
	
}
