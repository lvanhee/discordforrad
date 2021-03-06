package discordforrad.models.learning.focus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import discordforrad.Main;
import discordforrad.inputUtils.UserLearningTextManager;
import discordforrad.inputUtils.TextInputUtils;
import discordforrad.models.language.LanguageCode;
import discordforrad.models.language.LanguageText;
import discordforrad.models.language.LanguageWord;
import discordforrad.models.learning.VocabularyLearningStatus;

public class ReadThroughFocus {
	
	private static final Path FOCUS_PATH = Paths.get(Main.ROOT_DATABASE+"focus/current_focus.txt");  
	
	private final List<LanguageText> texts;
	private final Map<LanguageText, String> indexes;
	private final Set<LanguageWord> allWordsInCurrentFocusCache;
	

	public ReadThroughFocus(List<LanguageText> texts, Map<LanguageText, String> indexes) {
		this.texts = texts;
		this.indexes = indexes;
		allWordsInCurrentFocusCache = texts.parallelStream().map(x->x.getSetOfValidWords()).reduce(new HashSet<>(), (x,y)->{x.addAll(y); return x;});
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
		Map<LanguageText, String> indexes = new HashMap<LanguageText, String>();
		input = input.replaceAll("\r", "");
		for(String s: input.split("\n"))
		{
			String inputText = UserLearningTextManager.fromID(s.substring(3));
			
			LanguageText toAdd = LanguageText.newInstance(
					LanguageCode.valueOf(s.substring(0,2)),
					inputText); 
			texts.add(toAdd);
			
			indexes.put(toAdd, s);
		}

		return new ReadThroughFocus(texts, indexes);
	}

	public List<LanguageWord> getAllValidWordsSortedByTheirOrderOfOccurrenceInFocusTexts() {
		return texts.stream()
				.map(x->x.getListOfValidWords())
				.reduce(new ArrayList<LanguageWord>(), (x,y)->{x.addAll(y); return x;});
	}

	public static ReadThroughFocus newInstance(String string, LanguageCode lc) {
		//return new ReadThroughFocus(Arrays.asList(LanguageText.newInstance(lc, string)));
		throw new Error();
	}

	public static void saveFocusOnFile(ReadThroughFocus f) {
		/*try {
			Files.writeString(FOCUS_PATH, f.lc+" "+f.index, TextInputUtils.ISO_CHARSET);
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error();
		}*/
		throw new Error();
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
	
	public String getIndexOf(LanguageText lt) { return indexes.get(lt);}

	public boolean isInCurrentFocus(LanguageWord x) {
		return allWordsInCurrentFocusCache.contains(x);
	}

	public Set<LanguageWord> getAllEarlyPhaseWordsExposableForLearning(VocabularyLearningStatus vls) {
		return allWordsInCurrentFocusCache.stream().filter(x->vls.isEarlyPhaseWord(x)&&vls.isExposableForLearning(x))
		.collect(Collectors.toSet());
	}
}
