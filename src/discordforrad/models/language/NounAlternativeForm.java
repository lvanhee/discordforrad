package discordforrad.models.language;

import java.util.HashMap;
import java.util.Map;

public class NounAlternativeForm implements RelatedForms {
	private final GenericRelatedForm<NounFormEnum> gen; 

	public NounAlternativeForm(Map<NounFormEnum, String> forms) {
		this.gen = GenericRelatedForm.newInstance(forms);
	}

	public static RelatedForms newInstance(Map<NounFormEnum, String> forms) {
		return new NounAlternativeForm(forms);
	}
	
	public String toString() {
		String res = "";
		for(NounFormEnum f: NounFormEnum.values()) {
			res+= gen.get(f)+"\t";
			if(f.equals(NounFormEnum.OBESTAMD_FORM_GENITIV_SINGULAR))res+="\n";
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
	

}
