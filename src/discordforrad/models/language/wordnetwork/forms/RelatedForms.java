package discordforrad.models.language.wordnetwork.forms;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;

import discordforrad.inputUtils.WebScrapping.DataBaseEnum;
import discordforrad.models.language.LanguageCode;
import discordforrad.models.language.LanguageWord;
import discordforrad.models.language.WordDescription;
import discordforrad.models.language.WordDescription.WordType;

public interface RelatedForms {

	public final static String SAOL_FORMS_SPLITTER_TOKEN = "<span class=\"expansion\"";
	public static final String PAYLOAD_BEACON_SAOL = "<span class=\"bform\">";

	public RelatedForms blendWith(RelatedForms r);

	public enum NounFormEnum implements RelatedFormType{
		OBESTAMD_FORM_SINGULAR,
		BESTAMD_FORM_SINGULAR,
		OBESTAMD_FORM_PLURAL,
		BESTAMD_FORM_PLURAL,

		OBESTAMD_FORM_GENITIV_SINGULAR,
		BESTAMD_FORM_GENITIV_SINGULAR,
		OBESTAMD_FORM_GENITIV_PLURAL,
		BESTAMD_FORM_GENITIV_PLURAL, OTHER
	}

	public enum PronounFormEnum implements RelatedFormType {
		GRUNDFORM_UTRUM_SINGULAR,GRUNDFORM_UTRUM_PLURAL, GRUNDFORM_NEUTRUM, GRUNDFORM_PLURAL, GENITIV_UTRUM_SINGULAR,
		OBJEKTSFORM_SINGULAR,OBJEKTSFORM_PLURAL
	}

	public enum AdjectiveFormsEnum implements RelatedFormType {
		ENN, ETT,  PLURAL, DEFINED,MASCULINUM, COMPARATIVE, SUPERLATIVE, SUPERLATIVE_DEFINED		
	}

	public enum VerbAlternativeFormEnum implements RelatedFormType{
		IMPERATIV, INFINITIV, PRESENS, PRETERITUM, SUPINUM,
		PRESENS_PARTICIP, PAST_PARTICIP_ENN, PAST_PARTICIP_ETT, PAST_PARTICIP_PLUR, PERFEKT_PARTICIP_DEF,
		INFINITIV_PASSIV, PRESENS_PASSIV, PRETERITUM_PASSIV, SUPINUM_PASSIV, PRESENS_KUNJUNKTIV, PRETERITUM_KUNJUNKTIV;
	}


