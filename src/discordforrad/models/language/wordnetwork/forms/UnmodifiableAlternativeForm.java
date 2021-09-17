package discordforrad.models.language.wordnetwork.forms;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import discordforrad.models.language.LanguageWord;

public class UnmodifiableAlternativeForm implements RelatedForms, Serializable {

	private final LanguageWord lw;
	
	public UnmodifiableAlternativeForm(LanguageWord lw)
	{
		this.lw = lw;
	}
	public String toString() {return "unmodifiable";}

	@Override
	public RelatedForms blendWith(RelatedForms r) {
		return r;
	}

	@Override
	public boolean containsForm(LanguageWord lw) {
		return true;
	}

	@Override
	public Set<LanguageWord> getRelatedWords() {
		return Arrays.asList(lw).stream().collect(Collectors.toSet());
	}
	public static RelatedForms newInstance(LanguageWord lw2) {
		return new UnmodifiableAlternativeForm(lw2);
	}
	@Override
	public LanguageWord getGrundform() {
		return lw;
	}
}
