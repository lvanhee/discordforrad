package discordforrad.models.language;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import discordforrad.LanguageCode;
import discordforrad.Translator;
import discordforrad.inputUtils.WebScrapping;
import discordforrad.inputUtils.WebScrapping.DataBaseEnum;
import discordforrad.models.language.TranslationDescription.Origin;
import discordforrad.models.language.WordDescription.WordType;
import net.dv8tion.jda.api.entities.TextChannel;

public class WordDescription {
	public static enum WordType{UNDEFINED, ADV, VTR, N, S,UTTR, PREP, ADJ, V_EXPR,VI,VITR, VITR_PART, RAKN, PRON,
		N_PL,EXPR, INTERJ, CONJ, VBAL_UTTR, VTR_PHRASAL_SEP, VTR_PHRASAL_INSEP, VTR_PARTIKEL_OSKJ, V_PRES,
		PREFIX, EGEN_NAMN, SPL, N_AS_ADJ, V_AUX, HJV, V, VTR_PARTIKEL_SKJ,
		ADJECTIVE_ENDING,
		VI_PHRASAL;

		public static WordType parse(String type) {
			if(type.equals("adv")||type.startsWith("adverb"))return ADV;
			if(type.equals("vtr"))return VTR;
			if(type.equals("n")||type.equals("substantiv")||type.equals("oböjligt substantiv"))return N;
			if(type.equals("s"))return S;
			if(type.equals("uttr"))return UTTR;
			if(type.equals("prep")||type.equals("preposition")
					||type.equals("preposition och adverb"))return PREP;
			if(type.equals("adj")||type.equals("adjektiv")
					||type.equals("oböjligt adjektiv"))return ADJ;
			if(type.equals("adjektiviskt slutled")) return N;
			if(type.equals("v expr"))return V_EXPR;
			if(type.equals("vi"))return VI;
			if(type.equals("vitr"))return VITR;
			if(type.equals("vitr partikel"))return VITR_PART;
			if(type.equals("räkn")||type.equals("räkneord"))return RAKN;
			if(type.equals("pron")||type.equals("pronomen"))return PRON;
			if(type.equals("npl"))return N_PL;
			if(type.equals("expr"))return EXPR;
			if(type.equals("interj"))return INTERJ;
			if(type.equals("konj") || type.equals("subjunktion"))return CONJ;
			if(type.equals("conj"))return CONJ;
			if(type.equals("vbal uttr"))return VBAL_UTTR;
			if(type.equals("vtr phrasal sep"))return VTR_PHRASAL_SEP;
			if(type.equals("vtr phrasal insep"))return VTR_PHRASAL_INSEP;
			if(type.equals("vtr partikel oskj"))return VTR_PARTIKEL_OSKJ;
			if(type.equals("vtr partikel skj"))return VTR_PARTIKEL_SKJ;
			if(type.equals("v pres"))return V_PRES;
			if(type.equals("prefix"))return PREFIX;
			if(type.equals("egen"))return EGEN_NAMN;
			if(type.equals("spl"))return SPL;
			if(type.equals("n as adj"))return N_AS_ADJ;
			if(type.equals("v aux"))return V_AUX;
			if(type.equals("hjv"))return HJV;
			if(type.equals("verb"))return V;
			if(type.equals("vi phrasal"))return VI_PHRASAL;
			if(type.equals("interjektion"))return INTERJ;
			throw new Error(type+" undefined");
		}

		public static boolean hasAlternativeForms(WordType wt) {
			switch(wt)
			{
			case ADV:return false;
			case ADJ:return true;
			default: throw new Error();
			}
		}