	public static RelatedForms parseFrom(LanguageWord lw, String wholeForm, DataBaseEnum db) {

		String comparedForm = wholeForm.replaceAll("\n", "").replaceAll("\r", "").replaceAll(" ", "");
		if(comparedForm.contains("ingen</span><spanclass=\"tempmm\">böjning"))
			return UnmodifiableAlternativeForm.newInstance(lw);

		WordType wt = WordType.parseRaw(wholeForm, db);

		if(isUnmodifiableWordType(wt))
			return UnmodifiableAlternativeForm.newInstance(lw);
		if(wt.equals(WordType.V))
		{
			LanguageWord imperativAktiv = null;
			LanguageWord infinitivAktiv=null;
			LanguageWord presens = null;
			LanguageWord preteritumAktiv = null;
			LanguageWord supinumAktiv = null;
			LanguageWord presentParticip = null;
			LanguageWord pastParticipEnn = null;
			LanguageWord pastParticipEtt = null;
			LanguageWord pastParticipPlur = null;
			LanguageWord perfektParticipDef = null;
			LanguageWord presensPassiv = null;
			LanguageWord preteritumPassiv = null;
			LanguageWord presensKonjunktiv = null;
			LanguageWord infinitivPassiv = null;
			LanguageWord supinumPassiv = null;
			LanguageWord preteritumKonjunktiv = null;

			if(db.equals(DataBaseEnum.SO))
			{
				if(wholeForm.contains("<span class=\"orto\">"))
				{
					String infinitivStart = wholeForm.substring(wholeForm.indexOf("<span class=\"orto\">"));
					String infinitivEnd = infinitivStart.substring(new String("<span class=\"orto\">").length(), infinitivStart.indexOf("</span>"));
					String payload = infinitivEnd.trim();
					infinitivAktiv = LanguageWord.newInstance(payload,LanguageCode.SV);
				}

				if(wholeForm.contains("<span class=\"bojning\">"))
				{
					String pretetitumStart = wholeForm.substring(wholeForm.indexOf("<span class=\"bojning\">"));
					String preteritumEnd = pretetitumStart.substring(new String("<span class=\"bojning\">").length(), pretetitumStart.indexOf("</span>"));
					String payload = preteritumEnd.trim();
					preteritumAktiv = LanguageWord.newInstance(payload,LanguageCode.SV);
				}

				String remainingAfterPreteritum = wholeForm.substring(wholeForm.indexOf("<span class=\"bojning\">")+1);

				if(remainingAfterPreteritum.contains("<span class=\"bojning\">"))
				{
					String imperativeStart = remainingAfterPreteritum.substring(remainingAfterPreteritum.indexOf("<span class=\"bojning\">"));
					String supinumEnd = imperativeStart.substring(new String("<span class=\"bojning\">").length(), imperativeStart.indexOf("</span>"));
					supinumAktiv = LanguageWord.newInstance(supinumEnd.trim(),LanguageCode.SV);
				}
			}
			else if(db.equals(DataBaseEnum.SAOL))
			{
				final String subsplitType = "<td class=\"ledtext\">";
				String forms = wholeForm.substring(wholeForm.indexOf(SAOL_FORMS_SPLITTER_TOKEN));

				String startingPattern = "<span class=\"bform\">";
				for(String bit : forms.split("<i>"))
				{
					if(bit.startsWith("<span class=\"expansion\""))continue;
					if(bit.trim().startsWith("Finita former"))
					{
						for(String subbit:bit.split("<td class=\"ordform\">"))
						{
							String headerNewForm = "<span class=\"bform\">";
							subbit = subbit.substring(subbit.indexOf(headerNewForm)+headerNewForm.length());
							if(subbit.trim().startsWith("Finita former"))continue;
							String payload = subbit.substring(0, subbit.indexOf("</span>")).replaceAll("-", "").trim();
							String type = subbit.substring(subbit.indexOf(subsplitType)+subsplitType.length());
							type = type.substring(0,type.indexOf("</td>")).trim();
							if(type.equals("presens aktiv")||type.equals("presens deponens")) {presens = LanguageWord.newInstance(payload,LanguageCode.SV);; continue;}
							if(type.equals("preteritum aktiv")||type.equals("preteritum deponens")) {preteritumAktiv = LanguageWord.newInstance(payload,LanguageCode.SV);; continue;}
							if(type.equals("imperativ aktiv")||type.equals("imperativ deponens")) {imperativAktiv = LanguageWord.newInstance(payload,LanguageCode.SV);; continue;}
							if(type.equals("presens passiv")) {presensPassiv = LanguageWord.newInstance(payload,LanguageCode.SV);; continue;}
							if(type.equals("preteritum passiv")) {preteritumPassiv = LanguageWord.newInstance(payload,LanguageCode.SV);; continue;}
							if(type.equals("presens konjunktiv")) {presensKonjunktiv = LanguageWord.newInstance(payload,LanguageCode.SV);; continue;}
							if(type.equals("preteritum konjunktiv")) {preteritumKonjunktiv = LanguageWord.newInstance(payload,LanguageCode.SV);; continue;}
							throw new Error();
						}
						continue;
					}

					if(bit.trim().startsWith("Infinita former"))
					{
						for(String subbit:bit.split("<span class=\"bform\">"))
						{
							if(subbit.trim().startsWith("Infinita former"))continue;
							throw new Error();
						}
						continue;
					}
					if(bit.trim().startsWith("att"))
					{
						String payload = bit.substring(bit.indexOf(PAYLOAD_BEACON_SAOL)+PAYLOAD_BEACON_SAOL.length(), bit.indexOf("</span>")).trim().replaceAll("-", "");
						if(!bit.contains(subsplitType))continue;
						String type = bit.substring(bit.indexOf(subsplitType)+subsplitType.length());
						type = type.substring(0,type.indexOf("</td>")).trim();
						if(type.equals("infinitiv aktiv")||type.equals("infinitiv deponens")) {infinitivAktiv=LanguageWord.newInstance(payload,LanguageCode.SV);}
						else if(type.equals("infinitiv passiv"))infinitivPassiv = LanguageWord.newInstance(payload,LanguageCode.SV);
						else throw new Error();
						continue;
					}
					if(bit.trim().startsWith("har/hade"))
					{
						String payload = bit.substring(bit.indexOf(PAYLOAD_BEACON_SAOL)+PAYLOAD_BEACON_SAOL.length(), bit.indexOf("</span>")).trim();
						String type = bit.substring(bit.indexOf(subsplitType)+subsplitType.length());
						type = type.substring(0,type.indexOf("</td>")).trim();
						if(type.equals("supinum aktiv")||type.equals("supinum deponens"))supinumAktiv=LanguageWord.newInstance(payload,LanguageCode.SV);
						else if(type.equals("supinum passiv"))supinumPassiv = LanguageWord.newInstance(payload,LanguageCode.SV);
						else throw new Error();
						continue;
					}
					if(bit.trim().startsWith("Presens particip"))
					{
						String payload = bit.substring(bit.indexOf(PAYLOAD_BEACON_SAOL)+PAYLOAD_BEACON_SAOL.length(), bit.indexOf("</span>")).trim();
						presentParticip=LanguageWord.newInstance(payload,LanguageCode.SV);
						continue;
					}

					if(bit.trim().startsWith("Perfekt particip")) continue;
					if(bit.trim().startsWith("en"))
					{
						String payload = bit.substring(bit.indexOf(PAYLOAD_BEACON_SAOL)+PAYLOAD_BEACON_SAOL.length(), bit.indexOf("</span>")).trim();
						pastParticipEnn=LanguageWord.newInstance(payload,LanguageCode.SV);;
						continue;
					}
					if(bit.trim().startsWith("ett"))
					{
						String payload = bit.substring(bit.indexOf(PAYLOAD_BEACON_SAOL)+PAYLOAD_BEACON_SAOL.length(), bit.indexOf("</span>")).trim();
						pastParticipEtt=LanguageWord.newInstance(payload,LanguageCode.SV);;
						continue;
					}
					if(bit.trim().startsWith("den/det/de"))
					{
						String payload = bit.substring(bit.indexOf(PAYLOAD_BEACON_SAOL)+PAYLOAD_BEACON_SAOL.length(), bit.indexOf("</span>")).trim();
						pastParticipPlur=LanguageWord.newInstance(payload,LanguageCode.SV);;
						continue;
					}
					if(bit.substring(0,bit.indexOf("<")).trim().equals("den"))
					{
						String payload = bit.substring(bit.indexOf(PAYLOAD_BEACON_SAOL)+PAYLOAD_BEACON_SAOL.length(), bit.indexOf("</span>")).trim();
						perfektParticipDef=LanguageWord.newInstance(payload,LanguageCode.SV);;
						continue;
					}
					/*if(bit.trim().startsWith("Positiv"))continue;
					if(bit.trim().startsWith("en"))
					{
						String startEnn = bit.substring(bit.indexOf(startingPattern)+startingPattern.length());
						enn = startEnn.substring(0,startEnn.indexOf("</span>")).trim();
						continue;
					}
					if(bit.trim().startsWith("ett"))
					{
						String startEtt = bit.substring(bit.indexOf(startingPattern)+startingPattern.length());
						ett = startEtt.substring(0,startEtt.indexOf("</span>")).trim();
						continue;
					}
					if(bit.trim().startsWith("den/det/de"))
					{
						String startPlural = bit.substring(bit.indexOf(startingPattern)+startingPattern.length());
						plural = startPlural.substring(0,startPlural.indexOf("</span>")).trim();
						continue;
					}*/
					if(bit.substring(0,bit.indexOf("<")).trim().equals("Ordform(er)"))continue;
					if(bit.substring(0,bit.indexOf("<")).trim().equals("Övrig(a) form(er)"))continue;

				}
			}

			return VerbAlternativeForm.newInstance(imperativAktiv,
					infinitivAktiv, presens, presensKonjunktiv, preteritumAktiv, presensKonjunktiv,supinumAktiv,
					presentParticip,pastParticipEnn,pastParticipEtt,pastParticipPlur,perfektParticipDef,
					presensPassiv, preteritumPassiv, infinitivPassiv, supinumPassiv);			
		}
		if(wt.equals(WordType.N))
			return parseNoun(wholeForm,db,lw);
		/*if(wt.equals(WordType.PRON))
			return parsePronoun(wholeForm,db);*/

		if(wt.equals(WordType.ADJ))
		{
			return parseAdjective(wholeForm, db);
		}
		throw new Error();
	}


