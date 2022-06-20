package discordforrad.translation;

public class NoTranslationForTheRequest implements ResultOfTranslationAttempt{
	private final Origin o;

	public NoTranslationForTheRequest(Origin o) {
		this.o = o;
	}

	@Override
	public Origin getOriginOfTranslation() {
		return o;
	}

	@Override
	public String toParsableString() {
		return this.o+":NO_TRANSLATION_IN_DB";
	}

	public static NoTranslationForTheRequest newInstance(Origin wordReference) {
		return new NoTranslationForTheRequest(wordReference);
	}
	
	public String toString() {return toParsableString();}
}
