package discordforrad.inputUtils.databases;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import discordforrad.LanguageCode;
import discordforrad.inputUtils.DatabaseProcessingOutcome;
import discordforrad.inputUtils.WebScrapping;
import discordforrad.inputUtils.WebScrapping.DataBaseEnum;
import discordforrad.models.language.LanguageText;
import discordforrad.models.language.LanguageWord;
import discordforrad.models.language.SuccessfulTranslationDescription;
import discordforrad.models.language.TranslationDescription;
import discordforrad.models.language.TranslationDescription.Origin;
import discordforrad.models.language.WordDescription;
import discordforrad.models.language.WordDescription.WordType;
import discordforrad.models.language.wordnetwork.forms.SingleEntryWebScrapping;

public class BabLaProcessing {

	public static boolean isBabLaDictionnaryConsideredForNewWords() {
		return false;
	}

	public static boolean isWordTranslatedByBabLa(LanguageWord lw) {
		DatabaseProcessingOutcome outcome = WebScrapping.getContentsFrom(lw, DataBaseEnum.BAB_LA);

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

	public static Set<TranslationDescription> getBabLaTranslationDescriptions(LanguageWord lw, LanguageCode to)
	{
	/*	if(lw.getWord().equals("fetch"))
			System.out.print("");*/
		if(!isWordTranslatedByBabLa(lw))
			return new HashSet<>();
		DatabaseProcessingOutcome outcome = WebScrapping.getContentsFrom(lw, DataBaseEnum.BAB_LA);
		
		String babLaInput = ((SingleEntryWebScrapping)outcome).get();
		if(!babLaInput.toLowerCase().contains("<h2 class=\"h1\">\""+lw.getWord()+"\" in "))
			return new HashSet<>();
	
		List<String> definitions = WordDescription.getBabLaDefinitionsFrom(lw,babLaInput); 
		return definitions
				.stream().map(
						x->SuccessfulTranslationDescription.newInstance(
								LanguageText.newInstance(to,x),"",
								WordType.UNDEFINED, TranslationDescription.Origin.BAB_LA)).collect(Collectors.toSet());
	}

	private static double processingSpeedFactor = 1d;
	public static double getProcessingSpeedFactor() {
		return processingSpeedFactor;
	}

	public static boolean isValidlyProcessedRequest(String content, LanguageWord lw) {
		String header = lw.getWord().toUpperCase();
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
		String toSearchAbriged = "<title>"+header+" - Translation in ";
		if(content.length()<1000)
			return false;
		if(content.contains("<title>HTTPS:EN.BAB.LADICTIONARY"))
			return false;
		if(content.contains("<title>Discord</title>"))
			return false;
		if(content.contains(toSearch))
			return true;
		else if(content.contains("\""+lw.getWord()+"\" is currently not in our dictionary."))
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
		processingSpeedFactor*=2;
	}

	public synchronized static void decreaseProcessingTime() {
		processingSpeedFactor*=0.9;
		if(processingSpeedFactor<1) processingSpeedFactor = 1;
	}

	public static List<String> getBabLaDirectDefinitionsFrom(LanguageWord lw, String babLaInput) {
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
		
		List<String> allProposedTranslations = Arrays.asList(inputForDefinitions.split("class=\"babQuickResult\">"));
		
		List<String> allProposedTranslationsWithTheRightWord = allProposedTranslations
				.stream()
				.filter(x->x.toLowerCase().startsWith(lw.getWord().toLowerCase()+"<"))
				.collect(Collectors.toList());
		
		List<String> allLeftSideItemsBeforeFilteringOut = 
				allProposedTranslationsWithTheRightWord.stream().map(x->Arrays.asList(x.split("<li>")))
				.reduce(new ArrayList<>(), (x,y)->{x.addAll(y); return x;});
		
		List<String> allTranslationsOfTheRightWordWithNonTranslationsBeingFilteredOut = allLeftSideItemsBeforeFilteringOut
				.stream().filter(x->x.startsWith("<a href='https://en.bab.la/dictionary/")).collect(Collectors.toList());
		
		if(allTranslationsOfTheRightWordWithNonTranslationsBeingFilteredOut.size()==0)
			return new LinkedList<>();
		/*allTranslationsOfTheRightWord = allTranslationsOfTheRightWord.subList(1, allTranslationsOfTheRightWord.size());
		if(allTranslationsOfTheRightWord.size()==0)
			throw new Error();*/
		
		String firstItem = allTranslationsOfTheRightWordWithNonTranslationsBeingFilteredOut.get(0);
		allTranslationsOfTheRightWordWithNonTranslationsBeingFilteredOut = allTranslationsOfTheRightWordWithNonTranslationsBeingFilteredOut.stream()
				.map(x->x.substring(0, x.indexOf("</a>")))
				.map(x->x.substring(x.lastIndexOf(">")+1,x.length()))
				.collect(Collectors.toList());
		if(allTranslationsOfTheRightWordWithNonTranslationsBeingFilteredOut.contains("Translations & Examples"))
			allTranslationsOfTheRightWordWithNonTranslationsBeingFilteredOut=allTranslationsOfTheRightWordWithNonTranslationsBeingFilteredOut.subList(0, allTranslationsOfTheRightWordWithNonTranslationsBeingFilteredOut.indexOf("Translations & Examples"));
		return allTranslationsOfTheRightWordWithNonTranslationsBeingFilteredOut;
	}

}
