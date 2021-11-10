package discordforrad;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import cachingutils.PlainObjectFileBasedCache;
import discordforrad.inputUtils.DatabaseProcessingOutcome;
import discordforrad.inputUtils.TextInputUtils;
import discordforrad.inputUtils.WebScrapping;
import discordforrad.inputUtils.WebScrapping.DataBaseEnum;
import discordforrad.inputUtils.databases.BabLaProcessing;
import discordforrad.models.language.LanguageText;
import discordforrad.models.language.LanguageWord;
import discordforrad.models.language.SuccessfulTranslationDescription;
import discordforrad.models.language.WordDescription;
import discordforrad.models.language.TranslationDescription;
import discordforrad.models.language.TranslationDescription.Origin;
import discordforrad.models.language.WordDescription.WordType;
import discordforrad.models.language.wordnetwork.forms.SingleEntryWebScrapping;
import discordforrad.translation.TranslationOutcomeFailure;

public class Translator {

	private static final String ENGLISH_CODE = "en";
	private static final String SWEDISH_CODE = "sv";
	
	
	private static final File PATH_TO_GOOGLE_TRANSLATE_CACHE = new File(Main.ROOT_DATABASE+"caches/google_translate_word_translation_cache.obj");
	
	private static final PlainObjectFileBasedCache<Map<LanguageWord, LanguageText>> translatedWords=
			PlainObjectFileBasedCache.loadFromFile(PATH_TO_GOOGLE_TRANSLATE_CACHE, ()->{return new HashMap<LanguageWord, LanguageText>();});
	
	/* public static void main(String[] args) throws IOException {
        String text = "Hello world!";
        System.out.println("Translated text: " + translate(ENGLISH_CODE, SWEDISH_CODE, text));
    }*/

	private static TranslationDescription getOutcomeOfGoogleTranslation(LanguageText originalText, LanguageCode langTo) {
		assert(!originalText.getLanguageCode().equals(langTo));
		String text = originalText.getText();
		if(text.length()>1900) return TranslationOutcomeFailure.newInstance(Origin.GOOGLE);
		try {
			String encodedString = URLEncoder.encode(text, TextInputUtils.UTF8.displayName());
			String urlStr = "https://script.google.com/macros/s/AKfycbxUDfPgQoUEHg37I0WBHkV9GVkyJdDe1NpCkRh5rMqbhw52Em5G/exec" +
					"?q=" + encodedString +
					"&target=" + toGoogleUrlString(langTo) +
					"&source=" + toGoogleUrlString(originalText.getLanguageCode());
			URL url = new URL(urlStr);
			StringBuilder response = new StringBuilder();
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestProperty("User-Agent", "Mozilla/5.0");
			BufferedReader in = new BufferedReader(
					new InputStreamReader(con.getInputStream(), TextInputUtils.ISO_CHARSET));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			String res = response.toString();
			res = TextInputUtils.Utf8ToIso(res);
			if(res.startsWith("<!DOCTYPE html>"))
			{
				if(res.contains("Tjänsten har använts för många gånger under en dag: translate"))
				{
					translatedWords.doAndUpdate(x->{});
					return TranslationOutcomeFailure.newInstance(discordforrad.models.language.TranslationDescription.Origin.GOOGLE);
				}
				
				try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
				return getOutcomeOfGoogleTranslation(originalText, langTo);
			}
			if(res.contains("¿½"))
				throw new Error();
			return SuccessfulTranslationDescription.newInstance
					(LanguageText.newInstance(langTo,res.toLowerCase()), "", WordType.UNDEFINED, Origin.GOOGLE);

		} catch (IOException e) {
			e.printStackTrace();
			throw new Error();
		}

	}

	private static String toGoogleUrlString(LanguageCode lc) {
		return lc.toString().toLowerCase();
	}

	public static TranslationDescription getGoogleTranslation(LanguageText input, LanguageCode translateTo) {
		return getOutcomeOfGoogleTranslation(input, translateTo);
	}

