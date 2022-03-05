package discordforrad.models.language.wordnetwork.forms;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.json.simple.JSONObject;

import discordforrad.inputUtils.TextInputUtils;
import discordforrad.models.LanguageCode;
import discordforrad.models.language.LanguageWord;

public class GenericRelatedForm<T extends RelatedFormType> implements RelatedForms, Serializable {
	private final Map<T, LanguageWord> forms;
	
	private GenericRelatedForm(Map<T, LanguageWord> forms)
	{
		if(forms.isEmpty())
			throw new Error();
		this.forms = forms.keySet().stream().filter(x->forms.get(x)!=null).collect(Collectors.toMap(Function.identity(), x->forms.get(x)));
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

	@Override
	public LanguageWord getGrundform() {
		throw new Error();
	}

	@Override
	public String toParsableString() {
		return forms.toString();
	}

	public static<T extends RelatedFormType> GenericRelatedForm<T> parse(Function<String, T> formsParser, String string) {
		Map<T, LanguageWord> res = new HashMap<>();
		
		Map<T, LanguageWord> m = 
				Arrays.asList(string.substring(0,string.length()-1).split(","))
				.stream()
				.filter(x->{
					String consideredString = x.substring(x.indexOf('=')+1);
					return !(consideredString.equals("null"));})
				.collect(Collectors.toMap(
						x->{
							String leftSide = x.substring(1,x.indexOf('='));
							return formsParser.apply(leftSide); 
						},
						x->{
							String consideredString = x.substring(x.indexOf('=')+1);
							return LanguageWord.parse(consideredString);}
						));
		
		return GenericRelatedForm.newInstance(m);
				
	}
	public boolean equals(Object o) {
		if(!(o instanceof GenericRelatedForm))return false;
		return ((GenericRelatedForm)o).forms.equals(forms);
		}
	
	public int hashCode() {return forms.hashCode();}

	@Override
	public JSONObject toJsonObject() {
		JSONObject res = new JSONObject();
		for(T t: forms.keySet())
			if(forms.get(t)!=null)
				res.put(t.toString(), forms.get(t).toJsonObject());
		
		return res;
	}

	public static<T extends RelatedFormType> GenericRelatedForm<T> fromJsonObject(Function<String, T> formsParser, JSONObject object) {		
		Map<T, LanguageWord> m = new HashMap<>();
		for(Object o: object.keySet())
		{
			if(o.equals("language_word"))
				throw new Error();
			T parsed = formsParser.apply((String)o); 
			m.put(parsed, LanguageWord.fromJsonObject((JSONObject)object.get(o)));
		}
		
		return GenericRelatedForm.newInstance(m);
	}

}
