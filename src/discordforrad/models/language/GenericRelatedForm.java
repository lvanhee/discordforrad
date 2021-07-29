package discordforrad.models.language;

import java.util.HashMap;
import java.util.Map;

import discordforrad.models.language.RelatedForms.NounFormEnum;

public class GenericRelatedForm<T extends RelatedFormType> implements RelatedForms {
	private final Map<T, String> forms;
	
	private GenericRelatedForm(Map<T, String> forms)
	{
		this.forms = forms;
	}

	@Override
	public GenericRelatedForm<T> blendWith(RelatedForms r) {
		if(r instanceof UnmodifiableAlternativeForm)return this;
		if(!(r instanceof GenericRelatedForm))
				throw new Error();
		
		Map<T, String> res = new HashMap<>();
		res.putAll(forms);
		res.putAll(((GenericRelatedForm)r).forms);
				
		return newInstance(res);
	}

	public static <T extends RelatedFormType> GenericRelatedForm<T> newInstance(Map<T, String> res) {
		return new GenericRelatedForm<>(res);
	}

	public String get(T f) {
		return forms.get(f);
	}
	
	public String toString()
	{
		return forms.toString();
	}

	public Map<T, String> getForms() {
		return forms;
	}

	@Override
	public boolean containsForm(LanguageWord lw) {
		return forms.values().stream().filter(x->x!=null).anyMatch(x->x.equals(lw.getWord()));
	}

}
