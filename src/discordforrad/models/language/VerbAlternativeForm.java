package discordforrad.models.language;

import java.util.HashMap;
import java.util.Map;


public class VerbAlternativeForm implements RelatedForms {

	private final GenericRelatedForm<VerbAlternativeFormEnum> form;

	
	public VerbAlternativeForm (Map<VerbAlternativeFormEnum, String>m)
	{
		form = GenericRelatedForm.newInstance(m);
	}
	
	
	public static VerbAlternativeForm newInstance(String imperativ, String infinitiv, String presens, String past,
			String pastPerfekt, String presentParticip, String pastParticipEnn, String pastParticipEtt, String pastParticipPlur, String perfektParticipDef,
			String presensPassiv, String preteritumPassiv, String infinitivPassiv, String supinumPassiv) {
		
		Map<VerbAlternativeFormEnum, String> m = new HashMap<>();
		m.put(VerbAlternativeFormEnum.IMPERATIV,imperativ);
		m.put(VerbAlternativeFormEnum.INFINITIV,infinitiv);
		m.put(VerbAlternativeFormEnum.PRESENS,presens);
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
		
		
		return new VerbAlternativeForm(m);
	}
	
	public String toString() {
		String res = "";
		for(VerbAlternativeFormEnum f: VerbAlternativeFormEnum.values()) {
			res+= form.get(f)+"\t";
			if(f.equals(VerbAlternativeFormEnum.SUPINUM))res+="\n\t\t";
			if(f.equals(VerbAlternativeFormEnum.PERFEKT_PARTICIP_DEF))res+="\n\t\t";
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

}
