package discordforrad.inputUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import discordforrad.LanguageCode;
import discordforrad.models.language.LanguageWord;
import webscrapping.RobotBasedPageReader;
import webscrapping.WebpageReader;

public class WebScrapping {

	public enum DataBaseEnum{
		SO,SAOL,WORD_REFERENCE,BAB_LA
	}

	private static final Path CACHE_FILEPATH = Paths.get("data/cache/");

	public static File getCacheFileNameFor(DataBaseEnum db, LanguageWord lw)
	{
		String cacheFolder = "\\"+db.name()+"\\";

		String cacheFileName = CACHE_FILEPATH.toString()
				+cacheFolder
				+lw.toString().replaceAll(":", "")+".html";

		return new File(cacheFileName);
	}

	public static String getContentsFromBabLa(LanguageWord lw)
	{


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



		saveOnCache(pageCode,lw, res);
		return res;
	}

	public static String getContentsFrom(LanguageWord lw, DataBaseEnum db) {
		/*db = DataBaseEnum.SVENSKA_SE;
		lw  =LanguageWord.newInstance("katt", LanguageCode.SV);*/
		Set<String> pageToAskFor = new HashSet<>();

		if(!isPossibleEntryFor(lw,db))return null;
		switch(db)
		{
		case WORD_REFERENCE:pageToAskFor.add("https://" + getWordReferenceWebPageName(lw)+lw.getWord()); break;
		case SO:case SAOL:
			String indicator = db.toString().toLowerCase();
			String last = "https://svenska.se/"+indicator+"/?sok="+lw.getWord();
			pageToAskFor.add(last); 
			String page = WebpageReader.downloadWebPage(last);
			if(page.contains("<div class=\"cshow\">"))
			{
				page = page.substring(page.indexOf("<div class=\"cshow\">"));
				page = page.substring(0, page.indexOf("</div>"));

				for(String sub : page.split("<a class=\"slank\" href=\""))
				{
					if(sub.startsWith("<div class=\"cshow\">"))continue;
					String nextPage = sub.substring(0, sub.indexOf("\""));
					pageToAskFor.add("https://svenska.se"+nextPage);
				}
			}
			break;
		case BAB_LA: 		
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
			pageToAskFor.add("https://en.bab.la/dictionary/"+languageInPlainText+"-"+otherLanguageInPlainText+"/"+lw.getWord());
			break;
			
		default:throw new Error();
		}


		if(getCacheFileNameFor(db,lw).exists())
			try {
				// default StandardCharsets.UTF_8
				String content = Files.readString(
						getCacheFileNameFor(db,lw).toPath(),
						StandardCharsets.ISO_8859_1);
				return content;

			} catch (IOException e) {
				e.printStackTrace();
			}



		//String res = RobotManager.getFullPageAsHtml(pageToAskFor);
		String res =
				pageToAskFor.stream()
				.map(
						x->
						{
							final String startOfPageHtmlHeader = "<!--!!!START_OF_PAGE "+x+"!!!-->\n";
							switch (db) {
							case WORD_REFERENCE:case SO:case SAOL:
								return startOfPageHtmlHeader+WebpageReader.downloadWebPage(x);
							case BAB_LA: 	
								String r = null;
								while(r==null||!r.contains("\n"))
								{
									r = RobotBasedPageReader.getFullPageAsHtml(x);
									System.out.println("Failure getting the page as HTML");
								}
								return startOfPageHtmlHeader+r;
							default:
								throw new Error();
							}
						}
						)
				.filter(x->
				{
					if(db.equals(DataBaseEnum.SO))
					{
						String filtered = x.replaceAll(" ","").replaceAll("\n", "").replaceAll("\r", "");
						boolean resu = filtered.contains("<title>"+lw.getWord()); 
						return resu;
					}
					return true;
				}
					)
				.reduce("", (x,y)->x+"\n"+y);

		saveOnCache(db,lw, res);
		return res;
	}

	private static boolean isPossibleEntryFor(LanguageWord lw, DataBaseEnum db) {
		if(db.equals(DataBaseEnum.SO)&&lw.getCode()!=LanguageCode.SV)return false;

		return true;
	}

	private static void saveOnCache(DataBaseEnum db, LanguageWord lw, String res) {
		File cacheFileName = getCacheFileNameFor(db, lw);
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

	public static boolean isInDataBase(LanguageWord lw, DataBaseEnum db) {

		if(!isPossibleEntryFor(lw, db))return false;
		String webPageContents = WebScrapping.getContentsFrom(lw,db);

		switch(db)
		{
		case WORD_REFERENCE:
			if(!webPageContents.contains(getWordReferenceWebPageName(lw)))
				return false;

			boolean result = webPageContents.contains("Huvudsakliga översättningar")
					|| webPageContents.contains("is an alternate term for") 
					|| webPageContents.toLowerCase().contains("principal translations");
			return result;

		case SO:case SAOL:
			String toStudy = webPageContents.replaceAll("\n", "").replaceAll("\r", "").replaceAll(" ", "");
			boolean resultNotFound =toStudy.contains("Sökningenpå<strong>"+lw.getWord().replaceAll(" ", "")+"</strong>iSOgavingasvar."); 
			return !resultNotFound;

		default : throw new Error();
		}
	}

	public static String getWordReferenceWebPageName(LanguageWord lw)
	{
		return "www.wordreference.com/"+getWordReferenceNameFor(lw.getCode())+"/";
	}

}
