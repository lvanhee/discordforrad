package discordforrad.translation;

import java.io.Serializable;

import discordforrad.models.language.ResultOfTranslationAttempt;
import discordforrad.models.language.ResultOfTranslationAttempt.Origin;

public class TranslationOutcomeFailure implements ResultOfTranslationAttempt, Serializable {
	private final Origin o;

	public TranslationOutcomeFailure(Origin o2) {
		this.o = o2;
	}

	public static ResultOfTranslationAttempt newInstance(Origin o) {
		return new TranslationOutcomeFailure(o);
	}

	@Override
	public Origin getOriginOfTranslation() {
		return o;
	}

	@Override
	public String toParsableString() {
		return this.o+":FAILED_TO_TRANSLATE";
	}
	
	public String toString()
	{
		return "TranslationFailed "+o;
	}
	
	public boolean equals(Object o)
	{
		return ((TranslationOutcomeFailure)o).o.equals(this.o);
	}
}
