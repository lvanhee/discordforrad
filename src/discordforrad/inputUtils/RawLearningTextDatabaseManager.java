package discordforrad.inputUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class RawLearningTextDatabaseManager {
	
	private static final Path LOCATION_RAW_LEARNING_TEXT = Paths.get("data/raw_text_database.txt");
	
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
		String index = indexedEntries.size()+"";
		if(indexedEntries.containsKey(index))
			throw new Error();
		if(indexedEntries.containsValue(languageText))
			return indexedEntries.keySet().stream().filter(x->indexedEntries.get(x).equals(languageText)).findAny().get();
		indexedEntries.put(index, languageText);
		
		addToRawTextDatabase(index,languageText);
		
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
}
