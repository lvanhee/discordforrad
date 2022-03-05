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

import cachingutils.FileBasedStringSetCache;
import cachingutils.TextFileBasedCache;
import discordforrad.Main;
import discordforrad.inputUtils.DatabaseProcessingOutcome;
import discordforrad.inputUtils.FailedDatabaseProcessingOutcome;
import discordforrad.inputUtils.WebScrapping;
import discordforrad.inputUtils.WebScrapping.DataBaseEnum;
import discordforrad.models.LanguageCode;
import discordforrad.models.language.Dictionnary;
import discordforrad.models.language.LanguageText;
import discordforrad.models.language.LanguageWord;
import discordforrad.models.language.SuccessfulTranslationDescription;
import discordforrad.models.language.ResultOfTranslationAttempt;
import discordforrad.models.language.ResultOfTranslationAttempt.Origin;
import discordforrad.models.language.WordDescription;
import discordforrad.models.language.WordDescription.WordType;
import discordforrad.models.language.wordnetwork.forms.SingleEntryWebScrapping;
import discordforrad.translation.TranslationOutcome;

public class BabLaProcessing {
	
	private static final File PATH_TO_BAB_LA_TRANSLATE_DATABASE
	= new File(Main.ROOT_DATABASE+"databases/words_that_are_not_translated_by_babla.txt");

	private static final FileBasedStringSetCache<LanguageWord> babLaFailedTranslationOutcomeCache=
			FileBasedStringSetCache.loadCache(PATH_TO_BAB_LA_TRANSLATE_DATABASE.toPath(), LanguageWord::parse, x->x.toString());


	public static boolean isBabLaDictionnaryConsideredForNewWords() {
		return false;
	}

	public static boolean isWordTranslatedByBabLa(LanguageWord lw) {
		if(babLaFailedTranslationOutcomeCache.contains(lw))
			return false;
		
		DatabaseProcessingOutcome outcome = WebScrapping.getContentsFrom(lw, DataBaseEnum.BAB_LA);
		if(outcome instanceof FailedDatabaseProcessingOutcome)
		{
			babLaFailedTranslationOutcomeCache.add(lw);
			return false;
		}
		String babLaInput = ((SingleEntryWebScrapping)outcome).get();
		boolean missing = (babLaInput.contains("Our team was informed that the translation for \""+lw.getWord()+"\" is missing."));
		boolean notInDictionnary = babLaInput.contains("\""+lw+"\" is currently not in our dictionary.");
		
		String nameForm = null;
		switch (lw.getCode()) {
		case SV: nameForm = "English"; break;
		case EN: nameForm = "Swedish"; break;
		default:
			throw new IllegalArgumentException("Unexpected value: " + lw.getCode());
		}
		boolean inRightForm = babLaInput.toLowerCase().contains("\""+lw.getWord().toLowerCase()+"\" in "+nameForm.toLowerCase());
		
		return !missing && !notInDictionnary && inRightForm;
	}

	public static Set<ResultOfTranslationAttempt> getBabLaTranslationDescriptions(LanguageWord lw)
	{
		if(!isWordTranslatedByBabLa(lw))
			return new HashSet<>();
		DatabaseProcessingOutcome outcome = WebScrapping.getContentsFrom(lw, DataBaseEnum.BAB_LA);
		
		String babLaInput = ((SingleEntryWebScrapping)outcome).get();
		
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
		
		String headerToLookFor="<h2 class=\"h1\">\""+lw.getWord()+"\" in ";
		if(!babLaInput.contains(headerToLookFor))
			return new HashSet<>();
		
		LanguageCode to = LanguageCode.otherLanguage(lw.getCode());
	
		List<String> definitions = WordDescription.getBabLaDefinitionsFrom(lw,babLaInput); 
		
		 Set<ResultOfTranslationAttempt> res = definitions
					.stream().map(
							x->SuccessfulTranslationDescription.newInstance(
									LanguageText.newInstance(to,x),"",
									WordType.UNDEFINED, ResultOfTranslationAttempt.Origin.BAB_LA)).collect(Collectors.toSet()); 
		 
		 if(res.isEmpty())
			 babLaFailedTranslationOutcomeCache.add(lw);
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
		
		Dictionnary.getAllKnownWords()
		.parallelStream().sorted((x,y)->x.toString().compareTo(y.toString())).forEach(lw->{
			i.incrementAndGet();
			Set<ResultOfTranslationAttempt> res =
					getBabLaTranslationDescriptions(lw);
			if(res.isEmpty())
			{
				System.err.print(i+":\t"+lw);
				getBabLaTranslationDescriptions(lw);
			}
			else System.out.print(i+":\t\t"+lw);
			System.out.println();
		});
		
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
