package discordforrad.models.learning.focus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import discordforrad.LanguageCode;
import discordforrad.inputUtils.RawLearningTextDatabaseManager;
import discordforrad.inputUtils.TextInputUtils;
import discordforrad.languageModel.LanguageText;
import discordforrad.languageModel.LanguageWord;

public class ReadThroughFocus {
	
	private static final Path FOCUS_PATH = Paths.get("data/focus/current_focus.txt");  
	
	private final List<LanguageText> texts;
	

	public ReadThroughFocus(List<LanguageText> texts) {
		this.texts = texts;
	}

	public static ReadThroughFocus loadCurrentFocus() {
		try {
			return parse(Files.readString(FOCUS_PATH));
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error();
		}
	}

	private static ReadThroughFocus parse(String input) {
		List<LanguageText> texts = new LinkedList<>();
		input = input.replaceAll("\r", "");
		for(String s: input.split("\n"))
		{
			String inputText = RawLearningTextDatabaseManager.fromID(s.substring(3));
			
			texts.add(LanguageText.newInstance(
					LanguageCode.valueOf(s.substring(0,2)),
					inputText));
		}
		
		return new ReadThroughFocus(texts);
	}

	public List<LanguageWord> getAllValidSortedWords() {
		return texts.stream().map(x->x.getListOfValidWords()).reduce(new ArrayList<LanguageWord>(), (x,y)->{x.addAll(y); return x;});
	}

	public static ReadThroughFocus newInstance(String string, LanguageCode lc) {
		return new ReadThroughFocus(Arrays.asList(LanguageText.newInstance(lc, string)));
	}

	public static void saveFocusOnFile(ReadThroughFocus f) {
		try {
			Files.writeString(FOCUS_PATH, f.lc+" "+f.index, TextInputUtils.ISO_CHARSET);
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error();
		}
	}

	public List<LanguageText> getLanguageTextList() {
		return texts;
	}

	public Set<LanguageText> getAllMasteredTexts(Set<LanguageWord> allLongTermWords) 
	{
		return texts.stream().filter(x->!x.getListOfValidWords().stream()
				.anyMatch(y->!allLongTermWords.contains(x)))
		.collect(Collectors.toSet());
	}
}
