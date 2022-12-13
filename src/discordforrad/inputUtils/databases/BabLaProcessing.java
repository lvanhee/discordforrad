package discordforrad.inputUtils.databases;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import cachingutils.advanced.failable.AttemptOutcome;
import cachingutils.advanced.failable.FailedDatabaseProcessingOutcome;
import cachingutils.advanced.failable.SuccessfulOutcome;
import cachingutils.impl.FileBasedStringSetCache;
import cachingutils.impl.TextFileBasedCache;
import discordforrad.Main;
import discordforrad.inputUtils.WebScrapping;
import discordforrad.inputUtils.WebScrapping.DataBaseEnum;
import discordforrad.models.language.Dictionnary;
import discordforrad.models.language.LanguageCode;
import discordforrad.models.language.LanguageText;
import discordforrad.models.language.LanguageWord;
import discordforrad.models.language.WordDescription;
import discordforrad.models.language.WordDescription.WordType;
import discordforrad.translation.ResultOfTranslationAttempt;
import discordforrad.translation.SuccessfulTranslationDescription;
import discordforrad.translation.TranslationQuery;
import discordforrad.translation.Translator;

public class BabLaProcessing {
	
	private static final File PATH_TO_BAB_LA_TRANSLATIONS_DATABASE
	= new File(Main.ROOT_DATABASE+"caches/precomputed_babla_translations.txt");
	
	private static final File PATH_TO_BAB_LA_FAILED_TO_BE_TRANSLATED_DATABASE
	= new File(Main.ROOT_DATABASE+"caches/words_that_are_not_translated_by_babla.txt");

	private static final FileBasedStringSetCache<TranslationQuery> wordsWithoutEntryInBabla=
			FileBasedStringSetCache.loadCache(PATH_TO_BAB_LA_FAILED_TO_BE_TRANSLATED_DATABASE.toPath(), TranslationQuery::parse, x->x.toString());
	
	private static final TextFileBasedCache<TranslationQuery,Set<ResultOfTranslationAttempt>> babLaPrecomputedTranslations=
			TextFileBasedCache.newInstance(
					PATH_TO_BAB_LA_TRANSLATIONS_DATABASE, x->x.toString(), 
					TranslationQuery::parse, x->ResultOfTranslationAttempt.toParsableString(x), ResultOfTranslationAttempt::parseSet, "\t");


	public static boolean isBabLaDictionnaryConsideredForNewWords() {
		return false;
	}

	public static boolean isWordTranslatedByBabLa(TranslationQuery tq) {
		if(wordsWithoutEntryInBabla.contains(tq))
			return false;
		
		if(babLaPrecomputedTranslations.has(tq))
			return babLaPrecomputedTranslations.get(tq).isEmpty();
		
		
		AttemptOutcome<Set<String>> outcome = WebScrapping.getContentsFrom(tq, DataBaseEnum.BAB_LA);
		
		if(outcome instanceof FailedDatabaseProcessingOutcome)//babla failure is assumed to be a "no result" outcome
		{
			wordsWithoutEntryInBabla.add(tq);
			return false;
		}
		
		Set<String> str = ((SuccessfulOutcome<Set<String>>)outcome).getResult();
		if(str.isEmpty())
		{
			wordsWithoutEntryInBabla.add(tq);
			return false;
		}
		
		if(str.size()!=1)
			throw new Error();
		
		String babLaInput = str.iterator().next();
		boolean missing = (babLaInput.contains("Our team was informed that the translation for \""+tq.getWord().getWord()+"\" is missing."));
		boolean notInDictionnary = babLaInput.contains("\""+tq.getWord().getWord()+"\" is currently not in our dictionary.");
		
		String nameForm = null;
		switch (tq.getWord().getCode()) {
		case SV: nameForm = "English"; break;
		case EN: nameForm = "Swedish"; break;
		default:
			throw new IllegalArgumentException("Unexpected value: " + tq.getWord().getCode());
		}
		boolean inRightForm = babLaInput.toLowerCase().contains("\""+tq.getWord().getWord().toLowerCase()+"\" in "+nameForm.toLowerCase());
		
		boolean res = !missing && !notInDictionnary && inRightForm; 
		
		if(!res)
			wordsWithoutEntryInBabla.add(tq);
		
		return res;
	}