	public static boolean isUnmodifiableWordType(WordType wt) {
		return wt.equals(WordType.ADV)
				||wt.equals(WordType.CONJ)
				||wt.equals(WordType.UNDEFINED)
				||wt.equals(WordType.RAKN)
				||wt.equals(WordType.INTERJ)
				||wt.equals(WordType.NAME)
				||wt.equals(WordType.PRON)
				||wt.equals(WordType.ARTICLE)
				||wt.equals(WordType.PREP);
	}


	public static RelatedForms parseAdjective(String wholeForm, DataBaseEnum db) {
		LanguageWord enn = null;
		LanguageWord ett = null;
		LanguageWord defined = null;
		LanguageWord plural = null;
		LanguageWord masculinum = null;
		LanguageWord superlativ = null;
		LanguageWord superlativDefined = null;
		LanguageWord komparativ = null;
		LanguageWord komparativDefined = null;

		if(db.equals(DataBaseEnum.SO))
		{
			if(wholeForm.contains("<span class=\"orto\">"))
			{
				String infinitivStart = wholeForm.substring(wholeForm.indexOf("<span class=\"orto\">"));
				String infinitivEnd = infinitivStart.substring(new String("<span class=\"orto\">").length(), infinitivStart.indexOf("</span>")).trim();
				if(infinitivEnd.startsWith("-"))infinitivEnd = infinitivEnd.substring(1);
				enn = LanguageWord.newInstance(infinitivEnd.trim(), LanguageCode.SV);
			}

			for(String split:wholeForm.split("<span class=\"tempmm\">"))
			{
				if(split.contains("<span class=\"orto\">"))continue;
				if(!split.contains("<span class=\"bojning\">"))continue;

				String payloadStart = split.substring(split.indexOf("<span class=\"bojning\">"));
				String payloadEnd = payloadStart.substring(new String("<span class=\"bojning\">").length(), payloadStart.indexOf("</span>"));
				String actualContents = payloadEnd.trim().replaceAll("-", "");

				if(split.contains("maskulinum"))
					masculinum = LanguageWord.newInstance(actualContents.trim(), LanguageCode.SV);
				else if(split.contains("superlativ"))
					superlativ = LanguageWord.newInstance(actualContents.trim(), LanguageCode.SV);
				else if(split.trim().startsWith("neutrum"))
				{
					enn = LanguageWord.newInstance(actualContents.trim(), LanguageCode.SV);
					ett = LanguageWord.newInstance(actualContents.trim(), LanguageCode.SV);
				}
				else if(split.trim().startsWith("plural"))
				{
					plural = LanguageWord.newInstance(actualContents.trim(), LanguageCode.SV);
				}
				else if(split.trim().startsWith("används"))continue;
				else if(split.trim().startsWith("komparativ"))komparativ = LanguageWord.newInstance(actualContents.trim(), LanguageCode.SV);
				else continue;
				//					throw new Error();
			}

		}
		else if(db.equals(DataBaseEnum.SAOL))
		{
			final int DEFAULT = 0;
			final int KOMPARATIV = 1;
			final int SUPERLATIV = 2;
			int mode = DEFAULT;

			String forms = wholeForm.substring(wholeForm.indexOf(SAOL_FORMS_SPLITTER_TOKEN));

			boolean isUnmodifiableAdjective = wholeForm.substring(0, wholeForm.indexOf("<")).trim()
					.equals("oböjligt adjektiv");

			String startingPattern = "<span class=\"bform\">";
			for(String bit : forms.split("<i>"))
			{
				if(bit.startsWith("<span class=\"expansion\""))continue;
				String header = bit.substring(0,bit.indexOf("<")).trim();
				if(header.equals("Ordform(er)"))continue;
				if(header.equals("Övrig(a) ordform(er)"))continue;
				if(header.startsWith("Positiv"))continue;
				if(header.startsWith("Substantiverat adjektiv"))continue;
				if((header.equals("en")||header.startsWith("en/"))
						&&mode==DEFAULT
						||(header.equals("är")&& isUnmodifiableAdjective)
						)
				{
					String startEnn = bit.substring(bit.indexOf(startingPattern)+startingPattern.length());
					startEnn= startEnn.substring(0,startEnn.indexOf("</span>")).trim();
					if(startEnn.startsWith("-"))startEnn = startEnn.substring(1);
					enn = LanguageWord.newInstance(startEnn, LanguageCode.SV);
					continue;
				}
				if(header.equals("ett"))
				{
					String startEtt = bit.substring(bit.indexOf(startingPattern)+startingPattern.length());
					startEtt= startEtt.substring(0,startEtt.indexOf("</span>")).trim();
					if(startEtt.startsWith("-"))startEtt = startEtt.substring(1);
					ett = LanguageWord.newInstance(startEtt, LanguageCode.SV);
					continue;
				}
				if(header.equals("den/det/de")||header.equals("de"))
				{
					String startPlural = bit.substring(bit.indexOf(startingPattern)+startingPattern.length());
					String payload = startPlural.substring(0,startPlural.indexOf("</span>")).trim();
					if(payload.startsWith("-"))payload = payload.substring(1);
					if(mode==DEFAULT)plural = LanguageWord.newInstance(payload, LanguageCode.SV);
					else if(mode==SUPERLATIV)superlativDefined = LanguageWord.newInstance(payload, LanguageCode.SV);
					else if(mode==KOMPARATIV)komparativDefined = LanguageWord.newInstance(payload, LanguageCode.SV);
					else 
						throw new Error();
					continue;
				}
				if( header.equals("den")||header.equals("den/det"))
				{
					String startDefinite = bit.substring(bit.indexOf(startingPattern)+startingPattern.length());
					startDefinite= startDefinite.substring(0,startDefinite.indexOf("</span>")).trim();
					if(startDefinite.startsWith("-")) startDefinite = startDefinite.substring(1);
					defined = LanguageWord.newInstance(startDefinite, LanguageCode.SV);
					continue;
				}
				if(bit.trim().startsWith("Komparativ\r")||header.equals("Adjektiv (i komparativ)"))
				{
					mode = KOMPARATIV;
					continue;
				}
				if(bit.trim().startsWith("Superlativ\r"))
				{
					mode = SUPERLATIV;
					continue;
				}
				if(mode==KOMPARATIV&&bit.trim().startsWith("en/ett/den/det/de"))
				{
					String startKomparativ = bit.substring(bit.indexOf(startingPattern)+startingPattern.length());
					startKomparativ= startKomparativ.substring(0,startKomparativ.indexOf("</span>")).trim();
					if(startKomparativ.startsWith("-"))startKomparativ = startKomparativ.substring(1);
					komparativ = LanguageWord.newInstance(startKomparativ, LanguageCode.SV);
					continue;
				}

				if(mode==SUPERLATIV&&header.equals("är"))
				{
					String startSuperlativ = bit.substring(bit.indexOf(startingPattern)+startingPattern.length());
					startSuperlativ = startSuperlativ.substring(0,startSuperlativ.indexOf("</span>")).trim();
					if(startSuperlativ.startsWith("-"))startSuperlativ = startSuperlativ.substring(1);
					superlativ = LanguageWord.newInstance(startSuperlativ, LanguageCode.SV);
					continue;
				}
				if(header.equals("Adverb"))continue; //some words are both adjectives and adverbs; the splitting is not so great
				//if useful, have the system checkout for such splits and return sets of forms instead
				throw new Error();
			}


		}

		return AdjectiveRelatedForms.newInstance(enn,ett,plural,defined,masculinum,komparativ, superlativ,superlativDefined);
	}


