package discordforrad.models.learning.focus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import discordforrad.LanguageCode;
import discordforrad.inputUtils.RawLearningTextDatabaseManager;
import discordforrad.inputUtils.TextInputUtils;
import discordforrad.languageModel.LanguageText;
import discordforrad.languageModel.LanguageWord;

public class ReadThroughFocus {
	
	private static final Path FOCUS_PATH = Paths.get("data/focus/current_focus.txt");  
	
	private final String index;
	private final LanguageCode lc;
	

	public ReadThroughFocus(String string, LanguageCode lc2) {
		this.index = string;
		this.lc = lc2;
	}

	public static ReadThroughFocus loadCurrentFocus() {
		try {
			return parse(Files.readString(FOCUS_PATH));
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error();
		}
	}

	private static ReadThroughFocus parse(String string) {
		LanguageCode lc = LanguageCode.valueOf(string.substring(0,2));
		String index = string.substring(3);
		
		return new ReadThroughFocus(index,lc);
	}

	public List<LanguageWord> getAllSortedWords() {
		Set<String> words = new HashSet<>();
		List<LanguageWord> res = new ArrayList<>();
		
		List<String> l = TextInputUtils.toListOfWords(RawLearningTextDatabaseManager.fromID(index));
		
		for(String s: l)
			if(!words.contains(s))
			{
				words.add(s);
				res.add(LanguageWord.newInstance(s,lc));
			}
		
		return res;
	}

	public static ReadThroughFocus newInstance(String string, LanguageCode lc) {
		return new ReadThroughFocus(string, lc);
	}

	public static void saveFocusOnFile(ReadThroughFocus f) {
		try {
			Files.writeString(FOCUS_PATH, f.lc+" "+f.index, TextInputUtils.ISO_CHARSET);
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error();
		}
	}

	public String getIndex() {
		return index;
	}

	public String getRawText() {
		return RawLearningTextDatabaseManager.fromID(index);
	}

	public LanguageCode getLanguageCode() {
		return lc;
	}

}
