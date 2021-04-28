package discordforrad;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class VocabularyLearningStatus {
	private static final Path FILEPATH = Paths.get("input/learned_words.txt");
	private static final int MAX_LEARNED = 10;
	private final Map<String, Integer> successfulLearningPerWord;
	
	
	public VocabularyLearningStatus(Map<String, Integer> m) {
		this.successfulLearningPerWord = m;
	}


	public static VocabularyLearningStatus loadFromFile() throws IOException {
		return new VocabularyLearningStatus(Files.lines(FILEPATH).collect(Collectors.toMap(
				x->x.substring(0,x.indexOf(";")), 
				x->Integer.parseInt(x.substring(x.indexOf(";")+1)))));
	}


	public void addString(String string) throws IOException {
		string = clearOfSymbols(string);
		
		for(String word : string.split(" "))
		{
			if(!successfulLearningPerWord.containsKey(word))
				successfulLearningPerWord.put(word, 0);
		}
		
		updateFile();
	}
	
	private void updateFile() throws IOException {
		String res = "";
		for(String s: successfulLearningPerWord.keySet())
			res+=s+";"+successfulLearningPerWord.get(s)+"\n";
		
		Files.writeString(FILEPATH, res);
	}


	public String toString() {return successfulLearningPerWord.toString();}


	public Set<String> getFreshVocabulary() {
		return successfulLearningPerWord.keySet().parallelStream()
				.filter(x->successfulLearningPerWord.get(x)<5)
				.collect(Collectors.toSet());
	}


	public void incrementSuccess(String string) throws IOException {
		successfulLearningPerWord.put(string, successfulLearningPerWord.get(string)+1);
		updateFile();
	}


	public void resetWord(String string) throws IOException {
		successfulLearningPerWord.put(string,0);
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


	private String clearOfSymbols(String string) {
		string = string.replaceAll(",", "");
		string = string.replaceAll("\\.", "");
		string = string.replaceAll(";", "");
		
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


	private boolean isMastered(String s) {
		return successfulLearningPerWord.get(s)>MAX_LEARNED;
	}


	public double vocabularyKnownRatio() {
		return (double)successfulLearningPerWord.values().parallelStream().filter(x->x>MAX_LEARNED)
				.count()/successfulLearningPerWord.size();
	}

}
