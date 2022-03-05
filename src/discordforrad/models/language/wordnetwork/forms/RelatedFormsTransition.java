package discordforrad.models.language.wordnetwork.forms;

import java.util.Map;

import org.json.simple.JSONObject;

import discordforrad.models.language.WordDescription.WordType;

public interface RelatedFormsTransition {

	Map<WordType, RelatedForms> getForms();

	String toParsableString();
	
	JSONObject toJsonObject();

	public static RelatedFormsTransition fromJsonObject(JSONObject jsonObject) {
		return RelatedFormsTransitionImpl.fromJsonObject(jsonObject);
	}
}
