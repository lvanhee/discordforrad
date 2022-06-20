package discordforrad.translation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import cachingutils.advanced.failable.AttemptOutcome;
import cachingutils.advanced.failable.FailedDatabaseProcessingOutcome;
import cachingutils.advanced.failable.RequestSuccessfulButNoMatchingEntries;
import cachingutils.advanced.failable.SuccessfulOutcome;
import cachingutils.impl.TextFileBasedCache;
import discordforrad.Main;
import discordforrad.inputUtils.TextInputUtils;
import discordforrad.inputUtils.WebScrapping;
import discordforrad.inputUtils.WebScrapping.DataBaseEnum;
import discordforrad.inputUtils.databases.BabLaProcessing;
import discordforrad.models.language.LanguageCode;
import discordforrad.models.language.LanguageText;
import discordforrad.models.language.LanguageWord;
import discordforrad.models.language.WordDescription;
import discordforrad.models.language.WordDescription.WordType;
import discordforrad.models.language.wordnetwork.forms.SingleEntryWebScrapping;
import discordforrad.translation.ResultOfTranslationAttempt.Origin;

public class Translator {


	private static final File PATH_TO_GOOGLE_TRANSLATE_CACHE = new File(Main.ROOT_DATABASE+"databases/known_google_translate_word_translations.txt");
	private static final File PATH_TO_TRANSLATION_CACHE = new File(Main.ROOT_DATABASE+"caches/already_computed_translations.txt");
	private static final File PATH_TO_WR_TRANSLATION_CACHE = new File(Main.ROOT_DATABASE+"databases/word_reference_translations.txt");
	private static final File PATH_TO_SENTENCE_TRANSLATION_CACHE = new File(Main.ROOT_DATABASE+"databases/google_translated_sentences_database.txt");
	
	
	private static final TextFileBasedCache<LanguageWord, LanguageText> successfullyTranslatedGoogleWords=
			TextFileBasedCache.newInstance(
					PATH_TO_GOOGLE_TRANSLATE_CACHE,
					i->i.toString(),
					LanguageWord::parse,
					o->o.toString(),
					LanguageText::parse,"\t");



	private static final TextFileBasedCache<LanguageWord, List<ResultOfTranslationAttempt>> translationAttemptsCache=
			TextFileBasedCache.newInstance(
					PATH_TO_TRANSLATION_CACHE,
					i->i.toString(),
					LanguageWord::parse,
					o->{if(o.isEmpty()) return "[]"; else return o.stream().map(x->x.toParsableString()).reduce("",(x,y)->x+";"+y).substring(1);},
					ResultOfTranslationAttempt::parseList,"\t");
	
	
	private static final TextFileBasedCache<LanguageWord, List<ResultOfTranslationAttempt>> wordReferenceTranslationAttemptsCache=
			TextFileBasedCache.newInstance(
					PATH_TO_WR_TRANSLATION_CACHE,
					i->i.toString(),
					LanguageWord::parse,
					o->{if(o.isEmpty()) return "[]"; else return o.stream().map(x->x.toParsableString()).reduce("",(x,y)->x+";"+y).substring(1);},
					ResultOfTranslationAttempt::parseList,"\t");
	
	private static final TextFileBasedCache<LanguageText, LanguageText> textTranslationCache=
			TextFileBasedCache.newInstance(
					PATH_TO_SENTENCE_TRANSLATION_CACHE,
					i->i.toString(),
					LanguageText::parse,
					o->o.toString(),
					LanguageText::parse,"\t");


	/* public static void main(String[] args) throws IOException {
        String text = "Hello world!";
        System.out.println("Translated text: " + translate(ENGLISH_CODE, SWEDISH_CODE, text));
    }*/

