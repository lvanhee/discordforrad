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
import java.util.stream.Collectors;

import cachingutils.PlainObjectFileBasedCache;
import discordforrad.LanguageCode;
import discordforrad.Translator;
import discordforrad.inputUtils.TextInputUtils;
import discordforrad.inputUtils.WebScrapping;
import discordforrad.inputUtils.WebScrapping.DataBaseEnum;
import discordforrad.inputUtils.databases.BabLaProcessing;
import discordforrad.models.VocabularyLearningStatus;
import discordforrad.models.language.wordnetwork.forms.RelatedFormsNetwork;
import webscrapping.RobotBasedPageReader;
import webscrapping.WebpageReader;

public class Dictionnary {

	private static final File DATABASE_VALID = Paths.get("data/cache/words_in_dictionnary.obj").toFile();
	private static final PlainObjectFileBasedCache<Set<LanguageWord>> validWordCache = PlainObjectFileBasedCache.loadFromFile(DATABASE_VALID, ()->new HashSet<>());
	private static final Set<LanguageWord> invalidWordCache = new HashSet<>();

	private static final File DATABASE_INVALID = Paths.get("data/cache/invalid_words.obj").toFile();
	private static final File DATABASE_ENGLISH = Paths.get("data/english_words.txt").toFile();
	private static final Set<String> ENGLISH_DICTIONNARY=new HashSet<>();
	
	static {
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
		
		try {
			ENGLISH_DICTIONNARY.addAll(Files.readAllLines(DATABASE_ENGLISH.toPath())
					.stream().collect(Collectors.toSet()));		
			System.out.println(ENGLISH_DICTIONNARY);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}



	public static boolean isInDictionnaries(LanguageWord lw)
	{		
		if(lw.getCode().equals(LanguageCode.EN))
			return ENGLISH_DICTIONNARY.contains(lw.getWord());
		if(validWordCache.get().contains(lw))return true;

		else return false;
	}
	public static boolean isInDictionnariesWithCrosscheck(LanguageWord lw)
	{
		if(validWordCache.get().contains(lw))return true;
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
				validWordCache.get().add(lw);
				if(validWordCache.get().size()%5000==0)
					validWordCache.doAndUpdate(x->{System.out.println("Updating word cache:"+validWordCache.get().size());});
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

		

		for(DataBaseEnum db:DataBaseEnum.values())
			if(isInDictionnaryOf(lw,db)) return true;
		return false;
	}

	private static boolean isSwedishWord(LanguageWord lw) {
		//	if(true) isInBabLaDisctionnary(lw);

		for(DataBaseEnum db:DataBaseEnum.values())
			if(isConsideredDictionnaryForCheckingWordExistence(db)&&isInDictionnaryOf(lw,db)) 
				return true;
		
		return false;

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
		String webPageContents = WebpageReader.getWebclientWebPageContents(pageToAskFor);
		System.out.println("----------------------");
		System.out.println(webPageContents);
		boolean result = !webPageContents.contains("automaträttades till")&&
				!webPageContents.contains("Ingen översättning av");
		return result;
	}


	public static void main(String args[]) throws AWTException, InterruptedException, IOException
	{
		String current = "";
		String prev = "prev";
		PlainObjectFileBasedCache<Set<String>> allKnownWords = PlainObjectFileBasedCache.loadFromFile(new File("data/cache/dictionnary_from_saol.obj"), ()->new HashSet<>());
		Robot r = new Robot();

		/*RobotBasedPageReader.clickOn(200, 200);
		Thread.sleep(100);
		while(!current.equals(prev))
		{
			prev = current;
			current = RobotBasedPageReader.copyPasteWholePage(r);
			if(current.equals(prev))
			{
				Thread.sleep(2000);
				current = RobotBasedPageReader.copyPasteWholePage(r);
			}

			Set<String> newWords = TextInputUtils.toListOfWords(current)
					.stream().filter(x->!result.get().contains(x))
					.collect(Collectors.toSet());
			System.out.println(newWords);
			result.get().addAll(newWords);

			result.doAndUpdate((x)->{});


			RobotBasedPageReader.clickOn(1000, 1040);
			System.out.println(result.get().size());
			Thread.sleep(300);
		}
		
		result.doAndUpdate(x->{});*/

		Set<String> nextIteration = new HashSet<>();

		AtomicInteger i = new AtomicInteger();
		do {
			nextIteration.clear();
			
			List<String> shuffledWords = //new LinkedList<>();
					allKnownWords.get()
					.stream()
					.filter(x->Dictionnary.isInDictionnariesWithCrosscheck(LanguageWord.newInstance(x, LanguageCode.SV)))
					.collect(Collectors.toList());
			shuffledWords.addAll(VocabularyLearningStatus.loadFromFile().getAllWords().stream()
					.filter(x->x.getCode().equals(LanguageCode.SV))
					
					.map(x->x.getWord()).collect(Collectors.toSet()));
			Collections.shuffle(shuffledWords);
			
			long start = System.currentTimeMillis();
			shuffledWords
			.parallelStream()
			.forEach(s->
			{
				LanguageWord lw = LanguageWord.newInstance(s, LanguageCode.SV);
			//	isInDictionnaryOf(lw,DataBaseEnum.SAOL);
			//	isInDictionnaryOf(lw,DataBaseEnum.SO);
			//	isInDictionnaryOf(lw,DataBaseEnum.BAB_LA);
			//	isInDictionnaryOf(lw,DataBaseEnum.WORD_REFERENCE);
			//	Translator.getGoogleTranslation(lw, LanguageCode.EN);
				
				if(!isInDictionnariesWithCrosscheck(lw))return;


				Set<String> relatedWords = WordDescription.getDescriptionFor(lw).getAllAlternativeForms().stream().map(y->y.getRelatedWords())
						.reduce(new HashSet<>(), (x,y)->{x.addAll(y); return x;})
						.parallelStream()
						.map(x->x.getWord())
						.filter(x->!allKnownWords.get().contains(x))
						//.filter(x->isInDictionnaries(LanguageWord.newInstance(x,LanguageCode.SV)))
						.collect(Collectors.toSet());

				nextIteration.addAll(relatedWords);
				long time = (System.currentTimeMillis()-start)/1000;
				double proportion = (i.doubleValue()*100d / (double) shuffledWords.size());
				double proportionPerSecond = proportion / time*3600;
				System.out.println(time+"  "+String.format("%.1f", proportion)+"  "+String.format("%.2f", proportionPerSecond)+"  "+i+"/"+shuffledWords.size()//+" "+s+" "+relatedWords.size()
				+" "+nextIteration.size());
				i.incrementAndGet();
			//	if(WordDescription.cache.get().size()%10000==0) WordDescription.updateCache();
				
			});
			System.out.println("Next iteration:"+nextIteration.size()+" added to "+allKnownWords.get().size());
			allKnownWords.doAndUpdate(x->{x.addAll(nextIteration);});
			WordDescription.updateCache();
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

	public static boolean isInDictionnaryOf(LanguageWord lw, DataBaseEnum db) {
		if((db.equals(DataBaseEnum.SAOL)||db.equals(DataBaseEnum.SO))&&!lw.getCode().equals(LanguageCode.SV))return false;
		return WebScrapping.isInDataBase(lw, db);
	}
}
