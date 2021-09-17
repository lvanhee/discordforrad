package discordforrad.translation;

import java.io.Serializable;

import discordforrad.models.language.TranslationDescription;
import discordforrad.models.language.TranslationDescription.Origin;

public class TranslationOutcomeFailure implements TranslationDescription, Serializable {
	private final Origin o;

	public TranslationOutcomeFailure(Origin o2) {
		this.o = o2;
	}

	public static TranslationDescription newInstance(Origin o) {
		return new TranslationOutcomeFailure(o);
	}

	@Override
	public Origin getOriginOfTranslation() {
		return o;
	}
}
