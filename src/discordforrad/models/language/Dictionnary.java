package discordforrad.models.language;

import java.awt.AWTException;
import java.awt.Robot;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import cachingutils.impl.FileBasedStringSetCache;
import cachingutils.impl.PlainObjectFileBasedCache;
import cachingutils.impl.TextFileBasedCache;
import discordforrad.Main;
import discordforrad.inputUtils.TextInputUtils;
import discordforrad.inputUtils.WebScrapping;
import discordforrad.inputUtils.WebScrapping.DataBaseEnum;
import discordforrad.inputUtils.databases.BabLaProcessing;
import discordforrad.models.language.wordnetwork.forms.RelatedForms;
import discordforrad.models.language.wordnetwork.forms.RelatedFormsNetwork;
import discordforrad.models.language.wordnetwork.forms.RelatedFormsTransition;
import discordforrad.models.language.wordnetwork.forms.RelatedFormsTransitionImpl;
import discordforrad.models.learning.VocabularyLearningStatus;
import discordforrad.translation.Translator;
import webscrapping.RobotBasedPageReader;
import webscrapping.WebpageReader;

public class Dictionnary {
	
	private static final File VALID_SWEDISH_WORDS_DATABASE_FILE = Paths.get(Main.ROOT_DATABASE+"databases/valid_swedish_words_database.txt").toFile();
	private static final FileBasedStringSetCache<LanguageWord> validSwedishWordsCache = 
			FileBasedStringSetCache.loadCache(
					VALID_SWEDISH_WORDS_DATABASE_FILE.toPath(),
					LanguageWord::parse,
					x->x.toString());
	
	private static final File DATABASE_INVALID = Paths.get(Main.ROOT_DATABASE+"databases/invalid_words_database.txt").toFile();
	private static final FileBasedStringSetCache<LanguageWord> invalidWordCache = 	
			FileBasedStringSetCache.loadCache(
					DATABASE_INVALID.toPath(),
			LanguageWord::parse,
			x->x.toString());


	private static final File DATABASE_ENGLISH = Paths.get(Main.ROOT_DATABASE+"english_words.txt").toFile();

	private static final Set<String> ENGLISH_DICTIONNARY=new HashSet<>();
	
