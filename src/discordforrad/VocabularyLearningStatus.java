package discordforrad;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class VocabularyLearningStatus {
	private static final Path FILEPATH = Paths.get("data/learned_words.txt");
	private static final Path RECORD_FILEPATH = Paths.get("data/raw_text_database.txt");
	private static final int MAX_LEARNED = 10;
	private final Map<LanguageWord, Integer> successfulLearningPerWord;
	private final Map<LanguageWord, LocalDateTime> timeLastAttempt;
	private final Set<LanguageText> rawTextDatabase ;
	
	
	public VocabularyLearningStatus(
			Map<LanguageWord, Integer> m, 
			Map<LanguageWord, LocalDateTime> m2)
	{
		this.successfulLearningPerWord = m;
		this.timeLastAttempt = m2;
		
		rawTextDatabase = LanguageText.parse(RECORD_FILEPATH);
		
	}


	public static VocabularyLearningStatus loadFromFile() throws IOException {
		Map<LanguageWord, Integer> m = new HashMap<>();
		Map<LanguageWord, LocalDateTime> m2 = new HashMap<>();
		
		Charset cs = Charset.forName("ISO-8859-1");
		for(String line: Files.lines(FILEPATH,cs).collect(Collectors.toSet()))
		{
			if(line.isEmpty())continue;
			if(line.contains("ã"))continue;
			if(line.contains("¬"))continue;
			if(line.contains("£"))continue;

			String[] split = line.split(";");
			String word = split[0];
			LanguageCode lc = LanguageCode.valueOf(split[1]);
			int number = Integer.parseInt(split[2]);
			LocalDateTime time = LocalDateTime.parse(split[3]);
			LanguageWord lw = new LanguageWord(lc, word);

			m.put(lw, number);
			m2.put(lw, time);
		}
		return new VocabularyLearningStatus(m, m2);
	}


	public void addFreeString(LanguageText lt, AddStringResultContext c, boolean isOriginalString) {
		String rawText = clearOfSymbols(lt.getText());

		while(rawText.startsWith(" "))rawText = rawText.substring(1);
		
		if(!isOriginalString) rawText = rawText.replaceAll(" ", "_");
		for(String word : rawText.split(" "))
		{
			if(word.isEmpty())continue;
			LanguageWord lw = new LanguageWord(lt.getLanguageCode(), word);
			if(!successfulLearningPerWord.containsKey(lw))
			{
				c.addResult(lw);
				successfulLearningPerWord.put(lw, 0);
				timeLastAttempt.put(lw, LocalDateTime.MIN);
				
				for(String translation: Translator.getTranslation(word.replaceAll("_", " "), lt.getLanguageCode(), LanguageCode.otherLanguage(lt.getLanguageCode())))
				addFreeString(
						new LanguageText(
								translation,
										LanguageCode.otherLanguage(lt.getLanguageCode())),c, false);
			}
		}

		if(isOriginalString)
			try {
				updateFile();

				addToRawTextDatabase(lt);
			} catch (IOException e) {
				e.printStackTrace();
				throw new Error();
			}
	}

	private void addToRawTextDatabase(LanguageText lt) {
		String string = lt.getText();
		while(string.startsWith("\n"))
			string = string.substring(1);
		rawTextDatabase.add(new LanguageText(string, lt.getLanguageCode()));
		updateRawTextDatabase();
	}


	private void updateRawTextDatabase() {
		String res = "";
		for(LanguageText s:rawTextDatabase)
			res+=s.getLanguageCode()+"|"+s.getText().replaceAll("|","")+"|\n";
		try {
			Files.writeString(RECORD_FILEPATH, res,Charset.forName("ISO-8859-1"));
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error();
		}
	}


	private void updateFile() throws IOException {
		String res = "";
		for(LanguageWord s: successfulLearningPerWord.keySet())
			res+=s.getWord()+";"+s.getCode()+";"+successfulLearningPerWord.get(s)+";"+timeLastAttempt.get(s)+"\n";
		
		Charset cs = Charset.forName("ISO-8859-1");
		Files.writeString(FILEPATH, res,cs);
	}


	public String toString() {return successfulLearningPerWord.toString();}

	public void resetWord(String string, String countryCode) throws IOException {
		successfulLearningPerWord.get(countryCode).put(string,0);
		updateFile();
	}


	/*public String overlay(String string) {
		String res = "";
		
		for(String s: string.split(" "))
		{
			String sCleared = clearOfSymbols(s);
			if(is)
			
		}
			
	}*/


	public static String clearOfSymbols(String string) {
		string = string.replaceAll(",", " ");
		string = string.replaceAll("^", " ");
		string = string.replaceAll("”", " ");
		string = string.replaceAll("\\.", " ");
		string = string.replaceAll(";", " ");
		string = string.replaceAll("\\(", " ");
		string = string.replaceAll("\\)", " ");
		string = string.replaceAll("\\]", " ");
		string = string.replaceAll("\\[", " ");
		string = string.toLowerCase();
		string = string.replaceAll("\n", " ");
		string = string.replaceAll("[0-9]", "");
		while(string.contains("  "))
			string = string.replaceAll("  ", " ");
		
		return string;
	}


	public double knownRatio(String string) {
		String res = "";

		int known = 0;
		int total = 0;
		for(String s: string.split(" "))
		{
			String sCleared = clearOfSymbols(s);
			if(isMastered(sCleared)) known++;
			total++;
		}
		
		return (double)known/total;
	}


	private boolean isMastered(String s, String countryCode) {
		return successfulLearningPerWord.get(countryCode).get(s)>MAX_LEARNED;
	}


	/*public double vocabularyKnownRatio() {
		return (double)successfulLearningPerWordEN.values().parallelStream().filter(x->x>MAX_LEARNED)
				.count()/successfulLearningPerWordEN.size();
	}*/


	public Set<LanguageWord> getAllWords() {
		return successfulLearningPerWord.keySet();
	}


	public int getNumberOfSuccessLearning(LanguageWord s) {
		return successfulLearningPerWord.get(s);
	}


	public LocalDateTime getLastSuccessOf(LanguageWord s) {
		return timeLastAttempt.get(s);
	}


	public void incrementSuccess(LanguageWord lastWordAsked) throws IOException {
		timeLastAttempt.put(lastWordAsked, LocalDateTime.now());
		successfulLearningPerWord.put(lastWordAsked,successfulLearningPerWord.get(lastWordAsked)+1);
		updateFile();
	}


	public void decrementSuccessUpToOne(LanguageWord lastWordAsked) throws IOException {
		timeLastAttempt.put(lastWordAsked, LocalDateTime.now());
		successfulLearningPerWord.put(lastWordAsked,successfulLearningPerWord.get(lastWordAsked)-1);
		if(successfulLearningPerWord.get(lastWordAsked)<1)
			successfulLearningPerWord.put(lastWordAsked,1);
		updateFile();
	}


	public Set<LanguageWord> getAllShortTermWords() {
		return successfulLearningPerWord.keySet().stream().filter(x->successfulLearningPerWord.get(x)<=1).collect(Collectors.toSet());
	}


	public Set<LanguageWord> getAllMidTermWords() {
		return successfulLearningPerWord.keySet().stream().filter(x->successfulLearningPerWord.get(x)>=1 && successfulLearningPerWord.get(x)<=7 ).collect(Collectors.toSet());
	}


	public Set<LanguageWord> getAllLongTermWords() {
		return successfulLearningPerWord.keySet().stream().filter(x->successfulLearningPerWord.get(x)>7 ).collect(Collectors.toSet());
	}




	

}
