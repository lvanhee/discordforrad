package discordforrad.inputUtils.databases;

import java.util.HashSet;
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

}