	public static TranslationDescription getGoogleTranslation(LanguageWord lw, LanguageCode to) {
		if(translatedWords.get().containsKey(lw))
			return SuccessfulTranslationDescription
				.newInstance(translatedWords.get().get(lw), "",
						WordType.UNDEFINED, discordforrad.models.language.TranslationDescription.Origin.GOOGLE);
		TranslationDescription res = getOutcomeOfGoogleTranslation(LanguageText.newInstance(lw), to);

		if(res instanceof SuccessfulTranslationDescription)
		{
			translatedWords.get().put(lw, ((SuccessfulTranslationDescription)res).getTranslatedText());
			//if(translatedWords.get().size()%10==0)
			translatedWords.doAndUpdate(x->{});
		}
		return res;
	}

	public static Set<TranslationDescription> getAllTranslations(LanguageWord word, LanguageCode to) {
		if(word.getWord().equals("ansågs"))
			System.out.println();
		TranslationDescription googleTranslation = getGoogleTranslation(word, to);
		Set<TranslationDescription> babLaTranslations = BabLaProcessing.getBabLaTranslationDescriptions(word,to);
		Set<TranslationDescription> wordReferenceTranslation = getWordReferenceTranslation(word,to);
		
		Set<TranslationDescription> res = new HashSet<>();
		if(googleTranslation instanceof SuccessfulTranslationDescription)
			res.add(googleTranslation);
		res.addAll(babLaTranslations);
		res.addAll(wordReferenceTranslation);
		return res;
	}


	public static Set<TranslationDescription> getWordReferenceTranslation(LanguageWord word, LanguageCode to) {
		return getWordReferenceTranslationsFrom(word, to).stream()
				.collect(Collectors.toSet());
	}

