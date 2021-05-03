package discordforrad.languageModel;

import discordforrad.LanguageCode;
import discordforrad.inputUtils.WebpageReader;

public class Dictionnary {
	
	public static boolean isInDictionnaries(LanguageWord lw)
	{
		System.out.println("Searching for:"+lw+" in the dictionnaries.");

		if(lw.getCode().equals(LanguageCode.SV))
			return isSwedishWord(lw);
		if(lw.getCode().equals(LanguageCode.EN))
			return isEnglishWord(lw);
		throw new Error();
	}

	private static boolean isEnglishWord(LanguageWord lw) {
		if(isWordInReferenceWord(lw))
			return true;
		if(isInBabLaReferenceWord(lw))
			return true;
		
		return false;
		
		
	}

	private static boolean isSwedishWord(LanguageWord lw) {
		if(isWordInReferenceWord(lw))
			return true;
		
		if(isInBabLaReferenceWord(lw))
			return true;
		//if(isFolketsLexikonWord(lw))
		//	return true;
		
		//System.out.println("Could not find "+lw+" in the dictionnaries. Is it OK?");
		return false;
		
	}

	private static boolean isInBabLaReferenceWord(LanguageWord lw) {
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
		
		String webPageContents = WebpageReader.downloadWebPage(pageCode);
		if(webPageContents.toLowerCase()
				.contains("our team was informed that the translation for \""+lw.getWord()+"\" is missing"))
			return false;	
		
		boolean result = webPageContents.toLowerCase().contains("\""+lw.getWord()+"\" in "+otherLanguageInPlainText);

		return result;
	}

	//Does not work great.
	private static boolean isFolketsLexikonWord(LanguageWord lw)
	{	 
		String pageToAskFor = "https://folkets-lexikon.csc.kth.se/folkets/"+'#'
				+lw.getWord().replaceAll("_", "%20");
		String webPageContents = WebpageReader.downloadWebPage(pageToAskFor);
		System.out.println("----------------------");
		System.out.println(webPageContents);
		boolean result = !webPageContents.contains("automaträttades till")&&
				!webPageContents.contains("Ingen översättning av");
		return result;
	}

	private static boolean isWordInReferenceWord(LanguageWord lw) {
		String header = "https://";
		String pageCode = "www.wordreference.com/"+getWordReferenceNameFor(lw.getCode())+"/";
		String pageToAskFor = header + pageCode+lw.getWord();
		String webPageContents = WebpageReader.downloadWebPage(pageToAskFor);
		if(!webPageContents.contains(pageCode))
			return false;
		boolean result = webPageContents.contains("Matchande uppslagsord från andra sidan av ordboken.")
				|| webPageContents.contains("is an alternate term for") 
				|| webPageContents.toLowerCase().contains("principal translations");
		
		//System.out.println(webPageContents);
		/*if(!result)
			System.out.println("Could not find "+lw+" in the dictionnary. Is it OK?");*/
		return result;
	}

	private static String getWordReferenceNameFor(LanguageCode code) {
		switch (code) {
		case SV:return "sven";
		case EN:return "ensv";
		default:
			throw new IllegalArgumentException("Unexpected value: " + code);
		}
	}

}
