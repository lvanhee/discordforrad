package discordforrad.inputUtils.databases;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import discordforrad.discordmanagement.DiscordManager;
import discordforrad.models.VocabularyLearningStatus;
import discordforrad.models.language.LanguageWord;

public class DatabaseFiller {
	
	public static void main(String[] args) throws IOException
	{
		VocabularyLearningStatus vls = VocabularyLearningStatus.loadFromFile();
				
		AtomicInteger i = new AtomicInteger();
		Set<LanguageWord> allWords = vls.getAllExposableNewLanguageWords(); 
		vls.getAllExposableNewLanguageWords()
		.stream().sorted((x,y)->x.toString().compareTo(y.toString()))
		.forEach(
				x->{System.out.println("\n\n"+i.incrementAndGet()+"/"+allWords.size()+" "+x+"\n");DiscordManager.getHiddenAnswerStringFor(x);});
		
		new Thread(()->{BabLaProcessing.main(args);}).start();

	}

}