		public static WordType parseRaw(String s, DataBaseEnum saol) {
			if(saol.equals(DataBaseEnum.SAOL))
			{
				final String wordClassIndicator = "<a class=\"ordklass\">";
				String wordClassStart = s.substring(s.indexOf(wordClassIndicator)+wordClassIndicator.length());
				String wordClass = wordClassStart.substring(0, wordClassStart.indexOf("</a>")).trim();
				return parse(wordClass);
			}
			if(saol.equals(DataBaseEnum.SO))
			{
				final String wordClassIndicator = "<div class=\"ordklass\">";
				String wordClassStart = s.substring(s.indexOf(wordClassIndicator)+wordClassIndicator.length());
				String wordClass = wordClassStart.substring(0, wordClassStart.indexOf("</div>")).trim();
				return parse(wordClass);
			}
			throw new Error();
		}};

		private List<TranslationDescription> translations;
		private Set<StringPair> contextSentences;
		private final LanguageWord lw;
		private Map<WordType, RelatedForms> alternativeForms;

		public WordDescription(LanguageWord lw, List<TranslationDescription> translations2, 
				Set<StringPair> contextSentences2, 
				Map<WordType, RelatedForms> af
				) {
			this.lw = lw;
			this.translations = translations2;
			this.contextSentences = contextSentences2;
			this.alternativeForms = af;
		}

		public static WordDescription getDescriptionFor(LanguageWord lw) {
			if(!Dictionnary.isInDictionnaries(lw))
				throw new Error(lw+" not in dictionnaries");

			Map<WordType, RelatedForms>af = new HashMap<>();
			String babLaInput = WebScrapping.getContentsFrom(lw, DataBaseEnum.BAB_LA);
			LanguageCode translateTo = LanguageCode.otherLanguage(lw.getCode());

			//	System.out.println(babLaInput);

			List<TranslationDescription> translations = new ArrayList<>();

			translations.add(
					TranslationDescription.newInstance(
							LanguageText.newInstance(translateTo,Translator.getTranslation(lw).iterator().next().toString()),
							"",
							WordDescription.WordType.UNDEFINED,
							Origin.GOOGLE));


			Set<StringPair> contextSentences = new HashSet<>();
			if(Dictionnary.isInBabLaDisctionnary(lw))
			{
				//<a class="sound-inline bab-quick-sound"

				if(babLaInput.contains("<h2 class=\"h1\">\""+lw.getWord()+"\" in "))
				{
					translations.addAll(getBabLaDefinitionsFrom(lw,babLaInput)
							.stream().map(
									x->TranslationDescription.newInstance(
											LanguageText.newInstance(translateTo,x),"",
											WordType.UNDEFINED, Origin.BAB_LA)).collect(Collectors.toList()));
				}

				/******** CONTEXT SENTENCES ************/

				contextSentences.addAll(getBabLaContextSentencesFrom(lw, babLaInput));
			}

			if(Dictionnary.isInDictionnaryOf(lw, DataBaseEnum.SO))
			{
				List<String> allFormsFromSO = 
						Arrays.asList(WebScrapping.getContentsFrom(lw, DataBaseEnum.SO).split("<div class=\"superlemma\""))
						.stream().collect(Collectors.toList());
				allFormsFromSO.remove(0);

				for(String wholeForm:allFormsFromSO)
				{
					final String wordClassIndicator = "<div class=\"ordklass\">";
					String wordClassStart = wholeForm.substring(wholeForm.indexOf(wordClassIndicator)+wordClassIndicator.length());
					String wordClass = wordClassStart.substring(0, wordClassStart.indexOf("</div>")).trim();
					WordType wt = WordType.parse(wordClass);

					RelatedForms rf = RelatedForms.parseFrom(wholeForm,DataBaseEnum.SO);

					if(af.containsKey(wt)&&!af.get(wt).equals(rf)) 
						af.put(wt, af.get(wt).blendWith(rf));
					else af.put(wt,rf);

				}
			}
			
			if(Dictionnary.isInDictionnaryOf(lw, DataBaseEnum.SAOL))
			{
				List<String> allForms = 
						Arrays.asList(WebScrapping.getContentsFrom(lw, DataBaseEnum.SAOL).split("<a class=\"ordklass\">"))
						.stream().collect(Collectors.toList());
				allForms.remove(0);

				for(String wholeForm:allForms)
				{
					final String grund = wholeForm.substring(0,wholeForm.indexOf("</span>")).trim();
					WordType wt = WordType.parseRaw(wholeForm, DataBaseEnum.SAOL);

					RelatedForms rf = RelatedForms.parseFrom(wholeForm,DataBaseEnum.SAOL);
					if(!rf.containsForm(lw))continue;

					if(af.containsKey(wt)&&!af.get(wt).equals(rf)) 
						af.put(wt, af.get(wt).blendWith(rf));
					else af.put(wt,rf);

				}
			}

			translations.addAll(getWordReferenceDefinitionsFrom(lw,WebScrapping.getContentsFrom(lw, DataBaseEnum.WORD_REFERENCE)));
			//}


			return new WordDescription(lw,translations, contextSentences, af);

		}