	static {		
		try {
		/*	int index = 0;
			for(String s: Files.readAllLines(DATABASE_ENGLISH.toPath()))
			{
				index++;
				System.out.println(index+" "+s);
				ENGLISH_DICTIONNARY.add(s);
				if(s.equals("ny"))
					System.out.println();
			}*/
				
				
			ENGLISH_DICTIONNARY.addAll(Files.readAllLines(DATABASE_ENGLISH.toPath())
					.parallelStream().collect(Collectors.toSet()));
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static boolean isInDictionnaries(LanguageWord lw)
	{
		/*if(lw.getCode().equals(LanguageCode.EN))
			return ENGLISH_DICTIONNARY.contains(lw.getWord());
		if(validWordCache.contains(lw))return true;

		else return false;
	//	return isInDictionnariesWithCrosscheck(lw);*/
		throw new Error();
	}
	
	public static boolean isInDictionnariesWithCrosscheck(LanguageWord lw)
	{
		if(lw.getCode().equals(LanguageCode.EN))
			return isEnglishWord(lw);
		if(validSwedishWordsCache.contains(lw))return true;
		if(invalidWordCache.contains(lw))return false;
		System.out.println("Searching for:"+lw+" in the dictionnaries.");

		boolean res = false;
		if(lw.getCode().equals(LanguageCode.SV))
			res = isSwedishWord(lw);
		else if(lw.getCode().equals(LanguageCode.EN))
			res = isEnglishWord(lw);
		else throw new Error();

		if(res)
			validSwedishWordsCache.add(lw);
		else
			invalidWordCache.add(lw);
		return res;
	}

	private static boolean isEnglishWord(LanguageWord lw) {
		return ENGLISH_DICTIONNARY.contains(lw.getWord());
	}

	private static boolean isSwedishWord(LanguageWord lw) {
		//	if(true) isInBabLaDisctionnary(lw);

		List<DataBaseEnum> databases = Arrays.asList(DataBaseEnum.values());
		return databases.stream().anyMatch(x->isConsideredDictionnaryForCheckingWordExistence(x)&&isInDictionnaryOf(lw,x));
/*				return true;
		
		return false;*/

	}

	private static boolean isConsideredDictionnaryForCheckingWordExistence(DataBaseEnum db) {
		if(db.equals(DataBaseEnum.BAB_LA)&&!BabLaProcessing.isBabLaDictionnaryConsideredForNewWords())
			return false;
		return true;
	}
	//Does not work great.
	private static boolean isFolketsLexikonWord(LanguageWord lw)
	{	 
		String pageToAskFor = "https://folkets-lexikon.csc.kth.se/folkets/"+'#'
				+lw.getWord().replaceAll("_", "%20");
		String webPageContents = WebpageReader.getWebclientWebPageContents(pageToAskFor).toString();
		System.out.println("----------------------");
		System.out.println(webPageContents);
		boolean result = !webPageContents.contains("automaträttades till")&&
				!webPageContents.contains("Ingen översättning av");
		return result;
	}
	
	
	public static Set<LanguageWord> getAllKnownWords()
	{
		Set<LanguageWord> res = new HashSet<>();
		res.addAll(ENGLISH_DICTIONNARY.stream().map(x->LanguageWord.newInstance(x, LanguageCode.EN)).collect(Collectors.toSet()));
		res.addAll(validSwedishWordsCache);
		return res;
	}


	public static void main(String args[]) throws AWTException, InterruptedException, IOException
	{
		FileBasedStringSetCache<LanguageWord> allWordsFromSaol = FileBasedStringSetCache.loadCache(new File(
				Main.ROOT_DATABASE+"caches/dictionnary_from_saol.txt").toPath(), LanguageWord::parse, x->x.toString());

		//new Thread(()->{
		//	try {
	//	checkInDictionnaryAllWordsOnSvenskaSe(allWordsFromSaol);
		//	} catch (InterruptedException e) {
		//		e.printStackTrace();
		//	}
		//}).start();

		Set<LanguageWord> nextIteration = new HashSet<>();

		AtomicInteger i = new AtomicInteger();
		do {
			i.set(0);
			nextIteration.clear();
			
			
			List<LanguageWord> shuffledWords = //new LinkedList<>();
					allWordsFromSaol
					.stream()
					.sorted((x,y)->x.toString().compareTo(y.toString()))
					.filter(x->
					{
						return validSwedishWordsCache.contains(x);//Dictionnary.isInDictionnariesWithCrosscheck(x);
					})
					.collect(Collectors.toList());
			
			
			shuffledWords.addAll(VocabularyLearningStatus.loadFromFile().getAllWords().stream()
					.filter(x->x.getCode().equals(LanguageCode.SV))
					.collect(Collectors.toSet()));
			
			Collections.shuffle(shuffledWords);
			
			System.out.println(shuffledWords.contains(LanguageWord.newInstance("runds", LanguageCode.SV)));
			
			long start = System.currentTimeMillis();
			shuffledWords
			.parallelStream()
			.sorted((x,y)->x.toString().compareTo(y.toString()))
			//.filter(x->x.getWord().equals("rund"))
			.forEach(lw->
			{
				/*if(lw.equals(LanguageWord.newInstance("rund", LanguageCode.SV)))
					System.out.println(validWordCache.size());*/
				if(!isInDictionnariesWithCrosscheck(lw))return;
				Translator.getTranslationsOf(lw);
				WordDescription.getDescriptionFor(lw);
				
				
				if(Main.PRELOAD_BABLA_GRUNDFORMS)
					for(LanguageWord lw2: WordDescription.getGrundforms(lw))
						BabLaProcessing.getBabLaTranslationDescriptions(lw2);
				
				RelatedFormsNetwork.getRelatedForms(lw);

				WordDescription description =WordDescription.getDescriptionFor(lw); 
				Set<LanguageWord> unknownRelatedWords = description.getAllAlternativeForms().stream().map(y->y.getRelatedWords())
						.reduce(new HashSet<>(), (x,y)->{x.addAll(y); return x;})
						.parallelStream()
						.filter(x->!allWordsFromSaol.contains(x))
						.collect(Collectors.toSet());

				nextIteration.addAll(unknownRelatedWords);
				long time = (System.currentTimeMillis()-start)/1000;
				double proportion = (i.doubleValue()*100d / (double) shuffledWords.size());
				double proportionPerSecond = proportion / time*3600;
				System.out.println(time+"  "+String.format("%.1f", proportion)+"  "+String.format("%.2f", proportionPerSecond)+"  "+i+"/"+shuffledWords.size()//+" "+s+" "+relatedWords.size()
				+" "+nextIteration.size()+" "+lw);
				i.incrementAndGet();
			//	if(WordDescription.cache.get().size()%10000==0) WordDescription.updateCache();
				
			});
			System.out.println("Next iteration:"+nextIteration.size()+" added to "+allWordsFromSaol.size());
			allWordsFromSaol.addAll(nextIteration);
//			allKnownWords.doAndUpdate(x->{x.addAll(nextIteration);});
			RelatedFormsNetwork.updateCache();
		}
		while(!nextIteration.isEmpty());





		/*Set<LanguageWord> toExplore = (Set) getFromFile("data/cache/all_words_to_explore.obj");
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
		}*/
	}

	private static void checkInDictionnaryAllWordsOnSvenskaSe(FileBasedStringSetCache<LanguageWord> allWordsFromSaol) throws InterruptedException {
		
		String prev = "prev";
		String current = "";
		
		RobotBasedPageReader.clickOn(200, 200);
		Thread.sleep(100);
		while(!current.equals(prev))
		{
			prev = current;
			current = RobotBasedPageReader.getFullPageAsText();
			if(current.equals(prev))
			{
				Thread.sleep(2000);
				current = RobotBasedPageReader.getFullPageAsText();
				/*if(current.equals(prev)) {
					RobotBasedPageReader.clickOn(1000, 1060);
					current = RobotBasedPageReader.getFullPageAsText();
				}*/
			}

			allWordsFromSaol.addAll(TextInputUtils.toListOfWordsWithoutSymbols(current)
					.stream().map(x->LanguageWord.newInstance(x, LanguageCode.SV))
					.collect(Collectors.toSet()));
			

			RobotBasedPageReader.clickOn(1000, 760);
			System.out.println(allWordsFromSaol.size());
			Thread.sleep(300);
		}
	}
	public static boolean isInDictionnaryOf(LanguageWord lw, DataBaseEnum db) {
		if((db.equals(DataBaseEnum.SAOL)||db.equals(DataBaseEnum.SO))&&!lw.getCode().equals(LanguageCode.SV))return false;
		return WebScrapping.isInDataBase(lw, db);
	}
	public static boolean isInPrecomputedDictionnaries(LanguageWord newInstance) {
		return validSwedishWordsCache.contains(newInstance);
	}
}