	public static RelatedForms parsePronoun(String wholeForm, DataBaseEnum db) {
		Map<PronounFormEnum, LanguageWord> res = new HashMap<>();
		if(db.equals(DataBaseEnum.SAOL))
		{
			String forms = wholeForm.substring(wholeForm.indexOf(SAOL_FORMS_SPLITTER_TOKEN));

			String startingPattern = "<span class=\"bform\">";

			final int UNDEFINED = 0; final int SINGULAR = 1; final int PLURAL = 2;
			int mode = UNDEFINED;


			for(String bit : forms.split("<b>|<i>"))
			{
				/*	if(bit.contains("<i>") &&bit.substring(bit.indexOf("<i>")+3,bit.indexOf("</i>")).trim().equals("Singular"))
					mode = SINGULAR;*/
				if(bit.startsWith("<span class=\"expansion\""))continue;
				if(bit.substring(0,bit.indexOf("<")).trim().equals("pronomen"))continue;
				String header = bit.substring(0, bit.indexOf("<")).trim();
				if(header.equals("Singular")) {mode = SINGULAR; continue;}
				if(header.equals("Plural")) {mode = PLURAL; continue;}
				if(!bit.contains(SAOL_PAYLOAD_INDICATOR))continue;
				String payload = bit.substring(bit.indexOf(SAOL_PAYLOAD_INDICATOR)+SAOL_PAYLOAD_INDICATOR.length());
				payload = payload.substring(0, payload.indexOf("<")).trim();
				String typeRelation = bit.substring(bit.indexOf(SAOL_RELATION_INDICATOR)+ SAOL_RELATION_INDICATOR.length());
				typeRelation = typeRelation.substring(0,typeRelation.indexOf("<")).trim();

				if(typeRelation.equals("grundform utrum"))
					if(mode==SINGULAR) {res.put(PronounFormEnum.GRUNDFORM_UTRUM_SINGULAR, LanguageWord.newInstance(payload, LanguageCode.SV)); continue;}
					else if(mode==PLURAL) {res.put(PronounFormEnum.GRUNDFORM_UTRUM_PLURAL, LanguageWord.newInstance(payload, LanguageCode.SV)); continue;}
				if(typeRelation.equals("grundform neutrum"))
					if(mode==SINGULAR) {res.put(PronounFormEnum.GRUNDFORM_NEUTRUM, LanguageWord.newInstance(payload, LanguageCode.SV)); continue;}
				if(typeRelation.equals("grundform"))
					if(mode==PLURAL) {res.put(PronounFormEnum.GRUNDFORM_PLURAL, LanguageWord.newInstance(payload, LanguageCode.SV)); continue;}	
				if(typeRelation.equals("genitiv utrum"))
					if(mode==SINGULAR) {res.put(PronounFormEnum.GENITIV_UTRUM_SINGULAR, LanguageWord.newInstance(payload, LanguageCode.SV)); continue;}	
				if(typeRelation.equals("objektsform"))
					if(mode==SINGULAR) {res.put(PronounFormEnum.OBJEKTSFORM_SINGULAR, LanguageWord.newInstance(payload, LanguageCode.SV)); continue;}
					else if(mode==PLURAL) {res.put(PronounFormEnum.OBJEKTSFORM_PLURAL, LanguageWord.newInstance(payload, LanguageCode.SV)); continue;}


				//	throw new Error();
			}

		}
		else if(db.equals(DataBaseEnum.SO))
		{
			String header = "<span class=\"orto\">";
			String payload = wholeForm.substring(
					wholeForm.indexOf(header)+header.length());
			payload  =payload.substring(0,payload.indexOf("<")).trim();

			res.put(PronounFormEnum.GRUNDFORM_UTRUM_SINGULAR, LanguageWord.newInstance(payload,LanguageCode.SV));

		}


		return PronounRelatedForm.newInstance(res);
	}