	public static Set<TranslationDescription> getWordReferenceTranslationsFrom(LanguageWord lw,
			LanguageCode to) {
		DatabaseProcessingOutcome outcome = WebScrapping.getContentsFrom(lw, DataBaseEnum.WORD_REFERENCE);
		String wrInput = ((SingleEntryWebScrapping)outcome).get();


		boolean shouldContinue = true;
		Set<TranslationDescription>res = new HashSet<>();
		
		while(shouldContinue)
		{
			
			if(wrInput==null)
				throw new Error();

			if(!wrInput.contains("Huvudsakliga översättningar"))
				return res;

			int indexDef = wrInput.indexOf("Huvudsakliga översättningar");

			int startTable = wrInput.substring(0,indexDef)
					.lastIndexOf("<table");


			String startOfSearched = wrInput.substring(startTable);
			int endTable = startOfSearched.indexOf("</table>");
			String searchedString = startOfSearched.substring(0,endTable);

			String[] splittedTable = searchedString.split("<tr");


			/*	String headOfTable = searchedString.substring(searchedString.indexOf("<tr")+4);
		headOfTable = headOfTable.substring(0,headOfTable.indexOf(ch))*/
			String languageRow = splittedTable[2];
			LanguageCode languageCodeLeftTable = null;
			if(!languageRow.contains("Svenska")) throw new Error();
			if(!languageRow.contains("Engelska")) throw new Error();
			if(languageRow.indexOf("Svenska")<languageRow.indexOf("Engelska"))
				languageCodeLeftTable = LanguageCode.SV;
			else languageCodeLeftTable = LanguageCode.EN;
			LanguageCode languageCodeRightTable = LanguageCode.otherLanguage(languageCodeLeftTable);


			String lastLeftTranslation = null;
			String lastLeftComplementaryTranslation = "";
			String lastRightComplementaryTranslation = "";
			WordDescription.WordType currentTypeLeft = null;

			for(String line : 
				Arrays.asList(splittedTable).subList(3, splittedTable.length))
			{
				if(line.isBlank())continue;
				String[] subsplit = line.split("<td");
				for(String split:subsplit)
				{
					if(split.replaceAll(" ", "").startsWith("class=\"even\"")) continue;
					if(split.replaceAll(" ", "").startsWith("class=\"FrWrd\""))
					{
						lastLeftTranslation = split.substring(split.indexOf("<strong>")+8,
								split.indexOf("</strong>"))
								.replaceAll("</span>", "")
								.replaceAll("<br>", "")
								.replaceAll("<br/>", "")
								.replaceAll("<span title=\"somebody\">", "")
								.replaceAll("<span title=\"something\">", "")
								.replaceAll("<span title=\"something or somebody\">", "")
								.replaceAll("<span title=\"somebody or something\">", "")
								.replaceAll("!", "")
								.replaceAll("\n", "")
								.replaceAll("\r", "");
						lastLeftTranslation = lastLeftTranslation.trim();
						if(lastLeftTranslation.startsWith("-"))
							lastLeftTranslation = lastLeftTranslation.substring(1);
						while(lastLeftTranslation.contains("  "))
							lastLeftTranslation=lastLeftTranslation.replaceAll("  ", " ");

						currentTypeLeft = WordDescription.getWordTypeOf(split);
						/*while(lastLeftTranslation.contains("<span"))
					{
						int start = lastLeftTranslation.indexOf("<span");
						int end = lastLeftTranslation.indexOf(">");
						lastLeftTranslation = lastLeftTranslation.substring(0,start) +
								lastLeftTranslation.substring(end+1);
					}*/
					}
					if(split.startsWith(">"))
					{
						lastLeftComplementaryTranslation =
								split.replaceAll("</td>", "")
								.replaceAll("<span class=\"dsense\">","")
								.replaceAll("<i>", "")
								.replaceAll("</i>", "")
								.replaceAll("</span>", "")
								.replaceAll("<i class=\"Fr2\">", "")
								.replaceAll("\n", "")
								.replaceAll("<br/", "")
								.replaceAll("\r", "")
								.replaceAll("<span title=\"something\"", "")
								.replaceAll("<span title=\"something or somebody\">", "")
								.replaceAll("<span title=\"somebody or something\"", "")
								.replaceAll("<span title=\"somebody\"", "")
								.replaceAll(">", "").trim();
						while(lastLeftComplementaryTranslation.contains("  "))
							lastLeftComplementaryTranslation = lastLeftComplementaryTranslation.replaceAll("  ", "");


					}
					if(split.startsWith(" class=\"ToWrd\">"))
					{
						Set<String> rightTranslations = new HashSet<>();
						WordDescription.WordType currentTypeRight = WordDescription.WordType.UNDEFINED;

						String wordsOnRightSide = split.replaceAll(" class='ToWrd' >", "")
								.replaceAll(" class=\"ToWrd\">","")
								.replaceAll(" class='POS2'>", "")
								.replaceAll("</em>", "")
								.replaceAll("</td>", "")
								.replaceAll("</tr>", "")
								.replaceAll("<i>", "")
								.replaceAll("</i>", "")
								.replaceAll("</tr>", "")
								.replaceAll("&nbsp;", "")
								.replaceAll("<td>", "")
								;

						if(wordsOnRightSide.contains("<a")) 
							wordsOnRightSide = wordsOnRightSide.substring(0,wordsOnRightSide.indexOf("<a"));
						if(wordsOnRightSide.contains("<em")) 
							wordsOnRightSide = wordsOnRightSide.substring(0,wordsOnRightSide.indexOf("<em"));
						if(wordsOnRightSide.contains("</span>"))wordsOnRightSide = 
								wordsOnRightSide.substring(wordsOnRightSide.indexOf("</span>")+7);
						wordsOnRightSide = wordsOnRightSide.trim();
						rightTranslations.addAll(Arrays.asList(wordsOnRightSide.split(",")).stream()
								.map(x->x.replaceAll(",", "").trim()
										.replaceAll("</span>", "")
										.replaceAll("<span title=\"something\">", "")
										.replaceAll("<span title=\"somebody\">", "")
										)
								.filter(x->!x.equals("</span>"))
								.collect(Collectors.toSet()));

						currentTypeRight = WordDescription.getWordTypeOf(split);


						for(String rightTranslation:rightTranslations)
							if(lw.equals(LanguageWord.newInstance(lastLeftTranslation, languageCodeLeftTable)))
							{
								res.add(SuccessfulTranslationDescription.newInstance(
										LanguageText.newInstance(languageCodeRightTable, rightTranslation),
										lastLeftComplementaryTranslation+" "+lastRightComplementaryTranslation,
										currentTypeRight,
										SuccessfulTranslationDescription.Origin.WORD_REFERENCE));
							}

						for(String localTranslation:rightTranslations)
						{
							localTranslation = localTranslation.replaceAll("!", "").trim();
							while(localTranslation.startsWith("-")) localTranslation = localTranslation.substring(1);
							if(lw.equals(LanguageWord.newInstance(localTranslation, languageCodeRightTable)))
							{
								res.add(SuccessfulTranslationDescription.newInstance(LanguageText.newInstance(languageCodeLeftTable,lastLeftTranslation), 
										lastLeftComplementaryTranslation+lastRightComplementaryTranslation,
										currentTypeLeft, SuccessfulTranslationDescription.Origin.WORD_REFERENCE));
							}
						}
					}

				}





				/*	if(leftSideOfTheRow.contains("<td>"))
				{
					lastLeftComplementaryTranslation = leftSideOfTheRow.substring(leftSideOfTheRow.lastIndexOf("<td>")+4);
					if(lastLeftComplementaryTranslation.contains("</td>"))
						lastLeftComplementaryTranslation = lastLeftComplementaryTranslation.substring(0,lastLeftComplementaryTranslation.indexOf("</td>"));
					else if(lastLeftComplementaryTranslation.contains("&nbsp"))
						lastLeftComplementaryTranslation = lastLeftComplementaryTranslation.substring(0,lastLeftComplementaryTranslation.indexOf("&nbsp"));
					//if(lastLeftComplementaryTranslation.contains("&nbsp;<span class='dsense' >"))
					//{
					lastLeftComplementaryTranslation = lastLeftComplementaryTranslation.replaceAll("&nbsp;<span class='dsense' >", " ");
					lastLeftComplementaryTranslation = lastLeftComplementaryTranslation.replaceAll("<i>", "");
					lastLeftComplementaryTranslation = lastLeftComplementaryTranslation.replaceAll("</i>", "");
					if(lastLeftComplementaryTranslation.contains("</span>"))
						lastLeftComplementaryTranslation = lastLeftComplementaryTranslation.substring(0, lastLeftComplementaryTranslation.indexOf("</span>"));
					lastLeftComplementaryTranslation = lastLeftComplementaryTranslation.replaceAll("<i class='Fr2'>", "");
					while(lastLeftComplementaryTranslation.startsWith(" "))lastLeftComplementaryTranslation=lastLeftComplementaryTranslation.substring(1);
					//}
				}else lastLeftComplementaryTranslation="";
				currentTypeLeft = WordType.UNDEFINED;
				if(leftSideOfTheRow.contains("<em class='tooltip POS2'>"))
				{
					String type = leftSideOfTheRow.substring(leftSideOfTheRow.indexOf("<em class='tooltip POS2'>")+25,
							leftSideOfTheRow.indexOf("<span>"));
					currentTypeLeft = WordType.parse(type);
				}
			}*/

				/*	if(rightSideOfTheRow.replaceAll(" ", "").contains("<spanclass='dsense'>"))
			{
				lastRightComplementaryTranslation = 
						rightSideOfTheRow.substring(rightSideOfTheRow.indexOf("<i>")-1,
								rightSideOfTheRow.indexOf("</span>"))
						.replaceAll("<i>", "")

						.replaceAll("</i>", "");
			}else 
				lastRightComplementaryTranslation = "";

				 */
			}
			
			shouldContinue = false;

			if(wrInput.contains("Matchande uppslagsord från andra sidan av ordboken."))
			{
				shouldContinue = true;
				wrInput = wrInput.substring(wrInput.indexOf("Matchande uppslagsord från andra sidan av ordboken.")+1);
			}
		}
		return res;
	}

