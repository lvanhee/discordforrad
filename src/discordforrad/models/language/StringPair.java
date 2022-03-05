package discordforrad.models.language;

import org.json.simple.JSONObject;

public class StringPair {
	private final String left;
	private final String right;
	
	private StringPair(String l, String r)
	{
		left = l;
		right = r;
	}
	
	
	public String getLeft() {return left;}
	public String getRight() {return right;}


	public static StringPair newInstance(String left2, String right2) {
		return new StringPair(left2, right2);
	}
	
	public String toString()
	{
		return left+":"+right;
	}


	public static StringPair fromJsonObject(JSONObject x) {
		return new StringPair((String)x.get("left"), (String)x.get("right"));
	}

}
