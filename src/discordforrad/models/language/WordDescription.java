package discordforrad.models.language;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import cachingutils.PlainObjectFileBasedCache;
import discordforrad.LanguageCode;
import discordforrad.Main;
import discordforrad.Translator;
import discordforrad.inputUtils.WebScrapping;
import discordforrad.inputUtils.WebScrapping.DataBaseEnum;
import discordforrad.inputUtils.databases.BabLaProcessing;
import discordforrad.models.language.WordDescription.WordType;
import discordforrad.models.language.wordnetwork.forms.RelatedForms;
import discordforrad.models.language.wordnetwork.forms.RelatedFormsNetwork;
import discordforrad.models.language.wordnetwork.forms.RelatedFormsTransition;
import net.dv8tion.jda.api.entities.TextChannel;

public class WordDescription implements Serializable {
	private static final File cacheFile = Paths.get(Main.ROOT_DATABASE+"caches/word_description_cache.obj").toFile();
	static final PlainObjectFileBasedCache<Map<LanguageWord, WordDescription>> cache = PlainObjectFileBasedCache.loadFromFile(cacheFile, ()->new HashMap<>());
	public static enum WordType{UNDEFINED, ADV, VTR, N, S,UTTR, PREP, ADJ, V_EXPR,VI,VITR, VITR_PART, RAKN, PRON,
		N_PL,EXPR, INTERJ, CONJ, VBAL_UTTR, VTR_PHRASAL_SEP, VTR_PHRASAL_INSEP, VTR_PARTIKEL_OSKJ, V_PRES,
		PREFIX, EGEN_NAMN, SPL, N_AS_ADJ, V_AUX, HJV, V, VTR_PARTIKEL_SKJ,ARTICLE,
		ADJECTIVE_ENDING, NAME,
		VI_PHRASAL;

