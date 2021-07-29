package discordforrad.models.language;

import java.util.HashMap;
import java.util.Map;

public class AdjectiveRelatedForms implements RelatedForms {
	
	private final GenericRelatedForm<AdjectiveFormsEnum> form;
	
	public AdjectiveRelatedForms(Map<AdjectiveFormsEnum, String>f) {
		form = GenericRelatedForm.newInstance(f);
	}

	public static RelatedForms newInstance(String enn, String ett, String plural, String defined,String masculinum,
			String komparativ, String superlativ, String superlativDefined) {
		Map<AdjectiveFormsEnum, String> res = new HashMap<>();
		res.put(AdjectiveFormsEnum.ENN, enn);
		res.put(AdjectiveFormsEnum.ETT, ett);
		res.put(AdjectiveFormsEnum.PLURAL, plural);
		res.put(AdjectiveFormsEnum.DEFINED, defined);
		res.put(AdjectiveFormsEnum.MASCULINUM, masculinum);
		res.put(AdjectiveFormsEnum.COMPARATIVE, komparativ);
		res.put(AdjectiveFormsEnum.SUPERLATIVE, superlativ);
		res.put(AdjectiveFormsEnum.SUPERLATIVE_DEFINED, superlativDefined);
		return new AdjectiveRelatedForms(res);
	}
	
	public String toString()
	{
		String res = "";
		for(AdjectiveFormsEnum a:AdjectiveFormsEnum.values())
		{
			res+=form.get(a)+"\t";
			if(a.equals(AdjectiveFormsEnum.DEFINED))res+="\n\t\t";
		}
		return res;
	}

	private String getPlural() {
		if(plural!=null) return plural;
		if(getEnn().endsWith("a"))return "!"+getEnn();
		return "!"+getEnn()+"a";
	}

	private String getEnn() {
		return enn;
	}

	@Override
	public RelatedForms blendWith(RelatedForms r) {
		return AdjectiveRelatedForms.newInstance(((AdjectiveRelatedForms)r).form.blendWith(form).getForms());			
	}

	private static RelatedForms newInstance(Map<AdjectiveFormsEnum, String> forms) {
		return new AdjectiveRelatedForms(forms);
	}

	@Override
	public boolean containsForm(LanguageWord lw) {
		return form.containsForm(lw);
	}

}
