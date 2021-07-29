package discordforrad.models.language;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import discordforrad.LanguageCode;
import discordforrad.inputUtils.TextInputUtils;
import discordforrad.inputUtils.WebScrapping;
import discordforrad.inputUtils.WebScrapping.DataBaseEnum;
import webscrapping.WebpageReader;

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
			synchronized (validWordCache) {
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
		}
		else
		{
			synchronized (invalidWordCache) {			
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
		isInBabLaDisctionnary(lw);

		if(WebScrapping.isInDataBase(lw))
			return true;
		if(isInBabLaDisctionnary(lw))
			return true;

		return false;
	}

	private static boolean isSwedishWord(LanguageWord lw) {
		if(true) isInBabLaDisctionnary(lw);


		if(isInWordReferenceDictionnary(lw))
			return true;

		if(isInBabLaDisctionnary(lw))
			return true;
		//if(isFolketsLexikonWord(lw))
		//	return true;

		//System.out.println("Could not find "+lw+" in the dictionnaries. Is it OK?");
		return false;

	}

	public static boolean isInBabLaDisctionnary(LanguageWord lw) {

		String webPageContents = WebScrapping.getContentsFrom(lw, DataBaseEnum.BAB_LA);

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


	public static void main(String args[])
	{
		Set<LanguageWord> toExplore = (Set) getFromFile("data/cache/all_words_to_explore.obj");
		Set<LanguageWord> explored = (Set) getFromFile("data/cache/all_explored_words.obj");
		if(toExplore.isEmpty())
		{
			toExplore.add(new LanguageWord(LanguageCode.EN, "start"));
			explored.clear();
		}


		toExplore.add(LanguageWord.newInstance("beginning", LanguageCode.EN));

		while(!toExplore.isEmpty())
		{
			System.out.println("Done:\t\t"+explored.size());
			System.out.println("Remaining:\t"+toExplore.size()+"\n");

			LanguageWord next = toExplore.iterator().next();
			toExplore.remove(next);
			if(explored.contains(next)) continue;
			explored.add(next);

			

			String loadingContents = 
					Arrays.asList(DataBaseEnum.values())
					.stream()
					.map(x->WebScrapping.getContentsFrom(next, x))
					.reduce("", (x,y)->x+" "+y);

			MP3Loader.getFluxFromBabLa(next);

			Set<LanguageWord> nextSet = 
					TextInputUtils.toListOfWords(loadingContents)
					.stream()
					.map(
							x->

							Arrays.asList(
									LanguageWord.newInstance(x, LanguageCode.EN),
									LanguageWord.newInstance(x, LanguageCode.SV)
									)
							)
					.reduce(
							new LinkedList<>(), (x,y)->{x.addAll(y); return x;})
					.stream()
					.filter(x->!explored.contains(x))
					.collect(Collectors.toSet());
			toExplore.addAll(nextSet);

			if(explored.size()%200==0)
			{
				saveToFile("data/cache/all_words_to_explore.obj",toExplore);
				saveToFile("data/cache/all_explored_words.obj",explored);
			}
		}
	}

	private static void saveToFile(String string, Set<LanguageWord> toExplore) {
		try {
			FileOutputStream file = new FileOutputStream(string);
			ObjectOutputStream output = new ObjectOutputStream(file);
			output.writeObject(toExplore);
			output.close();
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error();
		}
	}

	private static Object getFromFile(String s)
	{
		try {
			FileInputStream fis = new FileInputStream(s);
			ObjectInputStream ois = new ObjectInputStream(fis);
			return ois.readObject();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return new HashSet<>();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		throw new Error();

	}

	public static boolean isInDictionnaryOf(LanguageWord lw, DataBaseEnum db) {
		return WebScrapping.isInDataBase(lw, db);
	}


}
