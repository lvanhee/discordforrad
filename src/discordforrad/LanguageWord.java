package discordforrad;

public class LanguageWord {
	private final LanguageCode lc;
	private final String word;
	
	public LanguageWord(LanguageCode lc, String word)
	{
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

}
