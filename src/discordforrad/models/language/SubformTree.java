package discordforrad.models.language;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import discordforrad.models.LanguageCode;
import discordforrad.models.language.wordnetwork.forms.RelatedFormsNetwork;

public class SubformTree implements Serializable {

	private final Set<LanguageWord> subwords;
	
	public SubformTree(LanguageWord lw) {
		if(lw.getCode().equals(LanguageCode.EN))
		{
			subwords = new HashSet<>();
			return;
		}
		
		subwords = getSubwords(lw);	
		
	}

	private static Set<LanguageWord> getSubwords(LanguageWord lw) {
		String whole = lw.getWord();
		Set<LanguageWord> subwords = new HashSet<>();
		while(whole.length()>3)
		{
			whole = whole.substring(1).trim();
			if(whole.startsWith("-")) whole = whole.substring(1);
			if(Dictionnary.isInPrecomputedDictionnaries(LanguageWord.newInstance(whole, lw.getCode())))
				subwords.add(LanguageWord.newInstance(whole, lw.getCode()));
		}
		
		whole = lw.getWord();
		while(whole.length()>3)
		{
			whole = whole.substring(0, whole.length()-1).trim();
			if(Dictionnary.isInPrecomputedDictionnaries(LanguageWord.newInstance(whole, lw.getCode())))
				subwords.add(LanguageWord.newInstance(whole, lw.getCode()));
		}
		
		Set<LanguageWord> grundforms = WordDescription.getGrundforms(lw);
		Set<LanguageWord> grundFormsToRemove = new HashSet<>();
		for(LanguageWord lw2:grundforms)
			for(LanguageWord lw3:grundforms)
				if(
						lw2.getWord().contains(lw3.getWord())&&
						!lw2.getWord().equals(lw3.getWord()))
					grundFormsToRemove.add(lw2);
		grundforms.removeAll(grundFormsToRemove);
		grundforms.removeAll(grundforms.stream()
				.filter(x->
				x.getWord().length()>=lw.getWord().length()).collect(Collectors.toSet()));

		grundforms.remove(lw);
		
			
	
		for(LanguageWord grund : grundforms)
			subwords.addAll(getSubwords(grund));
		return subwords;
	}

	public static SubformTree newInstance(LanguageWord lw) {
		return new SubformTree(lw);
	}
	
	public String toString()
	{
		return subwords.toString();
	}

	public Set<LanguageWord> getWords() {
		return subwords;
	}

	public JSONObject toJsonObject() {
		JSONObject res = new JSONObject();
		
		JSONArray arr = new JSONArray();
		arr.addAll(subwords.stream().map(x->x.toJsonObject()).collect(Collectors.toList()));
		res.put("subwords",arr);
		return res;
	}
	
	public int hashCode() {return subwords.hashCode();}
	public boolean equals(Object o)
	{
		return ((SubformTree)o).subwords.equals(subwords);
	}


}
