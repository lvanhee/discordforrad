package discordforrad.models.language;

import java.util.Map;

import discordforrad.models.language.RelatedForms.NounFormEnum;

public class PronounRelatedForm implements RelatedForms {

	private final GenericRelatedForm<PronounFormEnum> form;
	public PronounRelatedForm(Map<PronounFormEnum, String> res) {
		this.form = GenericRelatedForm.newInstance(res);
	}

	static RelatedForms newInstance(Map<PronounFormEnum, String> res) {
		return new PronounRelatedForm(res);
	}

	@Override
	public RelatedForms blendWith(RelatedForms r) {
		return ((PronounRelatedForm)r).form.blendWith(form);
	}
	
	public String toString() {
		String res = "";
		for(PronounFormEnum f: PronounFormEnum.values()) {
			res+= form.get(f)+"\t";
			}
		return res;
	}

	@Override
	public boolean containsForm(LanguageWord lw) {
		return form.containsForm(lw);
	}

}
