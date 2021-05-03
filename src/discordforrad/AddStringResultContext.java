package discordforrad;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import discordforrad.languageModel.LanguageWord;

public class AddStringResultContext {

	private final Set<LanguageWord> words = new HashSet<>();
	private final Consumer<LanguageWord>onAddingWord;
	
	public AddStringResultContext(Consumer<LanguageWord>onAddingWord)
	{
		this.onAddingWord = onAddingWord;
	}
	
	public void addResult(LanguageWord lw) {
		this.onAddingWord.accept(lw);
		words.add(lw);
	}

	public Set<LanguageWord> getWords() {
		return words;
	}

}
