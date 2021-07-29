package discordforrad.models.language;

public enum UnmodifiableAlternativeForm implements RelatedForms {INSTANCE;

	public String toString() {return "unmodifiable";}

	@Override
	public RelatedForms blendWith(RelatedForms r) {
		if(!r.equals(INSTANCE))
			return r;
		return INSTANCE;
	}

	@Override
	public boolean containsForm(LanguageWord lw) {
		return true;
	}
}