		private static List<TranslationDescription> getWordReferenceDefinitionsFrom(LanguageWord lw,
				String wrInput) {

			List<TranslationDescription>res = new ArrayList<>();
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
			WordType currentTypeLeft = null;

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
								.replaceAll("!", "")
								.replaceAll("\n", "")
								.replaceAll("\r", "");
						lastLeftTranslation = lastLeftTranslation.trim();
						while(lastLeftTranslation.contains("  "))
							lastLeftTranslation=lastLeftTranslation.replaceAll("  ", " ");

						currentTypeLeft = getWordTypeOf(split);
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
								.replaceAll(">", "").trim();
						while(lastLeftComplementaryTranslation.contains("  "))
							lastLeftComplementaryTranslation = lastLeftComplementaryTranslation.replaceAll("  ", "");


					}
					if(split.startsWith(" class=\"ToWrd\">"))
					{
						Set<String> rightTranslations = new HashSet<>();
						WordType currentTypeRight = WordType.UNDEFINED;

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
								.map(x->x.replaceAll(",", "").trim()).collect(Collectors.toSet()));

						currentTypeRight = getWordTypeOf(split);


						for(String rightTranslation:rightTranslations)
							if(lw.equals(LanguageWord.newInstance(lastLeftTranslation, languageCodeLeftTable)))
							{
								res.add(TranslationDescription.newInstance(
										LanguageText.newInstance(languageCodeRightTable, rightTranslation),
										lastLeftComplementaryTranslation+" "+lastRightComplementaryTranslation,
										currentTypeRight,
										TranslationDescription.Origin.WORD_REFERENCE));
							}

						for(String localTranslation:rightTranslations)
						{
							localTranslation = localTranslation.trim();
							if(lw.equals(LanguageWord.newInstance(localTranslation, languageCodeRightTable)))
							{
								res.add(TranslationDescription.newInstance(LanguageText.newInstance(languageCodeLeftTable,lastLeftTranslation), 
										lastLeftComplementaryTranslation+lastRightComplementaryTranslation,
										currentTypeLeft, TranslationDescription.Origin.WORD_REFERENCE));
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

			if(wrInput.contains("Matchande uppslagsord från andra sidan av ordboken."))
			{
				List<TranslationDescription> otherTranslation = getWordReferenceDefinitionsFrom(lw, 
						wrInput.substring(wrInput.indexOf("Matchande uppslagsord från andra sidan av ordboken.")+1));
				res.addAll(otherTranslation);
			}
			return res;
		}

		private static WordType getWordTypeOf(String split) {
			if(split.contains("<em class=\"tooltip POS2\">"))
			{
				String type = split.substring(split.indexOf("<em class=\"tooltip POS2\">")+25,
						split.indexOf("<span>")).trim();
				return WordType.parse(type);
			}
			return WordType.UNDEFINED;
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
					allLines.stream().collect(Collectors.toList());
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

		public List<TranslationDescription> getTranslations() {
			return translations;
		}

		public LanguageWord getWord() {
			return lw;
		}

		public Set<WordType> getWordTypes() {
			return alternativeForms.keySet();
		}

		public RelatedForms getAlternativesFor(WordType wt) {
			return alternativeForms.get(wt);
		}

}
