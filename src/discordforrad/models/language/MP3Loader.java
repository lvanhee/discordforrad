package discordforrad.models.language;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.gargoylesoftware.htmlunit.javascript.host.file.File;

import discordforrad.LanguageCode;
import discordforrad.inputUtils.WebScrapping;
import discordforrad.inputUtils.WebScrapping.DataBaseEnum;
import webscrapping.WebpageReader;

public class MP3Loader {

	public static void getFluxFromBabLa(LanguageWord lw) {
		if(Files.exists(Paths.get(getMP3FileNameFor(lw))))
			return;
	String page = WebScrapping.getContentsFrom(lw,DataBaseEnum.BAB_LA);
	
	//List<String> lines = 
			Arrays.asList(page.split("\n"))
			.stream()
			.filter(x->x.contains(".mp3"))
			.map(x->x.substring(x.indexOf("https://")))
			.forEach(x->
			{
				String URL = x.substring(0,x.indexOf(".mp3")+4);
				String next = x.substring(x.indexOf(".mp3")+5);
				String nextName = next.substring(next.indexOf("'")+1);
				nextName = nextName.replaceAll("\\\\'", "");
				nextName = nextName.substring(0,nextName.indexOf("'"));
				LanguageCode language = null;
				if(next.replaceAll(" ", "").contains("'en'"))
					language = LanguageCode.EN;
				if(next.replaceAll(" ", "").contains("'sv'"))
					language = LanguageCode.SV;
				if(language==null)
					throw new Error();
				
				if(language.equals(lw.getCode())&&nextName.equals(lw.getWord())) {
					WebpageReader.downloadFileFrom(URL, getMP3FileNameFor(lw));
				}
			}
			);
	
	//throw new Error();
	}

	private static String getMP3FileNameFor(LanguageWord lw) {
		return "data/cache/mp3/"+lw.getCode()+lw.getWord()+".mp3";
	}

}
