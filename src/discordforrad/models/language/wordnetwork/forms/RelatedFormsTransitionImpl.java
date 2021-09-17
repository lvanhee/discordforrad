package discordforrad.models.language.wordnetwork.forms;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import discordforrad.models.language.WordDescription.WordType;

public class RelatedFormsTransitionImpl implements RelatedFormsTransition, Serializable{
	
	private final Map<WordType, RelatedForms> af;

	public RelatedFormsTransitionImpl(Map<WordType, RelatedForms> map) {
		this.af = map;
	}

	public static RelatedFormsTransition newInstance() {
		return new RelatedFormsTransitionImpl(new HashMap<>());
	}
	
	public static RelatedFormsTransitionImpl mergeWithBase(RelatedFormsTransition transition, WordType wt, RelatedForms form) {
		Map<WordType, RelatedForms> merged = new HashMap<>();
		merged.putAll(transition.getForms());
		if(merged.containsKey(wt)&&!merged.get(wt).equals(form)) 
			merged.put(wt, merged.get(wt).blendWith(form));
		else merged.put(wt,form);
		
		return new RelatedFormsTransitionImpl(merged);
	}

	@Override
	public Map<WordType, RelatedForms> getForms() {
		return af;
	}

	public static RelatedFormsTransition mergeWithBase(RelatedFormsTransition net,RelatedFormsTransition formForBase) {
		Map<WordType, RelatedForms> m = formForBase.getForms();
		RelatedFormsTransition res = net;
		for(WordType wt:m.keySet())
			res = RelatedFormsTransitionImpl.mergeWithBase(res, wt, m.get(wt));
		
		return res;			
	}
	
	public String toString() {
		return af.toString();}
	
	public int hashCode() { return af.hashCode();}
	
	public boolean equals(Object o) { return (((RelatedFormsTransition)o).getForms()).equals(getForms());}
}
