package discordforrad.models.language;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class LanguageWord implements Serializable{
	private final LanguageCode lc;
	private final String word;
	
	public LanguageWord(LanguageCode lc, String word)
	{
		/*if(word.contains("!"))
			throw new Error();*/
		if(word.contains("\\"))
			throw new Error();
		/*if(word.contains("?"))
			throw new Error();*/
		if(!word.trim().equals(word))
			throw new Error();
		if(word.startsWith("-"))
			throw new Error();
		if(word.contains(":"))
			throw new Error();
		if(word.contains("&#39;"))
			throw new Error();
		if(word.isBlank())
			throw new Error();
		this.lc = lc;
		this.word = word.toLowerCase();
	}
	
	public int hashCode()
	{
		return lc.hashCode()*word.hashCode();
	}
	
	public boolean equals(Object o)
	{
		LanguageWord lw = (LanguageWord) o;
		return lw.lc.equals(lc) && lw.word.equals(word);
	}

	public String getWord() {
		return word;
	}

	public LanguageCode getCode() {
		return lc;
	}
	
	public String toString()
	{
		return lc+":"+word;
	}

	private static final Map<LanguageCode,Map<String, LanguageWord>> cache = new HashMap<>();
	public static synchronized LanguageWord newInstance(String s, LanguageCode languageCode) {
		s = s.replaceAll("-", "").trim().toLowerCase();
		synchronized (cache) {


			if(cache.containsKey(languageCode)&&cache.get(languageCode).containsKey(s))
				return cache.get(languageCode).get(s);
		}
		return new LanguageWord(languageCode, s);
	}

	public static Set<LanguageWord> toLanguageWordSet(Set<String> strings) {
		Set<LanguageWord>res = new HashSet<LanguageWord>();
		for(LanguageCode lc: LanguageCode.values())
			res.addAll(strings.stream().map(x->LanguageWord.newInstance(x, lc)).collect(Collectors.toSet()));
		return res;
	}
	
	public static LanguageWord parse(String s)
	{
		if(!s.contains(":"))
			throw new Error();
		String head = s.substring(0,s.indexOf(":"));
		String end = s.substring(s.indexOf(":")+1);
		return newInstance(end, LanguageCode.valueOf(head));
	}

	public JSONObject toJsonObject() {
		JSONObject res = new JSONObject();
		res.put("language_code", lc.toJsonObject());
		res.put("word", word);
		
		assert(this.equals(fromJsonObject(res)));
		//System.out.println(fromJsonObject(res.toJSONString()));
		return res;
	}

	public static LanguageWord fromJsonObject(JSONObject o) {
			

			//return LanguageWord.new
			JSONObject lc = (JSONObject) o.get("language_code");
			return LanguageWord.newInstance((String)o.get("word"), LanguageCode.fromJsonObject(lc));		
	}

}