		public static WordType parse(String type) {
			if(type.startsWith("obestämd ")) return parse(type.substring(new String("obestämd ").length()));
			if(type.equals("adv")||type.startsWith("adverb"))return ADV;
			if(type.equals("vtr")||type.equals("vtr + refl"))return VTR;
			if(type.equals("namn"))return NAME;
			if(type.equals("i sammansättningar"))return NAME;
			if(type.equals("i sms."))return UNDEFINED;
			if(type.equals("suffix"))return NAME;
			if(type.equals("n")||type.startsWith("substantiv")||type.equals("oböjligt substantiv")
					||type.equals("subst adj")
					)return N;
			if(type.equals("s"))return S;
			if(type.equals("uttr"))return UTTR;
			if(type.equals("prep")||type.startsWith("preposition"))return PREP;
			if(type.equals("adj")||type.startsWith("adjektiv")
					||type.equals("oböjligt adjektiv"))return ADJ;
			if(type.equals("adjektiviskt slutled")) return N;
			if(type.equals("v expr"))return V_EXPR;
			if(type.equals("indef art")||type.equals("art")||type.equals("def art")||type.equals("artikel")
					||type.equals("bestämd artikel")
					)return ARTICLE;
			if(type.equals("v past p"))return V;
			if(type.equals("v perf part"))return V;
			if(type.equals("v pret"))return V;
			if(type.equals("vi"))return VI;
			if(type.equals("vitr"))return VITR;
			if(type.equals("vitr partikel"))return VITR_PART;
			if(type.equals("räkn")||type.equals("räkneord"))return RAKN;
			if(type.equals("pron")||type.endsWith("pronomen"))return PRON;
			if(type.equals("npl"))return N_PL;
			if(type.equals("expr"))return EXPR;
			if(type.equals("interj"))return INTERJ;
			if(type.equals("konj") || type.equals("subjunktion")||type.equals("konjunktion"))return CONJ;
			if(type.equals("conj"))return CONJ;
			if(type.equals("vbal uttr"))return VBAL_UTTR;
			if(type.equals("vtr phrasal sep"))return VTR_PHRASAL_SEP;
			if(type.equals("vtr phrasal insep"))return VTR_PHRASAL_INSEP;
			if(type.equals("vtr partikel oskj"))return VTR_PARTIKEL_OSKJ;
			if(type.equals("vtr partikel skj"))return VTR_PARTIKEL_SKJ;
			if(type.equals("v pres"))return V_PRES;
			if(type.equals("infinitivmärke"))return V;
			if(type.equals("prefix"))return PREFIX;
			if(type.equals("egen"))return EGEN_NAMN;
			if(type.equals("spl"))return SPL;
			if(type.equals("n as adj"))return N_AS_ADJ;
			if(type.equals("v aux"))return V_AUX;
			if(type.startsWith("v "))return V;
			if(type.equals("hjv"))return HJV;
			if(type.startsWith("verb"))return V;
			if(type.equals("vi phrasal"))return VI_PHRASAL;
			if(type.equals("interjektion"))return INTERJ;
			if(type.equals("förled"))return ADV;
			if(type.equals("contraction"))return UNDEFINED;
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

		private Set<TranslationDescription> translations=new HashSet<>();
		private Set<StringPair> contextSentences;
		private final LanguageWord lw;
		private RelatedFormsTransition alternativeForms;
		
		private final SubformTree subforms;
		

		public WordDescription(LanguageWord lw, Set<StringPair> contextSentences2, 
				RelatedFormsTransition af
				) {
			this.lw = lw;
			this.contextSentences = contextSentences2;
			this.alternativeForms = af;
			subforms = SubformTree.newInstance(lw);
			assert(subforms!=null);
		}

		private static final boolean WITH_GOOGLE_TRANSLATE = false;
		private static final boolean WITH_BAB_LA = false;
		public static WordDescription getDescriptionFor(LanguageWord lw) {
			if(cache.get().containsKey(lw))return cache.get().get(lw);
			if(!Dictionnary.isInDictionnaries(lw))
				throw new Error(lw+" not in dictionnaries");


			LanguageCode translateTo = LanguageCode.otherLanguage(lw.getCode());

			

			Set<StringPair> contextSentences = new HashSet<>();
	/*		if(Dictionnary.isInBabLaDisctionnary(lw))
			{
				//<a class="sound-inline bab-quick-sound"


					translations.addAll(Translator.getBabLaTranslationDescriptions(lw, translateTo));

				

				contextSentences.addAll(getBabLaContextSentencesFrom(lw, babLaInput));
			}*/

			//}


			WordDescription res = new WordDescription(lw,contextSentences, RelatedFormsNetwork.getRelatedForms(lw));
			cache.get().put(lw, res);
			//cache.doAndUpdate(x->{});
			return res;

		}
		
		public static void updateCache() {
			cache.doAndUpdate(x->{});
		}

		public static WordType getWordTypeOf(String split) {
			if(split.contains("<em class=\"tooltip POS2\">"))
			{
				String type = split.substring(split.indexOf("<em class=\"tooltip POS2\">")+25,
						split.indexOf("<span>")).trim();
				return WordType.parse(type);
			}
			return WordType.UNDEFINED;
		}

		public static List<String> getBabLaDefinitionsFrom(LanguageWord lw, String babLaInput) {			
			List<String> res = BabLaProcessing.getBabLaDirectDefinitionsFrom(lw,babLaInput);
			
			List<String> translationsAndExamples = getBabLaDefinitionsFromTranslationsAndExamples(lw,babLaInput);
			
			res.addAll(translationsAndExamples);

			return res;
		}

		private static List<String> getBabLaDefinitionsFromTranslationsAndExamples(LanguageWord lw,
				String babLaInput) {
		/*	if(lw.getWord().startsWith("atn"))
				System.out.println();
			*/
			String inputForDefinitions = babLaInput;
			
			
			String startString = "<h2 class=\"section-label\"> Translations &amp; Examples</h2>";
			if(inputForDefinitions.contains(startString))
				inputForDefinitions = inputForDefinitions.substring(inputForDefinitions.indexOf(startString));
			
			List<String> subparts = Arrays.asList(inputForDefinitions.split("<h3>"));
			subparts = subparts.subList(1, subparts.size());
			
			subparts = subparts.stream().map(x->x.substring(x.indexOf(">")+1)).collect(Collectors.toList());
			subparts = subparts.stream().filter(x->x.toLowerCase().startsWith(lw.getWord()+"<")).collect(Collectors.toList());
			subparts = subparts.stream().map(x->Arrays.asList(x.split("\n"))).reduce(new ArrayList<String>(), (x,y)->{x.addAll(y); return x;});
			subparts = subparts.stream().map(x->x.trim())
					.filter(x->x.startsWith("<strong>"))
					.filter(x->x.endsWith("</strong>"))
					.map(x->x.replaceAll("<strong>", ""))
					.map(x->x.replaceAll("</strong>", ""))
					.collect(Collectors.toList());
			
			return subparts;
			/*while(inputForDefinitions.contains("\n "))
				inputForDefinitions = inputForDefinitions.replaceAll("\n ","\n");
			List<String> allLines = Arrays.asList(inputForDefinitions.split("\n"));
			allLines = allLines.stream().filter(x->x.startsWith("<li>")).collect(Collectors.toList());
			allLines = allLines.stream().filter(x->x.contains("title=")).map(x->x.substring(x.indexOf("title="))).collect(Collectors.toList());
			allLines = allLines.stream().map(x->x.substring(x.indexOf(">")+1)).collect(Collectors.toList());
			allLines = allLines.stream().map(x->x.substring(0,x.indexOf("<"))).collect(Collectors.toList());
			
			return allLines;*/
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


		public LanguageWord getWord() {
			return lw;
		}

		public Set<WordType> getWordTypes() {
			return alternativeForms.getForms().keySet();
		}

		public RelatedForms getAlternativesFor(WordType wt) {
			return alternativeForms.getForms().get(wt);
		}
		
		public String toString()
		{
			return this.lw+":"+this.translations+":"+this.alternativeForms+":"+this.alternativeForms;
		}

		public SubformTree getSubforms() {
			return SubformTree.newInstance(lw);
		}

		public Set<RelatedForms> getAllAlternativeForms() {
			return alternativeForms.getForms().values().stream().collect(Collectors.toSet());
		}

		public static Set<LanguageWord> getGrundforms(LanguageWord lw2) {
			RelatedFormsTransition trans = RelatedFormsNetwork.getRelatedForms(lw2); 
			return 
					trans.getForms().values().stream()
					.map(x->
					{
						if(x.getGrundform()==null)
							throw new Error();
						return x.getGrundform();
					})
					.collect(Collectors.toSet());
		}

}
