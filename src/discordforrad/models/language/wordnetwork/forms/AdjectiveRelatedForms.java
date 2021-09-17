package discordforrad.models.language.wordnetwork.forms;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import discordforrad.models.language.LanguageWord;

public class AdjectiveRelatedForms implements RelatedForms, Serializable {
	
	private final GenericRelatedForm<AdjectiveFormsEnum> form;
	
	public AdjectiveRelatedForms(Map<AdjectiveFormsEnum, LanguageWord>f) {
		form = GenericRelatedForm.newInstance(f);
	}

	public static RelatedForms newInstance(LanguageWord enn, LanguageWord ett, LanguageWord plural, LanguageWord defined,
			LanguageWord masculinum,
			LanguageWord komparativ, LanguageWord superlativ, LanguageWord superlativDefined) {
		Map<AdjectiveFormsEnum, LanguageWord> res = new HashMap<>();
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
			if(form.get(a)==null)
				res+="XXX\t";
			else res+=form.get(a).getWord()+"\t";
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
		if(r instanceof UnmodifiableAlternativeForm)return this;
		if(!(r instanceof AdjectiveRelatedForms))
			throw new Error();
		GenericRelatedForm<AdjectiveFormsEnum>otherForms =((AdjectiveRelatedForms)r).form;
		GenericRelatedForm<AdjectiveFormsEnum>blended = otherForms.blendWith(form);
		return AdjectiveRelatedForms.newInstance(blended.getForms());
	}

	private static RelatedForms newInstance(Map<AdjectiveFormsEnum, LanguageWord> forms) {
		return new AdjectiveRelatedForms(forms);
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
		for(AdjectiveFormsEnum f: AdjectiveFormsEnum.values())
			if(form.getForms().containsKey(f) && form.get(f)!=null)
				return form.getForms().get(f);

		throw new Error();
	}

}
