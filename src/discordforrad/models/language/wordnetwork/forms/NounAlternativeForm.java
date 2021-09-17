package discordforrad.models.language.wordnetwork.forms;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import discordforrad.models.language.LanguageWord;

public class NounAlternativeForm implements RelatedForms, Serializable {
	private final GenericRelatedForm<NounFormEnum> gen; 

	public NounAlternativeForm(Map<NounFormEnum, LanguageWord> forms) {
		this.gen = GenericRelatedForm.newInstance(forms);
	}

	public static RelatedForms newInstance(Map<NounFormEnum, LanguageWord> forms) {
		return new NounAlternativeForm(forms);
	}
	
	public String toString() {
		String res = "";
		for(NounFormEnum f: NounFormEnum.values()) {
			if(gen.get(f)==null)res+="XXX";
			else res+= gen.get(f).getWord()+"\t";
			if(f.equals(NounFormEnum.BESTAMD_FORM_PLURAL))res+="\n\t\t";
			}
		return res;
	}

	private String getBesPlu() {
		if(besPlu!=null)return besPlu;
		return "!"+obePlu+"na";
	}

	@Override
	public RelatedForms blendWith(RelatedForms r) {
		if(r instanceof UnmodifiableAlternativeForm)return this;
		else return newInstance(gen.blendWith(((NounAlternativeForm)r).gen).getForms());
	}

	@Override
	public boolean containsForm(LanguageWord lw) {
		return gen.containsForm(lw);
	}

	@Override
	public Set<LanguageWord> getRelatedWords() {
		return gen.getRelatedWords();
	}

	@Override
	public LanguageWord getGrundform() {
		for(NounFormEnum n: NounFormEnum.values())
			if(gen.get(n)!=null)return gen.get(n);
		throw new Error();
	}
	

}