	public static String getNiceTranslationString(LanguageWord lw, boolean multiLine) {
		String translationText="";
		
		
		Set<TranslationDescription> translations = getAllTranslations(lw, LanguageCode.otherLanguage(lw.getCode()));
		
		Set<SuccessfulTranslationDescription> successfulTranslations = translations.stream().filter(x->x instanceof SuccessfulTranslationDescription)
				.map(x->(SuccessfulTranslationDescription)x).collect(Collectors.toSet());

		Map<LanguageText, Set<TranslationDescription.Origin>> countPerTranslation = new HashMap<>();
		for(SuccessfulTranslationDescription td: successfulTranslations)
		{
			if(! countPerTranslation.containsKey(td.getTranslatedText()))
				countPerTranslation.put(td.getTranslatedText(), new HashSet<>());
			countPerTranslation.get(td.getTranslatedText()).add(td.getOriginOfTranslation());
		}

		List<LanguageText> textSortedByNumberOfOccurrences = countPerTranslation.keySet().stream()
				.sorted((x,y)->-Integer.compare(countPerTranslation.get(x).size(), countPerTranslation.get(y).size()))
				.collect(Collectors.toList());

		for(LanguageText currentText:textSortedByNumberOfOccurrences)
		{
			Set<SuccessfulTranslationDescription> translationsOfTheCurrentText = 
					successfulTranslations.stream()
					.filter(x->x.getTranslatedText().equals(currentText))
					.collect(Collectors.toSet());

			Set<String> translationOfTheCurrentTextWithAdvancedDescription = 
					translationsOfTheCurrentText.stream().filter(x->!x.getAdvancedDescription().isBlank())
					.map(x->x.getAdvancedDescription())
					.collect(Collectors.toSet());

			Set<WordDescription.WordType> allTypes = translationsOfTheCurrentText.stream()
					.map(x->x.getWordType())
					.collect(Collectors.toSet());

			allTypes.remove(WordDescription.WordType.UNDEFINED);
			/*	WordType officialType = WordType.UNDEFINED;
			if(allTypes.size()>1) throw new Error();
			if(allTypes.size() == 1) officialType = allTypes.iterator().next();*/
			String typeString = allTypes.toString();
			translationText+=currentText.getText();
			if(multiLine)translationText+=" "+typeString.substring(1,typeString.length()-1);
			for(String advanced : translationOfTheCurrentTextWithAdvancedDescription)
				translationText=translationText + " "+advanced;
			if(multiLine)translationText+=" ("+countPerTranslation.get(currentText)+")";
			if(multiLine)translationText+="\n";
			else translationText+=" ";
			
			/*for(TranslationDescription currentDescription:translations))
			{
				translationText+=s+"\n";
			}*/
		}
		
		if(! multiLine) translationText = translationText.replaceAll("  ", " ");
		
		return translationText;
	}

