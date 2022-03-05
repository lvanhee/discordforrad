package discordforrad.discordmanagement.audio;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import discordforrad.inputUtils.DatabaseProcessingOutcome;
import discordforrad.inputUtils.WebScrapping;
import discordforrad.inputUtils.WebScrapping.DataBaseEnum;
import discordforrad.inputUtils.databases.BabLaProcessing;
import discordforrad.models.LanguageCode;
import discordforrad.models.language.LanguageWord;
import discordforrad.models.language.WordDescription;
import discordforrad.models.language.wordnetwork.WordNetwork;
import discordforrad.models.language.wordnetwork.forms.SingleEntryWebScrapping;
import webscrapping.WebpageReader;

public class LocalAudioDatabase {
	private static final File LOCAL_AUDIO_DATABASE = new File("../databases/discordforrad/mp3/");
	public static boolean hasAudioFileFor(LanguageWord currentWordToAsk) {
		/*return hasDirectAudioFileFor(currentWordToAsk) ||
				hasGrundformAudioFileFor(cu)*/
		File mp3FileLocation = getAudioFileFor(currentWordToAsk);
		if(!mp3FileLocation.exists())
			getFluxFromBabLa(currentWordToAsk);
		/*
		if(mp3FileLocation.exists())
			return true;
		if(!mp3FileLocation.exists())
		{
			Set<LanguageWord> grundforms = WordDescription.getGrundforms(currentWordToAsk);
			for(LanguageWord lw:grundforms)
				if(lw.getWord().length()<currentWordToAsk.getWord().length() &&
						hasAudioFileFor(lw))
					return true;
			return false;
		}*/

		return ( getAudioFileFor(currentWordToAsk).exists());
	}

	public static File getAudioFileFor(LanguageWord lw) {
		
		return new File(LOCAL_AUDIO_DATABASE.getAbsolutePath()+"\\"+lw.getCode()+lw.getWord()+".mp3");
	}

	public static void getFluxFromBabLa(LanguageWord lw) {
		if(getAudioFileFor(lw).exists())
			return;
		DatabaseProcessingOutcome result = WebScrapping.getContentsFrom(lw,DataBaseEnum.BAB_LA);
		String page = null;
		if(result instanceof SingleEntryWebScrapping)
			page = ((SingleEntryWebScrapping)result).get();
		else throw new Error();

		List<String> lines = 
				Arrays.asList(page.split("\n"))
				.stream()
				.filter(x->x.contains(".mp3"))
				.map(x->x.substring(x.indexOf("https://")))
				.collect(Collectors.toList());

		lines.forEach(x->
		{
			String URL = x.substring(0,x.indexOf(".mp3")+4);
			String next = x.substring(x.indexOf(".mp3")+5);
			String nextName = next.substring(next.indexOf("'")+1);
			nextName = nextName.replaceAll("\\\\'", "")
					.replaceAll("-", "");
			
			nextName = nextName.substring(0,nextName.indexOf("'")).trim();
			LanguageCode language = null;
			if(next.replaceAll(" ", "").contains("'en'"))
				language = LanguageCode.EN;
			if(next.replaceAll(" ", "").contains("'sv'"))
				language = LanguageCode.SV;
			if(language==null)
				throw new Error();
			if(nextName.contains(":"))
			{
				System.err.println("Issue getting the MP3");
				return;
			}
			
			File res =  getAudioFileFor(LanguageWord.newInstance(nextName, language));
			if(!res.exists())
				WebpageReader.downloadFileFrom(URL,res);
		}
				);
	}

	public static void playAsynchronouslyIfHasAudioFile(LanguageWord currentWordToAsk) {
		Runnable r = ()->
		{
		if(LocalAudioDatabase.hasAudioFileFor(currentWordToAsk))
			LocalAudioPlayer.play(LocalAudioDatabase.getAudioFileFor(currentWordToAsk));
			}; 
		new Thread(r).start();
	}
}
