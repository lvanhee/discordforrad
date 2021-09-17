package discordforrad.models.language;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import discordforrad.LanguageCode;

public class LanguageWord implements Serializable{
	private final LanguageCode lc;
	private final String word;
	
	public LanguageWord(LanguageCode lc, String word)
	{
		if(word.contains("!"))
			throw new Error();
		this.lc = lc;
		this.word = word;
	}
	
	public int hashCode()
	{
		return lc.hashCode()*word.hashCode();
	}
	
	public boolean equals(Object o)
	{
		LanguageWord lw = (LanguageWord) o;
		return lw.lc.equals(lc) && lw.word.equals(word);
	}

	public String getWord() {
		return word;
	}

	public LanguageCode getCode() {
		return lc;
	}
	
	public String toString()
	{
		return lc+":"+word;
	}

	public static LanguageWord newInstance(String s, LanguageCode languageCode) {
		return new LanguageWord(languageCode, s.toLowerCase());
	}

	public static Set<LanguageWord> toLanguageWordSet(Set<String> strings) {
		Set<LanguageWord>res = new HashSet<LanguageWord>();
		for(LanguageCode lc: LanguageCode.values())
			res.addAll(strings.stream().map(x->LanguageWord.newInstance(x, lc)).collect(Collectors.toSet()));
		return res;
	}

}