	public static final String SAOL_PAYLOAD_INDICATOR = "<span class=\"bform\">";
	public static final String SAOL_RELATION_INDICATOR = "<td class=\"ledtext\">";
	public static RelatedForms parseNoun(String wholeForm, DataBaseEnum db, LanguageWord lw) {
		Map<NounFormEnum, LanguageWord> res = new HashMap<>();

		if(db.equals(DataBaseEnum.SAOL))
		{
			String forms = wholeForm.substring(wholeForm.indexOf(SAOL_FORMS_SPLITTER_TOKEN));

			String startingPattern = "<span class=\"bform\">";

			final int UNDEFINED = 0; final int SINGULAR = 1; final int PLURAL = 2;
			int mode = UNDEFINED;

			String wholeHeader = wholeForm.substring(0,wholeForm.indexOf("<")).trim();
			boolean isSpecialAdjective = wholeHeader.equals("adjektiviskt slutled")||wholeHeader.equals("oböjligt substantiv");

			if(isSpecialAdjective)
			{
				return UnmodifiableAlternativeForm.newInstance(lw);
			}
			for(String bit : forms.split("<b>|<i>"))
			{
				/*	if(bit.contains("<i>") &&bit.substring(bit.indexOf("<i>")+3,bit.indexOf("</i>")).trim().equals("Singular"))
					mode = SINGULAR;*/
				if(bit.startsWith("<span class=\"expansion\""))continue;
				String header = bit.substring(0, bit.indexOf("<")).trim();
				if(header.equals("Singular")) {mode = SINGULAR; continue;}
				if(header.equals("Plural")) {mode = PLURAL; continue;}
				if(!bit.contains(SAOL_PAYLOAD_INDICATOR))continue;
				String payload = bit.substring(bit.indexOf(SAOL_PAYLOAD_INDICATOR)+SAOL_PAYLOAD_INDICATOR.length());
				payload = payload.substring(0, payload.indexOf("<")).trim();
				if(payload.startsWith("-"))
					payload = payload.substring(1);
				String typeRelation = bit.substring(bit.indexOf(SAOL_RELATION_INDICATOR)+ SAOL_RELATION_INDICATOR.length());
				typeRelation = typeRelation.substring(0,typeRelation.indexOf("<")).trim();
				
				if(wholeHeader.equals("substantiv i plural"))
					System.out.print("");
				if(typeRelation.equals("obestämd form")//||wholeHeader.equals("substantiv i plural")
						)
					if(mode==SINGULAR) {res.put(NounFormEnum.OBESTAMD_FORM_SINGULAR, LanguageWord.newInstance(payload, LanguageCode.SV)); continue;}
					else if(mode==PLURAL//|| wholeHeader.equals("substantiv i plural")
							) {
						if(payload.endsWith(":n")
								||payload.endsWith(":er")
								||payload.endsWith(":ar")
								)
							payload = payload.replaceAll(":", "-");
						res.put(NounFormEnum.OBESTAMD_FORM_PLURAL, LanguageWord.newInstance(payload, LanguageCode.SV)); continue;}
				if(typeRelation.equals("obestämd form genitiv"))
					if(mode==SINGULAR) {
						if(payload.endsWith(":s"))payload = payload.replaceAll(":", "-");
						res.put(NounFormEnum.OBESTAMD_FORM_GENITIV_SINGULAR, LanguageWord.newInstance(payload, LanguageCode.SV)); continue;}
					else if(mode==PLURAL) {
						if(payload.endsWith(":ns")||payload.endsWith(":ers")||
								payload.endsWith(":ars")||
								payload.endsWith(":s"))payload = payload.replaceAll(":", "-");
						res.put(NounFormEnum.OBESTAMD_FORM_GENITIV_PLURAL, LanguageWord.newInstance(payload, LanguageCode.SV)); continue;}
				if(typeRelation.equals("bestämd form"))
					if(mode==SINGULAR) {
						if(payload.endsWith(":et")||
								payload.endsWith(":t")||
								payload.endsWith(":n")||
								payload.endsWith(":en")
								)payload = payload.replaceAll(":", "-");
						res.put(NounFormEnum.BESTAMD_FORM_SINGULAR, LanguageWord.newInstance(payload, LanguageCode.SV)); continue;}
					else if(mode==PLURAL) {
						if(payload.endsWith(":na")||payload.endsWith(":erna")||
								payload.endsWith(":arna")||
								payload.endsWith(":en"))payload = payload.replaceAll(":", "-");
						res.put(NounFormEnum.BESTAMD_FORM_PLURAL, LanguageWord.newInstance(payload, LanguageCode.SV)); continue;}
				if(typeRelation.equals("bestämd form genitiv"))
					if(mode==SINGULAR) {
						if(payload.endsWith(":ets")||
								payload.endsWith(":ts")||
								payload.endsWith(":ns")||
								payload.endsWith(":ens")
								)payload = payload.replaceAll(":", "-");
						res.put(NounFormEnum.BESTAMD_FORM_GENITIV_SINGULAR, LanguageWord.newInstance(payload, LanguageCode.SV)); continue;}
					else if(mode==PLURAL) {
						if(payload.endsWith(":nas")||payload.endsWith(":ernas")||
								payload.endsWith(":arnas")||
								payload.endsWith(":ens"))payload = payload.replaceAll(":", "-");
						res.put(NounFormEnum.BESTAMD_FORM_GENITIV_PLURAL, LanguageWord.newInstance(payload, LanguageCode.SV)); continue;}
				if(typeRelation.equals("i vissa uttryck"))
				{res.put(NounFormEnum.OTHER, LanguageWord.newInstance(payload, LanguageCode.SV)); continue;}

				if(header.isBlank() && isSpecialAdjective) continue;
				//throw new Error();
			}
			/*final int DEFAULT = 0;
			final int KOMPARATIV = 1;
			final int SUPERLATIV = 2;
			int mode = DEFAULT;

			String forms = wholeForm.substring(wholeForm.indexOf(SAOL_FORMS_SPLITTER_TOKEN));


			for(String bit : forms.split("<i>"))
			{
				if(bit.startsWith("<span class=\"expansion\""))continue;
				if(bit.trim().startsWith("Positiv"))continue;
				if(bit.trim().startsWith("en\r")&&mode==DEFAULT)
				{
					String startEnn = bit.substring(bit.indexOf(startingPattern)+startingPattern.length());
					enn = startEnn.substring(0,startEnn.indexOf("</span>")).trim();
					continue;
				}
				if(bit.trim().startsWith("ett"))
				{
					String startEtt = bit.substring(bit.indexOf(startingPattern)+startingPattern.length());
					ett = startEtt.substring(0,startEtt.indexOf("</span>")).trim();
					continue;
				}
				if(bit.trim().startsWith("den/det/de"))
				{
					String startPlural = bit.substring(bit.indexOf(startingPattern)+startingPattern.length());
					String payload = startPlural.substring(0,startPlural.indexOf("</span>")).trim(); 
					if(mode==DEFAULT)plural = payload;
					else if(mode==SUPERLATIV)superlativDefined = payload;
					else throw new Error();
					continue;
				}
				if(bit.trim().startsWith("den\r"))
				{
					String startDefinite = bit.substring(bit.indexOf(startingPattern)+startingPattern.length());
					defined = startDefinite.substring(0,startDefinite.indexOf("</span>")).trim();
					continue;
				}
				if(bit.trim().startsWith("Komparativ\r"))
				{
					mode = KOMPARATIV;
					continue;
				}
				if(bit.trim().startsWith("Superlativ\r"))
				{
					mode = SUPERLATIV;
					continue;
				}
				if(mode==KOMPARATIV&&bit.trim().startsWith("en/ett/den/det/de"))
				{
					String startKomparativ = bit.substring(bit.indexOf(startingPattern)+startingPattern.length());
					komparativ = startKomparativ.substring(0,startKomparativ.indexOf("</span>")).trim();
					continue;
				}

				if(mode==SUPERLATIV&&bit.trim().startsWith("är"))
				{
					String startSuperlativ = bit.substring(bit.indexOf(startingPattern)+startingPattern.length());
					superlativ = startSuperlativ.substring(0,startSuperlativ.indexOf("</span>")).trim();
					continue;
				}
				throw new Error();
			}*/
		}
		else if(db.equals(DataBaseEnum.SO))
		{

			if(wholeForm.contains("<span class=\"orto\">"))
			{
				String infinitivStart = wholeForm.substring(wholeForm.indexOf("<span class=\"orto\">"));
				String payload = infinitivStart.substring(new String("<span class=\"orto\">").length(), infinitivStart.indexOf("</span>")).trim();
				if(payload.startsWith("-"))payload = payload.substring(1);
				res.put(NounFormEnum.OBESTAMD_FORM_SINGULAR,LanguageWord.newInstance(payload, LanguageCode.SV));
			}

			if(wholeForm.contains("<span class=\"bojning\">"))
			{
				String pretetitumStart = wholeForm.substring(wholeForm.indexOf("<span class=\"bojning\">"));
				String payload = pretetitumStart.substring(new String("<span class=\"bojning\">").length(), pretetitumStart.indexOf("</span>")).trim();
				if(payload.startsWith("-"))payload = payload.substring(1);
				if(payload.endsWith(":et")||payload.endsWith(":t")||payload.endsWith(":er")
						||payload.endsWith(":ar")
						||payload.endsWith(":n")||payload.endsWith(":en"))payload = payload.replaceAll(":", "-");
				res.put(NounFormEnum.BESTAMD_FORM_SINGULAR,LanguageWord.newInstance(payload, LanguageCode.SV));
			}

			wholeForm = wholeForm.substring(wholeForm.indexOf("<span class=\"bojning\">")+1);
			if(wholeForm.contains("<span class=\"bojning\">"))
			{
				String pretetitumStart = wholeForm.substring(wholeForm.indexOf("<span class=\"bojning\">"));
				String payload = pretetitumStart.substring(new String("<span class=\"bojning\">").length(), pretetitumStart.indexOf("</span>")).trim();
				if(payload.startsWith("-")) payload = payload.substring(1);
				if(payload.endsWith(":n")
						||payload.endsWith(":er")
						||payload.endsWith(":en")
						||payload.endsWith(":t")
						||payload.endsWith(":ar")) payload = payload.replaceAll(":", "-");
				res.put(NounFormEnum.OBESTAMD_FORM_PLURAL,LanguageWord.newInstance(payload, LanguageCode.SV));
			}

			wholeForm = wholeForm.substring(wholeForm.indexOf("<span class=\"bojning\">")+1);
			if(wholeForm.contains("<span class=\"bojning\">"))
			{
				String pretetitumStart = wholeForm.substring(wholeForm.indexOf("<span class=\"bojning\">"));
				String payload = pretetitumStart.substring(new String("<span class=\"bojning\">").length(), pretetitumStart.indexOf("</span>")).trim();
				if(payload.startsWith("-")) payload = payload.substring(1);
				if(payload.endsWith(":n")
						||payload.endsWith(":en")
						||payload.endsWith(":et")
						)payload = payload.replaceAll(":", "-");
				res.put(NounFormEnum.OBESTAMD_FORM_PLURAL,LanguageWord.newInstance(payload, LanguageCode.SV));
			}
		}
		
		if(res.isEmpty())
		{
			if(!wholeForm.contains("Ordform(er)"))
				throw new Error();
			String start = wholeForm.substring(wholeForm.indexOf("Ordform(er)"));
			start  = start.substring(start.indexOf("<span class=\"bform\">")+20);
			start = start.substring(0,start.indexOf("</span>")).trim();
			return UnmodifiableAlternativeForm.newInstance(LanguageWord.newInstance(start, lw.getCode()));
		}

		return NounAlternativeForm.newInstance(res);
	}


