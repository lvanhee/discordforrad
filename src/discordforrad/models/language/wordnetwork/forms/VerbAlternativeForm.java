package discordforrad.models.language.wordnetwork.forms;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import discordforrad.models.language.LanguageWord;


public class VerbAlternativeForm implements RelatedForms, Serializable {

	private final GenericRelatedForm<VerbAlternativeFormEnum> form;

	
	public VerbAlternativeForm (Map<VerbAlternativeFormEnum, LanguageWord>m)
	{
		form = GenericRelatedForm.newInstance(m);
	}
	
	
	public static VerbAlternativeForm newInstance(
			LanguageWord imperativ, LanguageWord infinitiv, LanguageWord presens, LanguageWord presensKonjunktiv,
			LanguageWord past, LanguageWord preteritumKunjunktiv,
			LanguageWord pastPerfekt, LanguageWord presentParticip, LanguageWord pastParticipEnn, LanguageWord pastParticipEtt, LanguageWord pastParticipPlur, LanguageWord perfektParticipDef,
			LanguageWord presensPassiv, LanguageWord preteritumPassiv, LanguageWord infinitivPassiv, LanguageWord supinumPassiv) {
		
		Map<VerbAlternativeFormEnum, LanguageWord> m = new HashMap<>();
		m.put(VerbAlternativeFormEnum.IMPERATIV,imperativ);
		m.put(VerbAlternativeFormEnum.INFINITIV,infinitiv);
		m.put(VerbAlternativeFormEnum.PRESENS,presens);
		m.put(VerbAlternativeFormEnum.PRESENS_KUNJUNKTIV,presensKonjunktiv);
		m.put(VerbAlternativeFormEnum.PRETERITUM,past);
		m.put(VerbAlternativeFormEnum.SUPINUM,pastPerfekt);
		m.put(VerbAlternativeFormEnum.PRESENS_PARTICIP,presentParticip);
		m.put(VerbAlternativeFormEnum.PAST_PARTICIP_ENN,pastParticipEnn);
		m.put(VerbAlternativeFormEnum.PAST_PARTICIP_ETT,pastParticipEtt);
		m.put(VerbAlternativeFormEnum.PAST_PARTICIP_PLUR,pastParticipPlur);
		m.put(VerbAlternativeFormEnum.PERFEKT_PARTICIP_DEF,perfektParticipDef);
		m.put(VerbAlternativeFormEnum.INFINITIV_PASSIV,infinitivPassiv);
		m.put(VerbAlternativeFormEnum.PRESENS_PASSIV,presensPassiv);
		m.put(VerbAlternativeFormEnum.PRETERITUM_PASSIV,preteritumPassiv);
		m.put(VerbAlternativeFormEnum.SUPINUM_PASSIV,supinumPassiv);
		m.put(VerbAlternativeFormEnum.PRETERITUM_KUNJUNKTIV,presensKonjunktiv);	
		
		return new VerbAlternativeForm(m);
	}
	
	public String toString() {
		String res = "";
		for(VerbAlternativeFormEnum f: VerbAlternativeFormEnum.values()) {
			if(form.get(f)==null)res+="XXX\t";
			else res+= form.get(f).getWord()+"\t";
			if(f.equals(VerbAlternativeFormEnum.SUPINUM))res+="\n\t\t";
			if(f.equals(VerbAlternativeFormEnum.PERFEKT_PARTICIP_DEF))res+="\n\t\t";
			if(f.equals(VerbAlternativeFormEnum.PRESENS_KUNJUNKTIV))res+="(presens kunjunktiv)";
			if(f.equals(VerbAlternativeFormEnum.PRETERITUM_KUNJUNKTIV))res+="(preteritum kunjunktiv)";
			}
		return res;
	}


	private String getImperativ() {
		if(imperativ!=null) return imperativ;
		if(getPresens()!=null)
		{
			String p = getPresens();
			if(p.endsWith("er"))
				return "!"+p.substring(0,p.length()-2);
			else return "!"+p.substring(0,p.length()-1);
		}
		else return null;
	}


	private String getPresens() {
		if(presens!=null)
			return presens;
		return "!"+getInfinitiv()+"r";
	}


	private String getInfinitiv() {
		return infinitiv;
	}


	@Override
	public RelatedForms blendWith(RelatedForms r) {
		if(r instanceof UnmodifiableAlternativeForm)return this;
		else return new VerbAlternativeForm(form.blendWith(((VerbAlternativeForm)r).form).getForms());
	}


	@Override
	public boolean containsForm(LanguageWord lw) {
		return form.containsForm(lw);
	}


	@Override
	public Set<LanguageWord> getRelatedWords() {
		return form.getRelatedWords();
	}


	@Override
	public LanguageWord getGrundform() {
		return form.get(VerbAlternativeFormEnum.INFINITIV);
	}
}
