package discordforrad.inputUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import discordforrad.Main;
import discordforrad.models.language.Dictionnary;
import discordforrad.models.language.LanguageCode;
import discordforrad.models.language.LanguageWord;
import discordforrad.translation.Translator;


public class UserLearningTextManager {
	
	private static final Path LOCATION_RAW_LEARNING_TEXT = Paths.get(Main.ROOT_DATABASE+"raw_text_database.txt");
	
	private static final Map<String, String> indexedEntries = new HashMap<String, String>();
	static
	{
		try {
			String in =  Files.readString(LOCATION_RAW_LEARNING_TEXT,Charset.forName("ISO-8859-1"));
			String[] input =in.split("\\|\\|");

			for(String s:input)
			{
				while(s.startsWith("\n")||s.startsWith(" "))s=s.substring(1);				
				if(s.isEmpty())continue;
				
				String index = s.substring(0,s.indexOf("|"));
				String contents = s.substring(s.indexOf("|")+1);
				indexedEntries.put(index, contents);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error();
		}
		System.out.println(indexedEntries);
	}

	public static String fromID(String index) {
		if(!indexedEntries.containsKey(index))
			throw new Error();
		return indexedEntries.get(index);
	}


	public static String add(String languageText) {		
		final String fLanguageText = languageText.replaceAll("\\|", "");
		String index = (indexedEntries.size()+1)+"";
		if(indexedEntries.containsKey(index))
			throw new Error();
		if(indexedEntries.containsValue(fLanguageText))
			return indexedEntries.keySet().stream().filter(x->indexedEntries.get(x).equals(fLanguageText)).findAny().get();
		indexedEntries.put(index, fLanguageText);
		
		addToRawTextDatabase(index,fLanguageText);
		
		return index;
	}
	
	private static void addToRawTextDatabase(String index, String toSave) {
		try {
			Files.write(LOCATION_RAW_LEARNING_TEXT,
					
					(index+"|"+toSave+"||\n").getBytes(),
				//	SpecialCharacterManager.ISO_CHARSET,
					StandardOpenOption.APPEND
					);
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error();
		}
	}


	private static Map<String, Integer> allOccurrencesCache = null;
	public static Map<String, Integer> getAllOccurrencesOfEveryWordEverIncludedInUserText() {
		if(allOccurrencesCache!=null)
			return allOccurrencesCache;
		try {
			String fulltext = Files.readString(LOCATION_RAW_LEARNING_TEXT,Charset.forName("ISO-8859-1"));
			List<String> allWords = TextInputUtils.toListOfWordsWithoutSymbols(fulltext);
			Map<String, Integer> countPerWord = new HashMap<>();
			for(String s:allWords)
			{
				if(!countPerWord.containsKey(s))countPerWord.put(s, 0);
				countPerWord.put(s, countPerWord.get(s)+1);
			}
			
			allOccurrencesCache = countPerWord;
			
			return countPerWord;
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error();
		}
	}


	public static Set<LanguageWord> getAllPossibleLanguageWords() {
		return getAllOccurrencesOfEveryWordEverIncludedInUserText().keySet()
				.stream()
				.map(x->Arrays.asList(
						LanguageWord.newInstance(x, LanguageCode.EN),
						LanguageWord.newInstance(x, LanguageCode.SV)
						)
						)
				.reduce(new ArrayList<LanguageWord>(),(x,y)->{x.addAll(y); return x;})
				.stream()
				.sorted((x,y)->x.toString().compareTo(y.toString()))
				.filter(x->Dictionnary.isInDictionnariesWithCrosscheck(x))
				.collect(Collectors.toSet());
	}

	private static Map<LanguageWord, Integer> cacheNumberOfOccurrencesOfTranslationsOfWordsInUserTexts = null;
	/**
	 * This function returns all the words for which there exist a translation of this word in user text.
	 * The returned list of words may include words that are not part of the text corpus, as long as one of their translations
	 * belong to the text corpus.
	 * @return
	 */
	public static synchronized Map<LanguageWord, Integer> getNumberOfOccurrencesOfTranslationsOfWordsThatHaveATranslationInUserText() {
		if(cacheNumberOfOccurrencesOfTranslationsOfWordsInUserTexts!=null)
			return cacheNumberOfOccurrencesOfTranslationsOfWordsInUserTexts;
		Set<LanguageWord> allWordsAndTheirTranslations = new HashSet<>();
		allWordsAndTheirTranslations.addAll(getAllPossibleLanguageWords());
		Set<LanguageWord> allTranslations =
				allWordsAndTheirTranslations.parallelStream()
				.map(x->Translator.getTranslationsOf(x))
				.reduce(new HashSet<LanguageWord>(),(Set<LanguageWord> z,Set<LanguageWord> y)->{synchronized (z) {
				z.addAll(y); return z;}});
		
		allWordsAndTheirTranslations
		.addAll(
				allTranslations);
		
		Map<LanguageWord, Integer> res = allWordsAndTheirTranslations.parallelStream().collect(Collectors.toMap(Function.identity(), x->
		Translator.getTranslationsOf(x).stream().map(y->getNumberOfOccurrencesInTheText(y)).reduce(0,(z,y)->z+y)));
		
		
		cacheNumberOfOccurrencesOfTranslationsOfWordsInUserTexts = res;
		return res;
	}


	public static int getNumberOfOccurrencesInTheText(LanguageWord x) {
		return getAllOccurrencesOfEveryWordEverIncludedInUserText().getOrDefault(x.getWord(),0);
	}


	public static int getNumberOfOccurrencesInUserTextOfTheTranslationsOfThisWord(LanguageWord word) {
		return UserLearningTextManager.getNumberOfOccurrencesOfTranslationsOfWordsThatHaveATranslationInUserText().getOrDefault(word,0);
	}
}
