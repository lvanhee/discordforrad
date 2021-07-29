package discordforrad.models.language;

import java.util.HashMap;
import java.util.Map;

import discordforrad.inputUtils.WebScrapping.DataBaseEnum;
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
		ENN, ETT, DEFINED, PLURAL, MASCULINUM, COMPARATIVE, SUPERLATIVE, SUPERLATIVE_DEFINED		
	}
	
	public enum VerbAlternativeFormEnum implements RelatedFormType{
		IMPERATIV, INFINITIV, PRESENS, PRETERITUM, SUPINUM,
		PRESENS_PARTICIP, PAST_PARTICIP_ENN, PAST_PARTICIP_ETT, PAST_PARTICIP_PLUR, PERFEKT_PARTICIP_DEF,
		INFINITIV_PASSIV, PRESENS_PASSIV, PRETERITUM_PASSIV, SUPINUM_PASSIV;
	}


	public static RelatedForms parseFrom(String wholeForm, DataBaseEnum db) {

		String comparedForm = wholeForm.replaceAll("\n", "").replaceAll("\r", "").replaceAll(" ", "");
		if(comparedForm.contains("ingen</span><spanclass=\"tempmm\">böjning"))
			return UnmodifiableAlternativeForm.INSTANCE;

		WordType wt = WordType.parseRaw(wholeForm, db);

		if(wt.equals(WordType.ADV)
				||wt.equals(WordType.CONJ)
				||wt.equals(WordType.RAKN)
				||wt.equals(WordType.INTERJ)
				||wt.equals(WordType.PREP))
			return UnmodifiableAlternativeForm.INSTANCE;
		if(wt.equals(WordType.V))
		{
			String imperativAktiv = null;
			String infinitivAktiv=null;
			String presens = null;
			String preteritumAktiv = null;
			String supinumAktiv = null;
			String presentParticip = null;
			String pastParticipEnn = null;
			String pastParticipEtt = null;
			String pastParticipPlur = null;
			String perfektParticipDef = null;
			String presensPassiv = null;
			String preteritumPassiv = null;
			String infinitivPassiv = null;
			String supinumPassiv = null;

			if(db.equals(DataBaseEnum.SO))
			{
				if(wholeForm.contains("<span class=\"orto\">"))
				{
					String infinitivStart = wholeForm.substring(wholeForm.indexOf("<span class=\"orto\">"));
					String infinitivEnd = infinitivStart.substring(new String("<span class=\"orto\">").length(), infinitivStart.indexOf("</span>"));
					infinitivAktiv = infinitivEnd.trim();
				}

				if(wholeForm.contains("<span class=\"bojning\">"))
				{
					String pretetitumStart = wholeForm.substring(wholeForm.indexOf("<span class=\"bojning\">"));
					String preteritumEnd = pretetitumStart.substring(new String("<span class=\"bojning\">").length(), pretetitumStart.indexOf("</span>"));
					preteritumAktiv = preteritumEnd.trim();
				}

				String remainingAfterPreteritum = wholeForm.substring(wholeForm.indexOf("<span class=\"bojning\">")+1);

				if(remainingAfterPreteritum.contains("<span class=\"bojning\">"))
				{
					String imperativeStart = remainingAfterPreteritum.substring(remainingAfterPreteritum.indexOf("<span class=\"bojning\">"));
					String supinumEnd = imperativeStart.substring(new String("<span class=\"bojning\">").length(), imperativeStart.indexOf("</span>"));
					supinumAktiv = supinumEnd.trim();
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
							String payload = subbit.substring(0, subbit.indexOf("</span>")).trim();
							String type = subbit.substring(subbit.indexOf(subsplitType)+subsplitType.length());
							type = type.substring(0,type.indexOf("</td>")).trim();
							if(type.equals("presens aktiv")) {presens = payload; continue;}
							if(type.equals("preteritum aktiv")) {preteritumAktiv = payload; continue;}
							if(type.equals("imperativ aktiv")) {imperativAktiv = payload; continue;}
							if(type.equals("presens passiv")) {presensPassiv = payload; continue;}
							if(type.equals("preteritum passiv")) {preteritumPassiv = payload; continue;}
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
						String payload = bit.substring(bit.indexOf(PAYLOAD_BEACON_SAOL)+PAYLOAD_BEACON_SAOL.length(), bit.indexOf("</span>")).trim();
						if(!bit.contains(subsplitType))continue;
						String type = bit.substring(bit.indexOf(subsplitType)+subsplitType.length());
						type = type.substring(0,type.indexOf("</td>")).trim();
						if(type.equals("infinitiv aktiv")) {infinitivAktiv=payload;}
						else if(type.equals("infinitiv passiv"))infinitivPassiv = payload;
						else throw new Error();
						continue;
					}
					if(bit.trim().startsWith("har/hade"))
					{
						String payload = bit.substring(bit.indexOf(PAYLOAD_BEACON_SAOL)+PAYLOAD_BEACON_SAOL.length(), bit.indexOf("</span>")).trim();
						String type = bit.substring(bit.indexOf(subsplitType)+subsplitType.length());
						type = type.substring(0,type.indexOf("</td>")).trim();
						if(type.equals("supinum aktiv"))supinumAktiv=payload;
						else if(type.equals("supinum passiv"))supinumPassiv = payload;
						else throw new Error();
						continue;
					}
					if(bit.trim().startsWith("Presens particip"))
					{
						String payload = bit.substring(bit.indexOf(PAYLOAD_BEACON_SAOL)+PAYLOAD_BEACON_SAOL.length(), bit.indexOf("</span>")).trim();
						presentParticip=payload;
						continue;
					}

					if(bit.trim().startsWith("Perfekt particip")) continue;
					if(bit.trim().startsWith("en"))
					{
						String payload = bit.substring(bit.indexOf(PAYLOAD_BEACON_SAOL)+PAYLOAD_BEACON_SAOL.length(), bit.indexOf("</span>")).trim();
						pastParticipEnn=payload;
						continue;
					}
					if(bit.trim().startsWith("ett"))
					{
						String payload = bit.substring(bit.indexOf(PAYLOAD_BEACON_SAOL)+PAYLOAD_BEACON_SAOL.length(), bit.indexOf("</span>")).trim();
						pastParticipEtt=payload;
						continue;
					}
					if(bit.trim().startsWith("den/det/de"))
					{
						String payload = bit.substring(bit.indexOf(PAYLOAD_BEACON_SAOL)+PAYLOAD_BEACON_SAOL.length(), bit.indexOf("</span>")).trim();
						pastParticipPlur=payload;
						continue;
					}
					if(bit.substring(0,bit.indexOf("<")).trim().equals("den"))
					{
						String payload = bit.substring(bit.indexOf(PAYLOAD_BEACON_SAOL)+PAYLOAD_BEACON_SAOL.length(), bit.indexOf("</span>")).trim();
						perfektParticipDef=payload;
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
					throw new Error();
				}
			}

			return VerbAlternativeForm.newInstance(imperativAktiv,
					infinitivAktiv, presens, preteritumAktiv, supinumAktiv,
					presentParticip,pastParticipEnn,pastParticipEtt,pastParticipPlur,perfektParticipDef,
					presensPassiv, preteritumPassiv, infinitivPassiv, supinumPassiv);			
		}
		if(wt.equals(WordType.N))
			return parseNoun(wholeForm,db);
		if(wt.equals(WordType.PRON))
			return parsePronoun(wholeForm,db);

		if(wt.equals(WordType.ADJ))
		{
			String enn = null;
			String ett = null;
			String defined = null;
			String plural = null;
			String masculinum = null;
			String superlativ = null;
			String superlativDefined = null;
			String komparativ = null;

			if(db.equals(DataBaseEnum.SO))
			{
				if(wholeForm.contains("<span class=\"orto\">"))
				{
					String infinitivStart = wholeForm.substring(wholeForm.indexOf("<span class=\"orto\">"));
					String infinitivEnd = infinitivStart.substring(new String("<span class=\"orto\">").length(), infinitivStart.indexOf("</span>"));
					enn = infinitivEnd.trim();
				}

				for(String split:wholeForm.split("<span class=\"tempmm\">"))
				{
					if(split.contains("<span class=\"orto\">"))continue;
					if(!split.contains("<span class=\"bojning\">"))continue;

					String payloadStart = split.substring(split.indexOf("<span class=\"bojning\">"));
					String payloadEnd = payloadStart.substring(new String("<span class=\"bojning\">").length(), payloadStart.indexOf("</span>"));
					String actualContents = payloadEnd.trim().replaceAll("-", "");

					if(split.contains("maskulinum"))
						masculinum = actualContents;
					else if(split.contains("superlativ"))
						superlativ = actualContents;
					else if(split.trim().startsWith("neutrum"))
					{
						enn = actualContents;
						ett = actualContents;
					}
					else if(split.trim().startsWith("plural"))
					{
						plural = actualContents;
					}
					else if(split.trim().startsWith("används"))continue;
					else if(split.trim().startsWith("komparativ"))komparativ = actualContents;
					else
						throw new Error();
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
					if((header.equals("en")||header.equals("en/ett/den/det/de"))&&mode==DEFAULT
							||(header.equals("är")&& isUnmodifiableAdjective)
							)
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

					if(mode==SUPERLATIV&&header.equals("är"))
					{
						String startSuperlativ = bit.substring(bit.indexOf(startingPattern)+startingPattern.length());
						superlativ = startSuperlativ.substring(0,startSuperlativ.indexOf("</span>")).trim();
						continue;
					}
					throw new Error();
				}


			}

			return AdjectiveRelatedForms.newInstance(enn,ett,plural,defined,masculinum,komparativ, superlativ,superlativDefined);

		}
		throw new Error();
	}


	public static RelatedForms parsePronoun(String wholeForm, DataBaseEnum db) {
		Map<PronounFormEnum, String> res = new HashMap<>();
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
					if(mode==SINGULAR) {res.put(PronounFormEnum.GRUNDFORM_UTRUM_SINGULAR, payload); continue;}
					else if(mode==PLURAL) {res.put(PronounFormEnum.GRUNDFORM_UTRUM_PLURAL, payload); continue;}
				if(typeRelation.equals("grundform neutrum"))
					if(mode==SINGULAR) {res.put(PronounFormEnum.GRUNDFORM_NEUTRUM, payload); continue;}
				if(typeRelation.equals("grundform"))
					if(mode==PLURAL) {res.put(PronounFormEnum.GRUNDFORM_PLURAL, payload); continue;}	
				if(typeRelation.equals("genitiv utrum"))
					if(mode==SINGULAR) {res.put(PronounFormEnum.GENITIV_UTRUM_SINGULAR, payload); continue;}	
				if(typeRelation.equals("objektsform"))
					if(mode==SINGULAR) {res.put(PronounFormEnum.OBJEKTSFORM_SINGULAR, payload); continue;}
					else if(mode==PLURAL) {res.put(PronounFormEnum.OBJEKTSFORM_PLURAL, payload); continue;}
				
				
			//	throw new Error();
			}

		}

		return PronounRelatedForm.newInstance(res);
	}


	public static final String SAOL_PAYLOAD_INDICATOR = "<span class=\"bform\">";
	public static final String SAOL_RELATION_INDICATOR = "<td class=\"ledtext\">";
	public static RelatedForms parseNoun(String wholeForm, DataBaseEnum db) {
		Map<NounFormEnum, String> res = new HashMap<>();

		if(db.equals(DataBaseEnum.SAOL))
		{
			String forms = wholeForm.substring(wholeForm.indexOf(SAOL_FORMS_SPLITTER_TOKEN));

			String startingPattern = "<span class=\"bform\">";

			final int UNDEFINED = 0; final int SINGULAR = 1; final int PLURAL = 2;
			int mode = UNDEFINED;
			
			String wholeHeader = wholeForm.substring(0,wholeForm.indexOf("<")).trim();
			boolean isSpecialAdjective = wholeHeader.equals("adjektiviskt slutled")||wholeHeader.equals("oböjligt substantiv");

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
				String typeRelation = bit.substring(bit.indexOf(SAOL_RELATION_INDICATOR)+ SAOL_RELATION_INDICATOR.length());
				typeRelation = typeRelation.substring(0,typeRelation.indexOf("<")).trim();

				if(typeRelation.equals("obestämd form"))
					if(mode==SINGULAR) {res.put(NounFormEnum.OBESTAMD_FORM_SINGULAR, payload); continue;}
					else if(mode==PLURAL) {res.put(NounFormEnum.OBESTAMD_FORM_PLURAL, payload); continue;}
				if(typeRelation.equals("obestämd form genitiv"))
					if(mode==SINGULAR) {res.put(NounFormEnum.OBESTAMD_FORM_GENITIV_SINGULAR, payload); continue;}
					else if(mode==PLURAL) {res.put(NounFormEnum.OBESTAMD_FORM_GENITIV_PLURAL, payload); continue;}
				if(typeRelation.equals("bestämd form"))
					if(mode==SINGULAR) {res.put(NounFormEnum.BESTAMD_FORM_SINGULAR, payload); continue;}
					else if(mode==PLURAL) {res.put(NounFormEnum.BESTAMD_FORM_PLURAL, payload); continue;}
				if(typeRelation.equals("bestämd form genitiv"))
					if(mode==SINGULAR) {res.put(NounFormEnum.BESTAMD_FORM_GENITIV_SINGULAR, payload); continue;}
					else if(mode==PLURAL) {res.put(NounFormEnum.BESTAMD_FORM_GENITIV_PLURAL, payload); continue;}
				if(typeRelation.equals("i vissa uttryck"))
				{res.put(NounFormEnum.OTHER, payload); continue;}

				if(header.isBlank() && isSpecialAdjective) continue;
				throw new Error();
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
				res.put(NounFormEnum.OBESTAMD_FORM_SINGULAR,payload);
			}

			if(wholeForm.contains("<span class=\"bojning\">"))
			{
				String pretetitumStart = wholeForm.substring(wholeForm.indexOf("<span class=\"bojning\">"));
				String payload = pretetitumStart.substring(new String("<span class=\"bojning\">").length(), pretetitumStart.indexOf("</span>")).trim();
				res.put(NounFormEnum.BESTAMD_FORM_SINGULAR,payload);
			}

			wholeForm = wholeForm.substring(wholeForm.indexOf("<span class=\"bojning\">")+1);
			if(wholeForm.contains("<span class=\"bojning\">"))
			{
				String pretetitumStart = wholeForm.substring(wholeForm.indexOf("<span class=\"bojning\">"));
				String payload = pretetitumStart.substring(new String("<span class=\"bojning\">").length(), pretetitumStart.indexOf("</span>")).trim();
				res.put(NounFormEnum.OBESTAMD_FORM_PLURAL,payload);
			}

			wholeForm = wholeForm.substring(wholeForm.indexOf("<span class=\"bojning\">")+1);
			if(wholeForm.contains("<span class=\"bojning\">"))
			{
				String pretetitumStart = wholeForm.substring(wholeForm.indexOf("<span class=\"bojning\">"));
				String payload = pretetitumStart.substring(new String("<span class=\"bojning\">").length(), pretetitumStart.indexOf("</span>")).trim();
				res.put(NounFormEnum.OBESTAMD_FORM_PLURAL,payload);
			}
		}

		return NounAlternativeForm.newInstance(res);
	}


	public boolean containsForm(LanguageWord lw);

}