	public boolean containsForm(LanguageWord lw);


	public Set<LanguageWord> getRelatedWords();


	public LanguageWord getGrundform();


	public static RelatedForms parse(WordType wt, String string) {
		if(string.endsWith("unmodifiable"))
			return UnmodifiableAlternativeForm.newInstance(LanguageWord.parse(string.substring(0, string.indexOf(":unmodifiable"))));
		switch (wt) {
		case ADV: throw new Error();
		case N: return NounAlternativeForm.parse(string);
		case V: return VerbAlternativeForm.parse(string);
		case ADJ: return AdjectiveRelatedForms.parse(string);
		default:
			throw new IllegalArgumentException("Unexpected value: " + wt);
		}
		//throw new Error();
	}


	public String toParsableString();


	public JSONObject toJsonObject();


	public static RelatedForms fromJsonObject(WordType wt, JSONObject jsonObject) {
		if(isUnmodifiableWordType(wt)||jsonObject.containsKey("UNMODIFIABLE"))
			return UnmodifiableAlternativeForm.fromJsonObject(jsonObject);
		
		switch (wt) {
		case V: 
			return VerbAlternativeForm.fromJsonObject(jsonObject);
		case N:
			return NounAlternativeForm.fromJsonObject(jsonObject);
		case ADJ:
			return AdjectiveRelatedForms.fromJsonObject(jsonObject);
		default:
			throw new IllegalArgumentException("Unexpected value: " + wt);
		}
	}
}
