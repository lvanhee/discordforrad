package discordforrad.models.language.wordnetwork.forms;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import discordforrad.models.language.LanguageWord;

public class GenericRelatedForm<T extends RelatedFormType> implements RelatedForms, Serializable {
	private final Map<T, LanguageWord> forms;
	
	private GenericRelatedForm(Map<T, LanguageWord> forms)
	{
		if(forms.isEmpty())
			throw new Error();
		this.forms = forms;
	}

	@Override
	public GenericRelatedForm<T> blendWith(RelatedForms r) {
		if(r instanceof UnmodifiableAlternativeForm)return this;
		if(!(r instanceof GenericRelatedForm))
				throw new Error();
		
		Map<T, LanguageWord> res = new HashMap<>();
		res.putAll(((GenericRelatedForm)r).forms);
		res.putAll(forms.keySet().stream().filter(x->forms.get(x)!=null).collect(Collectors.toMap(Function.identity(), x->forms.get(x))));
		
				
		return newInstance(res);
	}

	public static <T extends RelatedFormType> GenericRelatedForm<T> newInstance(Map<T, LanguageWord> res) {
		return new GenericRelatedForm<>(res);
	}

	public LanguageWord get(T f) {
		return forms.get(f);
	}
	
	public String toString()
	{
		return forms.toString();
	}

	public Map<T, LanguageWord> getForms() {
		return forms;
	}

	@Override
	public boolean containsForm(LanguageWord lw) {
		return forms.values().stream().filter(x->x!=null).anyMatch(x->x.equals(lw));
	}

	@Override
	public Set<LanguageWord> getRelatedWords() {
		return forms.values().stream().filter(x->x!=null).collect(Collectors.toSet());
	}

}
