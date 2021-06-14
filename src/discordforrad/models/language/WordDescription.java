package discordforrad.models.language;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import discordforrad.LanguageCode;
import discordforrad.Translator;
import discordforrad.inputUtils.WebScrapping;
import net.dv8tion.jda.api.entities.TextChannel;

public class WordDescription {

	private List<String> translations;
	private Set<StringPair> contextSentences;
	private final LanguageWord lw;

	public WordDescription(LanguageWord lw, List<String> translations2, Set<StringPair> contextSentences2) {
		this.lw = lw;
		this.translations = translations2;
		this.contextSentences = contextSentences2;
	}

	public static WordDescription getDescriptionFor(LanguageWord lw) {
		String babLaInput = WebScrapping.getContentsFromBabLa(lw);
		String wrInput = WebScrapping.getContentsFromReferenceWord(lw);

		//	System.out.println(babLaInput);

		List<String> translations = new ArrayList<>();

		translations.add(Translator.getTranslation(lw).iterator().next().toString()+" (google)");
		

		Set<StringPair> contextSentences = new HashSet<>();
		if(Dictionnary.isInBabLaDisctionnary(lw))
		{
			//<a class="sound-inline bab-quick-sound"

			if(babLaInput.contains("<h2 class=\"h1\">\""+lw.getWord()+"\" in "))
			{
				translations.addAll(getBabLaDefinitionsFrom(lw,babLaInput));
			}
			
			
			
			
			/******** CONTEXT SENTENCES ************/
			
			contextSentences.addAll(getBabLaContextSentencesFrom(lw, babLaInput));

		}

		return new WordDescription(lw,translations, contextSentences);

	}

	private static List<String> getBabLaDefinitionsFrom(LanguageWord lw, String babLaInput) {
		String inputForDefinitions =  babLaInput
				.substring(
						babLaInput.indexOf("<h2 class=\"h1\">\""
				+lw.getWord()+"\" in "));
		
		if(inputForDefinitions.contains("<h2 class=\"section-label\">Context sentences</h2>"))
			inputForDefinitions = inputForDefinitions.substring(0,
					inputForDefinitions.indexOf("<h2 class=\"section-label\">Context sentences</h2>"));
		
		if(inputForDefinitions.contains("class=\"babQuickResult\">"+lw.getWord()+"<"))
		{
			int length = ("class=\"babQuickResult\">").length();
			inputForDefinitions = inputForDefinitions.substring(
					inputForDefinitions.indexOf(
					"class=\"babQuickResult\">"+lw.getWord()+"<")+length);
			
			try {
				if(inputForDefinitions.contains("class=\"babQuickResult\""))
					inputForDefinitions = inputForDefinitions.substring(
					0,
					inputForDefinitions.indexOf(
					"class=\"babQuickResult\""));
			}
			catch(Exception e) {
				throw new Error();
			}
			
			
		}
		
		if(inputForDefinitions.contains("Translations & Examples"))
			inputForDefinitions = inputForDefinitions.substring(0, inputForDefinitions.indexOf("Translations & Examples"));
		
		while(inputForDefinitions.contains("\n "))
			inputForDefinitions = inputForDefinitions.replaceAll("\n ","\n");
		List<String> allLines = Arrays.asList(inputForDefinitions.split("\n"));
		allLines = allLines.stream().filter(x->x.startsWith("<li>")).collect(Collectors.toList());
		allLines = allLines.stream().filter(x->x.contains("title=")).map(x->x.substring(x.indexOf("title="))).collect(Collectors.toList());
		allLines = allLines.stream().map(x->x.substring(x.indexOf(">")+1)).collect(Collectors.toList());
		allLines = allLines.stream().map(x->x.substring(0,x.indexOf("<"))).collect(Collectors.toList());
		
		return
				allLines.stream()
				.map(x->x+" (bab.la)").collect(Collectors.toList());
	}

	private static Set<StringPair> getBabLaContextSentencesFrom(
			LanguageWord lw, String babLaInput) {
		Set<StringPair> res = new HashSet<>();
		String toSearch = "Context sentences for \""+lw.getWord()+"\" in";
		int indexContextSentences = 
				babLaInput.indexOf(toSearch);
		if(indexContextSentences==-1)return res;
		String startContext = babLaInput.substring(indexContextSentences);
		startContext = startContext.substring(
				startContext.indexOf("<div class=\"dict-source\"><span class=\"flag "));
		
		if(startContext.contains("<span class=\"material-icons\">chevron_right</span>"))
			startContext = startContext.substring(0, startContext.indexOf("<span class=\"material-icons\">chevron_right</span>"));
		if(startContext.contains("<div id=\"synonyms\" class=\"content\">"))
			startContext = startContext.substring(0, startContext.indexOf("<div id=\"synonyms\" class=\"content\">"));

		List<String> split = Arrays.asList(startContext.split("<div class=\"dict-example\">"));
		for(String s:split)
		{
			String usefulPayload = s.substring(s.indexOf("</span>")+7);
			String usefulPayloadLeft = usefulPayload.substring(0,usefulPayload.indexOf("</div>"));
			String startOfRight = usefulPayload.substring(usefulPayload.indexOf("</div>")+6);
			startOfRight = startOfRight.substring(startOfRight.indexOf("</div>")+6);
			String right = startOfRight.substring(0,startOfRight.indexOf("</div>"));
			while(right.endsWith(" "))right = right.substring(0,right.length()-1);
			res.add(StringPair.newInstance(usefulPayloadLeft, right));
			/*;
			String left = startContext.substring(0,startContext.indexOf("</div>"));
			String remainingAfterLeft =  startContext.substring(startContext.indexOf("</div>")+5);

			String startOfRight = remainingAfterLeft.substring(remainingAfterLeft.indexOf("</div>")+6);
			int indexEndRight = startOfRight.indexOf("</div>");
			String right = startOfRight.substring(0,indexEndRight);
			while(right.contains("  "))right = right.replaceAll("  ", " ");
			res.add(StringPair.newInstance(left,right));
			startContext = startOfRight;*/
			
		}
		
		return res;
	}

	public Set<StringPair> getContextSentences() {
		return contextSentences;
	}

	public List<String> getTranslations() {
		return translations;
	}

	public LanguageWord getWord() {
		return lw;
	}

}
