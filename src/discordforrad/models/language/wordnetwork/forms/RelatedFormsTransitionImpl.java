package discordforrad.models.language.wordnetwork.forms;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;

import discordforrad.inputUtils.TextInputUtils;
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
	
	public boolean equals(Object o) { 
		return (((RelatedFormsTransition)o).getForms()).equals(getForms());
		}
	
	public String toParsableString() {
		if(af.isEmpty())return "{}";
		String res = af.keySet().stream().map(x->"{"+x.toParsableString()+"="+af.get(x).toParsableString()+"}")
				.reduce((x,y)->x+","+y).get();
		
		if(res.contains("::"))
			throw new Error();
		return res;
	}
	
	public static RelatedFormsTransition parse(String s)
	{
		Map<WordType, RelatedForms> res = new HashMap<>();
		if(s.equals("{}"))
			return RelatedFormsTransitionImpl.newInstance();
		
		
		for(String bit : TextInputUtils.splitAlong(s, '{','}'))
		{
			String left = bit.substring(0,bit.indexOf('='));
			String right = bit.substring(bit.indexOf('=')+1);
			WordType wt = WordType.parse(left);
			RelatedForms rf = RelatedForms.parse(wt,right);
			res.put(wt, rf);
		}
		
		return new RelatedFormsTransitionImpl(res);
	}

	public static RelatedFormsTransition fromJsonString(String s) {
		throw new Error();
	}

	@Override
	public JSONObject toJsonObject() {
		JSONObject res = new JSONObject();
		for(WordType wt:af.keySet())
			res.put(wt.toString(), af.get(wt).toJsonObject());
		if(!fromJsonObject(res).equals(this))
			throw new Error();
		return res;
	}

	public static RelatedFormsTransitionImpl fromJsonObject(JSONObject jsonObject) {
		Map<WordType, RelatedForms> res = new HashMap<>();
		for(Object o: jsonObject.keySet())
		{
			WordType wt = WordType.valueOf((String)o); 
			res.put(
					wt,
					RelatedForms.fromJsonObject(wt,(JSONObject)jsonObject.get(o)));
					
		}
		return new RelatedFormsTransitionImpl(res);
	}
}