	private static boolean cacheFailedTooManyAttemptsGoogle = false; 
	private static ResultOfTranslationAttempt getOutcomeOfGoogleTranslation(LanguageText originalText, LanguageCode langTo) {
		
		assert(!originalText.getLanguageCode().equals(langTo));
		if(cacheFailedTooManyAttemptsGoogle)
			return TranslationOutcomeFailure.newInstance(discordforrad.translation.ResultOfTranslationAttempt.Origin.GOOGLE);
		String text = originalText.getText();
		if(text.length()>1900) return TranslationOutcomeFailure.newInstance(Origin.GOOGLE);
		try {
			String encodedString = URLEncoder.encode(text, TextInputUtils.UTF8.displayName());

			String urlStr = "https://script.google.com/macros/s/AKfycbxFY_r7smMbqD5o9_zLa0dj9jZtWaG0Lazqi1Fd34KYTLvrF_E/exec" +
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
					System.err.println("GOOGLE TRANSLATION HAS BEEN OVERUSED TODAY");
					
					cacheFailedTooManyAttemptsGoogle = true;

					new Thread(()->
					{
						try {
							Thread.sleep(60000);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
						cacheFailedTooManyAttemptsGoogle = false;
					}).start();

					if(Main.SHUTDOWN_WHEN_RUNNING_OUT_OF_GOOGLE_TRANSLATE)
					{
						System.err.println("ABOUT TO KILL THE WHOLE SYSTEM AS GOOGLE TRANSLATE IS NOT WORKING");
					    Runtime runtime = Runtime.getRuntime();
					    try {
					    	Thread.sleep(60000);
							Process proc = runtime.exec("shutdown -s -t 0");
						} catch (IOException e) {
							e.printStackTrace();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					   // System.exit(0);
					}
					return TranslationOutcomeFailure.newInstance(discordforrad.translation.ResultOfTranslationAttempt.Origin.GOOGLE);
				}

				try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
				return getOutcomeOfGoogleTranslation(originalText, langTo);
			}
			if(res.contains("¿½"))
				throw new Error();

			System.out.println("Translator: Translating via google: "+originalText+" -> "+res.toLowerCase());

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

	
	public static ResultOfTranslationAttempt getGoogleTranslation(LanguageText input, LanguageCode translateTo, boolean cached) {
		if(input.isSingleWord())
			return getGoogleTranslation(input.toSingleWord());
		
		if(textTranslationCache.has(input))
			return SuccessfulTranslationDescription.newInstance(textTranslationCache.get(input), "", WordType.UNDEFINED, Origin.GOOGLE);
		
		ResultOfTranslationAttempt res =  getOutcomeOfGoogleTranslation(input, translateTo); 
		
		if(res instanceof SuccessfulTranslationDescription)	
			textTranslationCache.add(input, ((SuccessfulTranslationDescription)res).getTranslatedText());
		return res;
	}

	public static ResultOfTranslationAttempt getGoogleTranslation(LanguageWord lw) {
		synchronized(lw)
		{
			if(successfullyTranslatedGoogleWords.has(lw))
				return SuccessfulTranslationDescription
						.newInstance(successfullyTranslatedGoogleWords.get(lw), "",
								WordType.UNDEFINED, discordforrad.translation.ResultOfTranslationAttempt.Origin.GOOGLE);
			ResultOfTranslationAttempt res = getOutcomeOfGoogleTranslation(LanguageText.newInstance(lw), LanguageCode.otherLanguage(lw.getCode()));

			if(res instanceof SuccessfulTranslationDescription)
			{
				successfullyTranslatedGoogleWords.add(lw, ((SuccessfulTranslationDescription)res).getTranslatedText());
			}
			
			return res;
		}
	}


	public static Set<ResultOfTranslationAttempt> getResultOfTranslationAttempts(LanguageWord word) {
		synchronized (word) {
			Set<ResultOfTranslationAttempt> res = new HashSet<>();
			if(translationAttemptsCache.has(word))
			{
				res.addAll(translationAttemptsCache.get(word));
			//	res.addAll(BabLaProcessing.getBabLaTranslationDescriptions(word));
			//	res.add(getGoogleTranslation(word));
				
				if(!res.stream().anyMatch(x->x instanceof TranslationOutcomeFailure))
					return res;
				res = res.stream().filter(x->!(x instanceof TranslationOutcomeFailure)).collect(Collectors.toSet());
			}

			ResultOfTranslationAttempt googleTranslation = getGoogleTranslation(word);
			Set<ResultOfTranslationAttempt> babLaTranslations = BabLaProcessing.getBabLaTranslationDescriptions(word);
			Set<ResultOfTranslationAttempt> wordReferenceTranslation = getWordReferenceTranslation(word);

			
			res.add(googleTranslation);
			res.addAll(babLaTranslations);
			res.addAll(wordReferenceTranslation);

			
			translationAttemptsCache.addOrReplace(word,res.stream().collect(Collectors.toList()));
			return res;
		}
	}


	public static Set<ResultOfTranslationAttempt> getWordReferenceTranslation(LanguageWord word) {
		return getWordReferenceTranslationsFrom(word, LanguageCode.otherLanguage(word.getCode())).stream()
				.collect(Collectors.toSet());
	}

	public static Set<ResultOfTranslationAttempt> getWordReferenceTranslationsFrom(LanguageWord lw,
			LanguageCode to) {
		
		if(wordReferenceTranslationAttemptsCache.has(lw))
		{
			Set<ResultOfTranslationAttempt>res = wordReferenceTranslationAttemptsCache.get(lw)
					.stream()
					.collect(Collectors.toSet());
			if(res.stream().anyMatch(x->(x instanceof TranslationOutcomeFailure)))
				throw new Error();
			return res;
		}
		
		AttemptOutcome outcome = WebScrapping.getContentsFrom(lw, DataBaseEnum.WORD_REFERENCE);
		if(outcome instanceof FailedDatabaseProcessingOutcome)
		{
			Set<ResultOfTranslationAttempt> res =  Arrays.asList(TranslationOutcomeFailure.newInstance(Origin.WORD_REFERENCE))
					.stream().collect(Collectors.toSet());
		//	wordReferenceTranslationAttemptsCache.add(lw, res.stream().collect(Collectors.toList()));
			
			return res;
		}
		
		if(outcome instanceof SuccessfulOutcome && ((SuccessfulOutcome<Set>)outcome).getResult().isEmpty())
		{
			Set<ResultOfTranslationAttempt> res = Arrays.asList(NoTranslationForTheRequest.newInstance(Origin.WORD_REFERENCE))
					.stream().collect(Collectors.toSet());
			wordReferenceTranslationAttemptsCache.add(lw, res.stream().collect(Collectors.toList()));
			return res;
		}
		
		Set<String> possibleResults = ((SuccessfulOutcome<Set<String>>)outcome).getResult();
		if(possibleResults.size()!=1)
			throw new Error();
		String wrInput = possibleResults.iterator().next();

		boolean shouldContinue = true;
		Set<ResultOfTranslationAttempt>res = new HashSet<>();

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

						if(lastLeftTranslation.contains(", also "))
							lastLeftTranslation = lastLeftTranslation.substring(0,lastLeftTranslation.indexOf(", also "));


						if(lastLeftTranslation.contains("<a")&&!lastLeftTranslation.startsWith("<a"))
						{
							lastLeftTranslation = lastLeftTranslation
									.substring(0, lastLeftTranslation.indexOf("<a")).trim();
						}

						if(lastLeftTranslation.contains("</a>"))
						{
							lastLeftTranslation = 
									lastLeftTranslation.substring(lastLeftTranslation.indexOf("</a>")+4).trim();

							if(lastLeftTranslation.contains("<a")&&!lastLeftTranslation.startsWith("<a"))
							{
								lastLeftTranslation = lastLeftTranslation
										.substring(0, lastLeftTranslation.indexOf("<a")).trim();
							}
						}
						if(lastLeftTranslation.contains(", plural"))
							lastLeftTranslation = lastLeftTranslation.substring(0,lastLeftTranslation.indexOf(", plural"))
							.trim();
						if(lastLeftTranslation.contains(", pl:"))
							lastLeftTranslation = lastLeftTranslation.substring(0,lastLeftTranslation.indexOf(", pl:"))
							.trim();

						if(lastLeftTranslation.contains(", f:"))
							lastLeftTranslation = lastLeftTranslation.substring(0,lastLeftTranslation.indexOf(", f:"))
							.trim();

						if(lastLeftTranslation.contains(", replacce:"))
							lastLeftTranslation = lastLeftTranslation.substring(0,lastLeftTranslation.indexOf(", replacce:"))
							.trim();

						if(lastLeftTranslation.contains(", UK:"))
							lastLeftTranslation = lastLeftTranslation.substring(0,lastLeftTranslation.indexOf(", UK:"))
							.trim();
						if(lastLeftTranslation.contains("also UK:"))
							lastLeftTranslation = lastLeftTranslation.substring(0,lastLeftTranslation.indexOf("also UK:"))
							.trim();
						if(lastLeftTranslation.contains(", US:"))
							lastLeftTranslation = lastLeftTranslation.substring(0,lastLeftTranslation.indexOf(", US:"))
							.trim();

						if(lastLeftTranslation.toLowerCase().startsWith("re:"))
							continue;

						if(lastLeftTranslation.toLowerCase().contains(", rare:"))
							lastLeftTranslation = lastLeftTranslation.substring(0,lastLeftTranslation.indexOf(", rare:"))
							.trim();
						if(lastLeftTranslation.toLowerCase().contains(", singular:"))
							lastLeftTranslation = lastLeftTranslation.substring(0,lastLeftTranslation.indexOf(", singular:"))
							.trim();

						if(lastLeftTranslation.contains(":"))
							throw new Error();

						lastLeftTranslation = lastLeftTranslation.trim();

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
						{
							if(lastLeftTranslation.toLowerCase().startsWith("re:"))continue;
							if(lastLeftTranslation.isBlank())continue;
							if(lw.equals(LanguageWord.newInstance(lastLeftTranslation.replaceAll("-", ""), languageCodeLeftTable)))
							{
								rightTranslation = rightTranslation.replaceAll("\n", "").replaceAll("\t", "").replaceAll("\r", "");
								while(rightTranslation.contains("  ")){rightTranslation = rightTranslation.replaceAll("  ", " ");}
								if(rightTranslation.equals("-"))continue;
								if(rightTranslation.equals("--"))continue;
								if(rightTranslation.isBlank())continue;
								if(lastLeftTranslation.isBlank()) continue;
								res.add(SuccessfulTranslationDescription.newInstance(
										LanguageText.newInstance(languageCodeRightTable, rightTranslation),
										lastLeftComplementaryTranslation+" "+lastRightComplementaryTranslation,
										currentTypeRight,
										SuccessfulTranslationDescription.Origin.WORD_REFERENCE));
							}
						}

						for(String localTranslation:rightTranslations)
						{
							if(localTranslation.contains(":"))continue;

							localTranslation = localTranslation.replaceAll("!", "").trim();
							while(localTranslation.startsWith("-")) localTranslation = localTranslation.substring(1);
							localTranslation = localTranslation.trim();
							if(localTranslation.isEmpty())continue;
							if(lw.equals(LanguageWord.newInstance(localTranslation, languageCodeRightTable)))
							{
								if(lastLeftTranslation.isBlank())continue;
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
		
		wordReferenceTranslationAttemptsCache.add(lw, res.stream().collect(Collectors.toList()));
		return res;
	}

	public static String getNiceTranslationString(LanguageWord lw, boolean multiLine) {
		String translationText="";

		Set<ResultOfTranslationAttempt> translations = getResultOfTranslationAttempts(lw);

		Set<SuccessfulTranslationDescription> successfulTranslations = translations.stream().filter(x->x instanceof SuccessfulTranslationDescription)
				.map(x->(SuccessfulTranslationDescription)x).collect(Collectors.toSet());

		Map<LanguageText, Set<ResultOfTranslationAttempt.Origin>> countPerTranslation = 
				getOriginsPerTranslation(lw);
		
		
		
		List<LanguageText> textSortedByNumberOfOccurrences = 
				getTranslationSortedByNumberOfOrigins(lw);


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
			for(String advanced : translationOfTheCurrentTextWithAdvancedDescription)
				translationText=translationText + " "+advanced+" ";

			//if(currentText.isSingleWord())
			//	{
			
			List<LanguageText> words = getTranslationSortedByNumberOfOrigins(currentText,true);
			/*LanguageWord mostSuitedTranslation = 
			ResultOfTranslationAttempt translate=Translator.getGoogleTranslation(
					currentText, LanguageCode.otherLanguage(currentText.getLanguageCode()),true);*/ 
			
			String googleText = "";
			if(words.isEmpty())
			{
				googleText = "NO TRANSLATION FOR THIS WORD";
				getTranslationSortedByNumberOfOrigins(currentText,true);
			}
			else 
				googleText = words.get(0).toString();
			translationText+=" --"+googleText;
			//}
			//else {translationText}

			if(multiLine)translationText+=" "+typeString.substring(1,typeString.length()-1);

			if(multiLine)translationText+=" "+countPerTranslation.get(currentText)+"";
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

	private static List<LanguageText> getTranslationSortedByNumberOfOrigins(LanguageText lt,
			boolean cached) 
	{
		if(lt.isSingleWord())
		{
			return getTranslationSortedByNumberOfOrigins(lt.toSingleWord());
		}
		else
		{
			ResultOfTranslationAttempt to = getGoogleTranslation(
					lt,
					LanguageCode.otherLanguage(lt.getLanguageCode()),
					cached);
			
			List<LanguageText> res = new ArrayList<>();
			if(to instanceof SuccessfulTranslationDescription)
				res.add(((SuccessfulTranslationDescription)to).getTranslatedText());
			return res;
		}
	}

	private static List<LanguageText> getTranslationSortedByNumberOfOrigins(LanguageWord lw) {
		
		Map<LanguageText, Set<ResultOfTranslationAttempt.Origin>> countPerTranslation = 
				getOriginsPerTranslation(lw);

		return countPerTranslation.keySet().stream()
				.sorted((x,y)->-Integer.compare(countPerTranslation.get(x).size(), countPerTranslation.get(y).size()))
				.collect(Collectors.toList());
	}

	private static Map<LanguageText, Set<Origin>> getOriginsPerTranslation(LanguageWord lw) {
		Set<ResultOfTranslationAttempt> translations = getResultOfTranslationAttempts(lw);

		Set<SuccessfulTranslationDescription> successfulTranslations = translations.stream().filter(x->x instanceof SuccessfulTranslationDescription)
				.map(x->(SuccessfulTranslationDescription)x).collect(Collectors.toSet());

		Map<LanguageText, Set<ResultOfTranslationAttempt.Origin>> countPerTranslation = 				new HashMap<>();
		for(SuccessfulTranslationDescription td: successfulTranslations)
		{
			if(! countPerTranslation.containsKey(td.getTranslatedText()))
				countPerTranslation.put(td.getTranslatedText(), new HashSet<>());
			countPerTranslation.get(td.getTranslatedText()).add(td.getOriginOfTranslation());
		}
		
		return countPerTranslation;
	}

	private static final Map<LanguageWord, Set<LanguageWord>> translationPerWord = new ConcurrentHashMap<>();
	public static Set<LanguageWord> getTranslationsOf(LanguageWord lw) {
		if(translationPerWord.containsKey(lw))
			return translationPerWord.get(lw);
		Set<ResultOfTranslationAttempt> translations = 
				Translator.getResultOfTranslationAttempts(lw);
		Set<LanguageWord> alternatives = 
				translations.stream()
				.filter(x->x instanceof SuccessfulTranslationDescription)
				.map(x->((SuccessfulTranslationDescription)x))
				.map(x->x.getTranslatedText())
				.filter(x->x.getListOfValidWords().size()==1)
				.map(x->x.getListOfValidWords().get(0)).collect(Collectors.toSet());


		translationPerWord.put(lw, alternatives);
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

		Set<ResultOfTranslationAttempt> translation = 
				allForms.stream().map(y->Translator.getResultOfTranslationAttempts(y))
				.reduce(new HashSet<>(), (z,y)->{z.addAll(y); return z;});

		boolean res = translation.stream().filter(y->y instanceof SuccessfulTranslationDescription)
				.map(y->(SuccessfulTranslationDescription)y)
				.anyMatch(y->!y.getOrigin().equals(Origin.GOOGLE));

		return res;
	}

	public static boolean hasRunOutOfGoogleTranslateForTheDay() {
		return cacheFailedTooManyAttemptsGoogle;
	}


}