	public static Set<ResultOfTranslationAttempt> getBabLaTranslationDescriptions(TranslationQuery tq)
	{
		if(!isWordTranslatedByBabLa(tq))
			return new HashSet<>();
		if(babLaPrecomputedTranslations.has(tq))
			return babLaPrecomputedTranslations.get(tq);
		AttemptOutcome<Set<String>> outcome = WebScrapping.getContentsFrom(tq, DataBaseEnum.BAB_LA);
		
		Set<String> resO = ((SuccessfulOutcome<Set<String>>)outcome).getResult();
		if(resO.size()!=1) throw new Error();
		String babLaInput = resO.iterator().next();
		
		String reduced = Arrays.asList(babLaInput.split("\n")).stream().map(x->x.trim()).reduce("",(x,y)->x+y);
		
	/*	while(babLaInput.contains("\n "))
			babLaInput = babLaInput.replaceAll("\n ","\n");
		babLaInput=	babLaInput.replaceAll("\n", "").replaceAll("\r", "");
		
		System.out.println(reduced.equals(babLaInput));
		for(int i = 0 ; i < reduced.length(); i++)
			if(reduced.charAt(i)!=babLaInput.charAt(i))
			{
				char c1 = reduced.charAt(i);
				char c2 = babLaInput.charAt(i);
				System.out.println(i+" "+c1+" "+c2);
			}*/
		
		babLaInput = reduced;
		
		String headerToLookFor="<h2 class=\"h1\">\""+tq.getWord()+"\" in ";
		if(!babLaInput.contains(headerToLookFor))
			return new HashSet<>();
		
		LanguageCode to = LanguageCode.otherLanguage(tq.getCode());
	
		List<String> definitions = WordDescription.getBabLaDefinitionsFrom(tq,babLaInput); 
		
		 Set<ResultOfTranslationAttempt> res = definitions
					.stream().map(
							x->SuccessfulTranslationDescription.newInstance(
									LanguageText.newInstance(to,x),"",
									WordType.UNDEFINED, ResultOfTranslationAttempt.Origin.BAB_LA)).collect(Collectors.toSet()); 
		 
		 if(res.isEmpty())
			 wordsWithoutEntryInBabla.add(tq);
		 else
			 babLaPrecomputedTranslations.add(tq, res);
		return res;
	}

	private static double processingSpeedFactor = 1d;
	public static double getProcessingSpeedFactor() {
		return processingSpeedFactor;
	}

	public static boolean isValidlyProcessedRequest(String content, LanguageWord lw) {
		String header = lw.getWord()
				.toUpperCase().replaceAll("-", " ")
				.replace("[", "")
				.replace("]", "")
				;
		String countryCode = null;
		String otherCountryCode = null;
		if(lw.getCode().equals(LanguageCode.SV))
		{
			otherCountryCode = "Swedish";
			countryCode = "English";
		}
		if(lw.getCode().equals(LanguageCode.EN))
		{
			countryCode = "Swedish";
			otherCountryCode = "English";
		}
		String toSearch = "<title>"+header+" - Translation in "+countryCode+" - bab.la</title>";
		String searchedTheWrongWay = "<title>"+header+" - Translation in "+otherCountryCode+" - bab.la</title>";
		String toSearchAbriged = "<title>"+header+" - Translation in ";
		String isNotInDictionnary = "\""+lw.getWord()+"\" is currently not in our dictionary.";
		String babLaFailure = "<title>Swedish-English dictionary - translation - bab.la</title>";
		
		if(content.length()<1000)
			return false;
		if(content.contains("<title>HTTPS:EN.BAB.LADICTIONARY"))
			return false;
		if(content.contains("<title>Discord</title>"))
			return false;
		if(content.contains(toSearch))
			return true;
		else if(content.contains(isNotInDictionnary))
			return true;
		else if(content.contains(searchedTheWrongWay))
			return true;
		if(content.contains(babLaFailure))
			return true;
		else 
			if(lw.getWord().length()<5&&content.contains(toSearchAbriged))
				return true;
			else if(content.contains("/"+lw.getWord()+"\" onclick"))
				return true;
			else
				return false;
	}

	public synchronized static void increaseProcessingTime() {
		processingSpeedFactor*=1.5;
		System.out.println(processingSpeedFactor);
		if(processingSpeedFactor>10)
			System.out.println();
	}

	public synchronized static void decreaseProcessingTime() {
		processingSpeedFactor*=0.95;
		if(processingSpeedFactor>5)
			processingSpeedFactor=5;
		System.out.println(processingSpeedFactor);
		//if(processingSpeedFactor<1) processingSpeedFactor = 1;
	}
	
	public static void main(String[] args)
	{
		//System.out.println(getBabLaTranslationDescriptions(LanguageWord.newInstance("vartdera", LanguageCode.SV)));
		
		AtomicInteger i = new AtomicInteger();
	/*	for(LanguageWord lw: Dictionnary.getAllKnownWords()
				.stream().sorted((x,y)->x.toString().compareTo(y.toString())).collect(Collectors.toList()))
		{
			i++;
			Set<ResultOfTranslationAttempt> res =
					getBabLaTranslationDescriptions(lw);
			if(res.isEmpty())
			{
				System.err.print(i+":\t"+lw);
				getBabLaTranslationDescriptions(lw);
			}
			else System.out.print(i+":\t\t"+lw);
			System.out.println();
		}*/
		

		Set<LanguageWord> wordsToExplore = Dictionnary.getAllKnownWords();
		Set<LanguageWord> exploredWords = new HashSet<>();
		
		new Thread(()->{
			while(!wordsToExplore.isEmpty())
			{
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.out.println("BABLA PROCESSING:"+wordsToExplore.size()+" "+exploredWords.size());
			}
			
		}).start();
		
		
		while(!wordsToExplore.isEmpty())
		{
			LanguageWord lw = wordsToExplore.iterator().next();
			wordsToExplore.remove(lw);
			if(exploredWords.contains(lw)) continue;
			exploredWords.add(lw);
			
			
			wordsToExplore.addAll(Translator.getTranslationsOf(lw));
			
			//if(!BabLaProcessing.isWordTranslatedByBabLa(lw)) continue;
			//wordsToExplore.addAll(BabLaProcessing.getBabLaTranslationDescriptions(lw))
			
		}
		
		System.out.println("BAB LA COMPLETED");
		
		/*Dictionnary.getAllKnownWords()
		.stream()
		.filter(x->BabLaProcessing.isWordTranslatedByBabLa(x))
		.sorted((x,y)->x.toString().compareTo(y.toString()))
		.forEach(lw->{
			i.incrementAndGet();
			Set<ResultOfTranslationAttempt> res =
					getBabLaTranslationDescriptions(lw);
			if(res.isEmpty())
			{
				System.err.print(i+":\t"+lw);
				getBabLaTranslationDescriptions(lw);
			}
			else
				{
				System.out.print(i+":\t\t"+lw);
				LocalAudioDatabase.getFluxFromBabLa(lw);

				}
			System.out.println();
		});*/
	}

