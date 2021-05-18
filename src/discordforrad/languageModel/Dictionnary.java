package discordforrad.languageModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import discordforrad.LanguageCode;
import discordforrad.inputUtils.WebpageReader;

public class Dictionnary {
	
	private static final Set<LanguageWord> validWordCache = new HashSet<>();
	private static final Set<LanguageWord> invalidWordCache = new HashSet<>();
	private static final File DATABASE_VALID = Paths.get("data/cache/valid_words.obj").toFile();
	private static final File DATABASE_INVALID = Paths.get("data/cache/invalid_words.obj").toFile();
	static {
		try {
			if(DATABASE_VALID.exists()) {
				FileInputStream fileIn = new FileInputStream(DATABASE_VALID);
				ObjectInputStream objectIn = new ObjectInputStream(fileIn);

				Object obj = objectIn.readObject();
				objectIn.close();
				validWordCache.addAll((Set)obj);}

		} catch (Exception ex) {
			ex.printStackTrace();
        }
		
		try {
			if(DATABASE_INVALID.exists()) {
				FileInputStream fileIn = new FileInputStream(DATABASE_INVALID);
				ObjectInputStream objectIn = new ObjectInputStream(fileIn);

				Object obj = objectIn.readObject();
				objectIn.close();
				invalidWordCache.addAll((Set)obj);}

		} catch (Exception ex) {
			ex.printStackTrace();
        }
	}
	
	
	
	public static boolean isInDictionnaries(LanguageWord lw)
	{
		if(validWordCache.contains(lw))return true;
		if(invalidWordCache.contains(lw))return false;
		System.out.println("Searching for:"+lw+" in the dictionnaries.");

		boolean res = false;
		if(lw.getCode().equals(LanguageCode.SV))
			res = isSwedishWord(lw);
		else if(lw.getCode().equals(LanguageCode.EN))
			res = isEnglishWord(lw);
		else throw new Error();
		
		if(res) {
			validWordCache.add(lw);
			try {
	            FileOutputStream fileOut = new FileOutputStream(DATABASE_VALID);
	            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
	            objectOut.writeObject(validWordCache);
	            objectOut.close(); 
	        } catch (Exception ex) {
	            ex.printStackTrace();
	        }
		}
		else
		{
			invalidWordCache.add(lw);
			try {
	            FileOutputStream fileOut = new FileOutputStream(DATABASE_INVALID);
	            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
	            objectOut.writeObject(invalidWordCache);
	            objectOut.close(); 
	        } catch (Exception ex) {
	            ex.printStackTrace();
	        }
		}
		return res;
	}

	private static boolean isEnglishWord(LanguageWord lw) {
		if(lw.getWord().contains("å")||
				lw.getWord().contains("Å")||
				lw.getWord().contains("ö")||
				lw.getWord().contains("Ö")||
				lw.getWord().contains("ä")||
				lw.getWord().contains("Ä")
				) return false;
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
		
		String webPageContents = WebpageReader.downloadWebPage(pageCode, lw.toString());
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
		String webPageContents = WebpageReader.downloadWebPage(pageToAskFor, lw.toString());
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
		String webPageContents = WebpageReader.downloadWebPage(pageToAskFor,lw.toString());
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
