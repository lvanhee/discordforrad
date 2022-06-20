package discordforrad.inputUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import cachingutils.advanced.failable.AttemptOutcome;
import cachingutils.advanced.failable.FailedDatabaseProcessingOutcome;
import cachingutils.advanced.failable.RequestSuccessfulButNoMatchingEntries;
import cachingutils.advanced.failable.SuccessfulOutcome;
import cachingutils.impl.SplittedFileBasedCache;
import discordforrad.Main;
import discordforrad.inputUtils.WebScrapping.DataBaseEnum;
import discordforrad.inputUtils.databases.BabLaProcessing;
import discordforrad.inputUtils.databases.WordReferenceDBManager;
import discordforrad.models.language.LanguageCode;
import discordforrad.models.language.LanguageWord;
import discordforrad.models.language.wordnetwork.forms.SingleEntryWebScrapping;
import webscrapping.RobotBasedPageReader;
import webscrapping.WebpageReader;
import webscrapping.WebpageReader.PageContentsResult;

public class WebScrapping {

	public enum DataBaseEnum{SAOL,SO,WORD_REFERENCE,BAB_LA}

	private static final Path CACHE_FILEPATH = Paths.get("caches/");

	private static final class DbLwPair
	{
		private final DataBaseEnum db;
		private final LanguageWord lw;

		private DbLwPair(DataBaseEnum db, LanguageWord lw) {
			this.db = db; this.lw = lw;
		}

		public static DbLwPair newInstance(DataBaseEnum db2, LanguageWord lw2) {
			return new DbLwPair(db2, lw2);
		}

	}

	public static File getCacheFileNameFor(DataBaseEnum db, LanguageWord lw)
	{
		String cacheFolder = "\\"+db.name()+"\\";

		String cacheFileName = Main.ROOT_DATABASE+CACHE_FILEPATH.toString()
		+cacheFolder
		+lw.toString().replaceAll(":", "")+".html";

		return new File(cacheFileName);
	}


	public static final SplittedFileBasedCache<DbLwPair, String> cache = SplittedFileBasedCache.newInstance(
			(DbLwPair dblw)-> getCacheFileNameFor(dblw.db, dblw.lw),
			Function.identity(),
			Function.identity()
			);



	private static final Object lockBabLaCalls = "BabLaLock";
	public static AttemptOutcome getContentsFrom(LanguageWord lw, DataBaseEnum db) {
	
		
		final DbLwPair inputPair = DbLwPair.newInstance(db, lw);

		if(!isPossibleEntryFor(lw,db))
			throw new Error();

		if(cache.has(inputPair))
		{
			String content = cache.get(inputPair);
			if(isRequestToBeReprocesse(db,content,lw))
			{
				cache.delete(inputPair);
				//	Files.delete(getCacheFileNameFor(db,lw).toPath());
				System.err.println("Deleting wrong input from:"+db+" "+lw);
				return getContentsFrom(lw, db);
				//		return SingleEntryWebScrapping.newInstance(content);
			}
			else return processToOutcomes(cache.get(inputPair),x->!WebScrapping.isRequestToBeReprocesse(db, x,lw),db);
		}
		

		Set<String> pageToAskFor =  getWebpagesFor(lw,db);


		//String res = RobotManager.getFullPageAsHtml(pageToAskFor);
		String entries =
				pageToAskFor.stream()
				.map(
						x->
						{
							final String startOfPageHtmlHeader = "<!--!!!START_OF_PAGE "+x+"!!!-->\n";

							PageContentsResult res = WebpageReader.getWebclientWebPageContents(x);
							if(res.fromCache && isRequestToBeReprocesse(db, res.res, lw))
								res = WebpageReader.getWebclientWebPageContents(x,true);

							String servedPage =res.res;

							switch (db) {
							case WORD_REFERENCE:case SO:case SAOL:
								return startOfPageHtmlHeader+servedPage;
							case BAB_LA:
								String r = null;

								r = servedPage;
								if(r.equals("NO_PAGE_TO_BE_SERVED"))return r;
								while(r.contains("\n "))
									r = r.replaceAll("\n ", "\n");
								r = r.replaceAll("\n", "");
								r = r.replaceAll("\r", "");
								if(r.contains("<title>English-Swedish dictionary - translation - bab.la</title>")
										||r.contains("<title>Swedish-English dictionary - translation - bab.la</title>"))
									return "NO_PAGE_TO_BE_SERVED";
								return startOfPageHtmlHeader+r;
							default:
								throw new Error();
							}
						}
						)
				.filter(x->!x.equals("NO_PAGE_TO_BE_SERVED"))
				/*.filter(x->
				{
					if(db.equals(DataBaseEnum.SO))
					{
						String filtered = x.replaceAll(" ","").replaceAll("\n", "").replaceAll("\r", "");
						boolean resu = filtered.contains("<title>"+lw.getWord()); 
						return resu;
					}
					return true;
				}
						)*/
				.reduce("", (x,y)->x+"\n"+y);

		if(isRequestToBeReprocesse(db, entries,lw))
		{
			if(db.equals(DataBaseEnum.WORD_REFERENCE))
			{
				System.err.println("Word reference database has been already overused today");
				//hasWordReferenceBeenOverloaded = true;
			}
			return FailedDatabaseProcessingOutcome.FAILED;
		}

		synchronized(cache)
		{ 
			if(!cache.has(inputPair))
				cache.add(inputPair,entries);
		}

		return processToOutcomes(entries, x->!WebScrapping.isRequestToBeReprocesse(db, x,lw), db);
	}