	public static List<String> getBabLaDirectTranslationsFrom(LanguageWord lw, String babLaInput) {
		String languageToTranslateTo = null;
		if(lw.getCode().equals(LanguageCode.EN)) languageToTranslateTo = "Swedish";
		else if(lw.getCode().equals(LanguageCode.SV)) languageToTranslateTo = "English";
		
		String startDelimitor = ("<h2 class=\"h1\">\""
				+lw.getWord()+"\" in "+languageToTranslateTo).toLowerCase();
		
		int startDelimitation = babLaInput.toLowerCase().indexOf(startDelimitor);
		if(startDelimitation==-1)return new ArrayList<>();
		String inputForDefinitions =  babLaInput
				.substring(startDelimitation+startDelimitor.length());
		inputForDefinitions = inputForDefinitions.substring(0,inputForDefinitions.indexOf("<h2"));
		
//		List<String> splitted = Arrays.asList(inputForDefinitions.split("class=\"babQuickResult\">"));
		
		List<String> allProposedTranslations = Arrays.asList(inputForDefinitions.split("class=\"babQuickResult\">"))
				.stream()
				.map(x->{
					if(x.contains("class=\"scroll-link babQuickResult\">Translations</a></li>"))
					{
						String res =x.substring(0, x.indexOf("class=\"scroll-link babQuickResult\">Translations</a></li>")); 
						return res.substring(0, res.lastIndexOf("<li>"));
					}
					else return x;
						})
//				.filter(x->!x.contains("class=\"scroll-link babQuickResult\">Translations</a></li>"))
				.filter(x->!x.contains("class=\"scroll-link babQuickResult\">Translations & Examples</a></li>"))
				.collect(Collectors.toList());
		
		List<String> allProposedTranslationsWithTheRightWord = allProposedTranslations
				.stream()
				.filter(x->x.toLowerCase().startsWith(lw.getWord().toLowerCase()+"<"))
				.collect(Collectors.toList());
		
		List<String> allLeftSideItemsBeforeFilteringOut = 
				allProposedTranslationsWithTheRightWord.stream().map(x->Arrays.asList(x.split("<li>")))
				.reduce(new ArrayList<>(), (x,y)->{x.addAll(y); return x;});
		
		List<String> allTranslationsOfTheRightWordWithNonTranslationsBeingFilteredOut = allLeftSideItemsBeforeFilteringOut
				.stream().filter(x->x.startsWith("<a ")).collect(Collectors.toList());
		
		if(allTranslationsOfTheRightWordWithNonTranslationsBeingFilteredOut.size()==0)
			return new LinkedList<>();
		/*allTranslationsOfTheRightWord = allTranslationsOfTheRightWord.subList(1, allTranslationsOfTheRightWord.size());
		if(allTranslationsOfTheRightWord.size()==0)
			throw new Error();*/
		
		allTranslationsOfTheRightWordWithNonTranslationsBeingFilteredOut = allTranslationsOfTheRightWordWithNonTranslationsBeingFilteredOut.stream()
				.map(x->{
				if(!x.contains("title="))
					throw new Error();
				return x.substring(x.indexOf("title="));
				})
				.map(x->x.substring(x.indexOf(">")+1))
				.map(x->x.substring(0,x.indexOf("<")))
				//.map(x->x.substring(x.lastIndexOf(">")+1,x.length()))
				.collect(Collectors.toList());
		if(allTranslationsOfTheRightWordWithNonTranslationsBeingFilteredOut.contains("Translations & Examples"))
			allTranslationsOfTheRightWordWithNonTranslationsBeingFilteredOut=allTranslationsOfTheRightWordWithNonTranslationsBeingFilteredOut.subList(0, allTranslationsOfTheRightWordWithNonTranslationsBeingFilteredOut.indexOf("Translations & Examples"));
		return allTranslationsOfTheRightWordWithNonTranslationsBeingFilteredOut;
	}

}
