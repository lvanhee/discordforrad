package discordforrad.models.language.wordnetwork.forms;

import java.util.Map;

import discordforrad.models.language.WordDescription.WordType;

public interface RelatedFormsTransition {

	Map<WordType, RelatedForms> getForms();

}
