package discordforrad.models.language.wordnetwork.forms;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import discordforrad.models.language.LanguageWord;

public class PronounRelatedForm implements RelatedForms, Serializable {

	private final GenericRelatedForm<PronounFormEnum> form;
	public PronounRelatedForm(Map<PronounFormEnum, LanguageWord> res) {
		this.form = GenericRelatedForm.newInstance(res);
	}

	static RelatedForms newInstance(Map<PronounFormEnum, LanguageWord> res) {
		return new PronounRelatedForm(res);
	}

	@Override
	public RelatedForms blendWith(RelatedForms r) {
		GenericRelatedForm<PronounFormEnum> blended= ((PronounRelatedForm)r).form.blendWith(form);
		return newInstance(blended.getForms());
	}
	
	public String toString() {
		String res = "";
		for(PronounFormEnum f: PronounFormEnum.values()) {
			if(form.get(f)==null) res+="XXX\t";
			else res+= form.get(f).getWord()+"\t";
			}
		return res;
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
		return form.getForms()
				.values()
				.stream()
				.min((x,y)->{
					if(x.getWord().length()<y.getWord().length())
						return -1;
					if(x.getWord().length()>y.getWord().length())
						return 1;
					return x.getWord().compareTo(y.getWord());
						}
				).get();
	}

	@Override
	public String toParsableString() {
		return form.toParsableString();
	}
	public boolean equals(Object o) {throw new Error();}
	public int hashCode() {return form.hashCode();}

}
