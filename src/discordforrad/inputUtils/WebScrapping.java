package discordforrad.inputUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import discordforrad.LanguageCode;
import discordforrad.models.language.LanguageWord;
import webscrapping.RobotBasedPageReader;
import webscrapping.WebpageReader;

public class WebScrapping {
	
	private static final Path CACHE_FILEPATH = Paths.get("data/cache/");

	public static File getCacheFileNameFor(String webpage, LanguageWord lw)
	{
		String cacheLocation = null;
		if(webpage.contains("wordreference.com"))
			cacheLocation = "\\wordReference\\";
		else if(webpage.contains("bab.la"))
			cacheLocation = "\\babla\\";
		String cacheFileName = CACHE_FILEPATH.toString()
				+cacheLocation
				+lw.toString().replaceAll(":", "")+".html";

		return new File(cacheFileName);
	}
	
	public static String getContentsFromBabLa(LanguageWord lw)
	{
		String header = "https://";
		
		String languageInPlainText = null;
		String otherLanguageInPlainText = null;
		if(lw.getCode().equals(LanguageCode.EN))
		{
			languageInPlainText = "english";
			otherLanguageInPlainText = "swedish";
		}
		if(lw.getCode().equals(LanguageCode.SV))
		{
			languageInPlainText = "swedish";
			otherLanguageInPlainText = "english";
		}
		
		
		String pageCode = header+"en.bab.la/dictionary/"+languageInPlainText+"-"+otherLanguageInPlainText+
				"/"+lw.getWord();
		
		if(getCacheFileNameFor(pageCode,lw).exists())
		{
			if(getCacheFileNameFor(pageCode,lw).exists())
				try {
					// default StandardCharsets.UTF_8
					String content = Files.readString(getCacheFileNameFor(pageCode,lw).toPath(), StandardCharsets.ISO_8859_1);
					if(!content.contains("\n"))
						{
						getCacheFileNameFor(pageCode, lw).delete();
						return getContentsFromBabLa(lw);
						}
					return content;

				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		
				
		//String res = WebpageReader.downloadWebPage(pageCode, lw.toString());
		
		String res = RobotBasedPageReader.getFullPageAsHtml(pageCode);
		
		//issue with processing; try again
		if(!res.contains("\n"))
			return getContentsFromBabLa(lw);
		
		saveOnCache(pageCode,lw, res);
		return res;
	}

	public static String getContentsFromReferenceWord(LanguageWord lw) {
		String header = "https://";
		
		String pageToAskFor = header + getWordReferenceWebPageName(lw)+lw.getWord();


		if(getCacheFileNameFor(pageToAskFor,lw).exists())
			try {
				// default StandardCharsets.UTF_8
				String content = Files.readString(
						getCacheFileNameFor(pageToAskFor,lw).toPath(),
						StandardCharsets.ISO_8859_1);
				return content;

			} catch (IOException e) {
				e.printStackTrace();
			}

		//String res = RobotManager.getFullPageAsHtml(pageToAskFor);
		String res =// WebpageReader.traditionalWebPageContents(pageToAskFor);
				WebpageReader.downloadWebPage(pageToAskFor,lw.toString());
		
	//	System.out.println(res);
		saveOnCache(pageToAskFor,lw, res);
		return res;
	}
	
	private static void saveOnCache(String pageToAskFor, LanguageWord lw, String res) {
		File cacheFileName = getCacheFileNameFor(pageToAskFor, lw);
		BufferedWriter writer;
		try {
			cacheFileName.createNewFile();
			writer = new BufferedWriter(new FileWriter(cacheFileName,StandardCharsets.ISO_8859_1));

			writer.write(res);

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Trying to save at:"+cacheFileName);
		}
	}

	private static String getWordReferenceNameFor(LanguageCode code) {
		switch (code) {
		case SV:return "sven";
		case EN:return "ensv";
		default:
			throw new IllegalArgumentException("Unexpected value: " + code);
		}
	}
	
	public static boolean isWordInReferenceWord(LanguageWord lw) {
		
		String webPageContents = WebScrapping.getContentsFromReferenceWord(lw);
		if(!webPageContents.contains(getWordReferenceWebPageName(lw)))
			return false;

		boolean result = webPageContents.contains("Huvudsakliga översättningar")
				|| webPageContents.contains("is an alternate term for") 
				|| webPageContents.toLowerCase().contains("principal translations");
		
		//System.out.println(webPageContents);
		/*if(!result)
			System.out.println("Could not find "+lw+" in the dictionnary. Is it OK?");*/
		return result;
	}
	
	public static String getWordReferenceWebPageName(LanguageWord lw)
	{
		return "www.wordreference.com/"+getWordReferenceNameFor(lw.getCode())+"/";
	}

}