	private static Set<String> getWebpagesFor(LanguageWord lw, DataBaseEnum db) {
		Set<String> res = new HashSet<>();
		switch(db)
		{
		case WORD_REFERENCE:res.add("https://" + getWordReferenceWebPageName(lw)+lw.getWord()); break;
		case SO:case SAOL:
			String indicator = db.toString().toLowerCase();
			String last = "https://svenska.se/"+indicator+"/?sok="+lw.getWord();
			res.add(last); 
			PageContentsResult result = WebpageReader.getWebclientWebPageContents(last);
			if(result.fromCache && isRequestToBeReprocesse(db, result.res, lw))
			{
				WebpageReader.getWebclientWebPageContents(last,true);
			}

			String page = result.res;
			if(page.contains("<div class=\"cshow\">"))
			{
				page = page.substring(page.indexOf("<div class=\"cshow\">"));
				page = page.substring(0, page.indexOf("</div>"));

				for(String sub : page.split("<a class=\"slank\" href=\""))
				{
					if(sub.startsWith("<div class=\"cshow\">"))continue;
					String nextPage = sub.substring(0, sub.indexOf("\""));
					res.add("https://svenska.se"+nextPage);
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
			res.add("https://en.bab.la/dictionary/"+languageInPlainText+"-"+otherLanguageInPlainText+"/"+lw.getWord());
			break;

		default:throw new Error();
		}
		
		return res;
	}

	private static AttemptOutcome<Set<String>> processToOutcomes(String entry, Predicate<String> checker, DataBaseEnum db) {
		if(! entry.contains("<!--!!!START_OF_PAGE "))return SuccessfulOutcome.newInstance(Arrays.asList(entry).stream().collect(Collectors.toSet()));
		List<String> entries = Arrays.asList(entry.split("<!--!!!START_OF_PAGE "));

		Set<String> res = entries
				.stream()
				.filter(x->!x.isBlank())
				.filter(checker)
				.collect(Collectors.toSet());

		if(res.size()==0)
			return FailedDatabaseProcessingOutcome.FAILED;
		
		res = res.stream().filter(x->!isRequestSuccessfulButNoTheResponseIndicatesNoMatchingEntry(entry,db)).collect(Collectors.toSet());

		return SuccessfulOutcome.newInstance(res);
	}

	private static boolean isRequestSuccessfulButNoTheResponseIndicatesNoMatchingEntry(String entry, DataBaseEnum db) {
		switch (db) {
		case WORD_REFERENCE: {
			if(entry.contains("Inget uppslagsord hittat för"))
				return true;
		}
		//default:
		//	throw new IllegalArgumentException("Unexpected value: " + db);
		}
		return false;
	}

	public static boolean isRequestToBeReprocesse(DataBaseEnum db, String content, LanguageWord lw) {
		if(db.equals(DataBaseEnum.WORD_REFERENCE)
				&&WordReferenceDBManager.isDatabaseDenyingService(content))
			return true;

		if(db.equals(DataBaseEnum.BAB_LA))
			return ! BabLaProcessing.isValidlyProcessedRequest(content,lw);

		if(content.equals("FAILED TO LOAD"))
			return true;

		return false;
	}

	private static boolean isPossibleEntryFor(LanguageWord lw, DataBaseEnum db) {
		if(db.equals(DataBaseEnum.SO)&&lw.getCode()!=LanguageCode.SV)return false;

		return true;
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
		AttemptOutcome<Set<String>> contents = WebScrapping.getContentsFrom(lw,db);
		

		if(contents instanceof FailedDatabaseProcessingOutcome)return false;
		
		Set<String> s = ((SuccessfulOutcome<Set<String>>)contents).getResult();
		s = s.stream().filter(x->isInDatabaseFromText(x, lw, db)).collect(Collectors.toSet());
		
		return !s.isEmpty();

	/*	if(contents instanceof SingleEntryWebScrapping)
			return isInDatabaseFromText(((SingleEntryWebScrapping)contents).get(), lw, db);
		
		Set<String> entries = ((EntriesFoundWebscrappingOutcome)contents).getEntries();

		for(String st: entries)
			if(isInDatabaseFromText(st,lw,db))return true;
		return false;*/
	}

	private static boolean isInDatabaseFromText(String webPageContents, LanguageWord lw, DataBaseEnum db) {


		switch(db)
		{
		case WORD_REFERENCE:
			if(!webPageContents.contains(getWordReferenceWebPageName(lw)))
				return false;

			boolean result = webPageContents.contains("Huvudsakliga översättningar")
					|| webPageContents.contains("is an alternate term for") 
					|| webPageContents.toLowerCase().contains("principal translations");
			if(webPageContents.contains("WordReference kan inte översätta just den här frasen, men klicka på varje enskilt ord för att se vad det betyder:"))
				result = false;
			return result;

		case SO:case SAOL:
			String toStudy = webPageContents.replaceAll("\n", "").replaceAll("\r", "").replaceAll(" ", "");
			boolean resultNotFound =toStudy.contains("Sökningenpå<strong>"+lw.getWord().replaceAll(" ", "")+"</strong>iSOgavingasvar.")||
					toStudy.contains("Sökningenpå<strong>"+lw.getWord().replaceAll(" ", "")+"</strong>iSAOLgavingasvar."); 
			return !resultNotFound;
		case BAB_LA:
			String otherLanguageInPlainText = null;
			if(lw.getCode().equals(LanguageCode.EN))
			{
				otherLanguageInPlainText = "swedish";
			}
			if(lw.getCode().equals(LanguageCode.SV))
			{
				otherLanguageInPlainText = "english";
			}

			if(webPageContents.toLowerCase()
					.contains("our team was informed that the translation for \""+lw.getWord()+"\" is missing"))
				return false;	

			result = webPageContents.toLowerCase().contains("\""+lw.getWord()+"\" in "+otherLanguageInPlainText);

			return result;
		default : throw new Error();
		}
	}

	public static String getWordReferenceWebPageName(LanguageWord lw)
	{
		return "www.wordreference.com/"+getWordReferenceNameFor(lw.getCode())+"/";
	}

}
