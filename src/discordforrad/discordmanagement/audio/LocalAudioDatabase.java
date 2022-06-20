package discordforrad.discordmanagement.audio;

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import cachingutils.advanced.failable.AttemptOutcome;
import cachingutils.advanced.failable.FailedDatabaseProcessingOutcome;
import cachingutils.advanced.failable.SuccessfulOutcome;
import discordforrad.inputUtils.WebScrapping;
import discordforrad.inputUtils.WebScrapping.DataBaseEnum;
import discordforrad.models.language.LanguageCode;
import discordforrad.models.language.LanguageWord;
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
		AttemptOutcome<Set<String>> result = WebScrapping.getContentsFrom(lw,DataBaseEnum.BAB_LA);
		if(result instanceof FailedDatabaseProcessingOutcome)
			return;
		
		String page = null;
		Set<String> outcomes = ((SuccessfulOutcome<Set<String>>)result).getResult();
		if(outcomes.size()!=1)
			throw new Error();
		page = outcomes.iterator().next();

		
		page = page.replaceAll("&gt;", ">");

		Set<String> lines = 
				Arrays.asList(page.split("href=\"javascript:babTTS"))
				.stream()
				.filter(x->x.contains(".mp3"))
				.map(x->x.substring(x.indexOf("https://"),x.indexOf(">")))						
				.collect(Collectors.toSet());

		lines.forEach(x->
		{
			String URL = x.substring(0,x.indexOf(".mp3")+4);
			String next = x.substring(x.indexOf(".mp3")+5);
			String wordAttachedToTheFile = next.substring(next.indexOf("'")+1);
			wordAttachedToTheFile = wordAttachedToTheFile.replaceAll("\\\\'", "")
					.replaceAll("-", "");
			wordAttachedToTheFile = wordAttachedToTheFile.substring(0,wordAttachedToTheFile.indexOf("'")).trim();
			
			LanguageCode language = null;
			if(next.replaceAll(" ", "").contains("'en'"))
				language = LanguageCode.EN;
			if(next.replaceAll(" ", "").contains("'sv'"))
				language = LanguageCode.SV;
			if(language==null)
				throw new Error();
			if(wordAttachedToTheFile.contains(":"))
			{
				System.err.println("Issue getting the MP3");
				return;
			}
			
			File res =  getAudioFileFor(LanguageWord.newInstance(wordAttachedToTheFile, language));
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