	public static Set<LanguageWord> getTranslationsOf(LanguageWord lw) {
		Set<TranslationDescription> translations = 
				Translator.getAllTranslations(lw, LanguageCode.otherLanguage(lw.getCode()));
		Set<LanguageWord> alternatives = 
				translations.stream()
				.map(x->((SuccessfulTranslationDescription)x))
				.map(x->x.getTranslatedText())
				.filter(x->x.getListOfValidWords().size()==1)
				.map(x->x.getListOfValidWords().get(0)).collect(Collectors.toSet());
		return alternatives;
	}

	public static Set<LanguageWord> getTranslationsOfTranslations(LanguageWord lw) {
		return getTranslationsOf(lw).stream().map(x->getTranslationsOf(x))
		.reduce(new HashSet<>(), (x,y)->{x.addAll(y);return x;});
	}

	public static boolean hasTranslationOrAGrundformRelatedTranslationThatIsNotFromGoogle(LanguageWord x) {
		Set<LanguageWord> allForms = new HashSet<>();
		allForms.add(x);
		allForms.addAll(WordDescription.getGrundforms(x));
		
		Set<TranslationDescription> translation = 
				allForms.stream().map(y->Translator.getAllTranslations(y, LanguageCode.otherLanguage(y.getCode())))
				.reduce(new HashSet<>(), (z,y)->{z.addAll(y); return z;});
		
		boolean res = translation.stream().filter(y->y instanceof SuccessfulTranslationDescription)
				.map(y->(SuccessfulTranslationDescription)y)
				.anyMatch(y->!y.getOrigin().equals(Origin.GOOGLE));
							
		return res;
	}


}