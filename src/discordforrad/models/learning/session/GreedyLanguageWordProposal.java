package discordforrad.models.learning.session;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import discordforrad.inputUtils.UserLearningTextManager;
import discordforrad.models.language.LanguageCode;
import discordforrad.models.language.LanguageWord;
import discordforrad.models.language.WordDescription;
import discordforrad.models.learning.VocabularyLearningStatus;
import discordforrad.models.learning.focus.FocusManager;
import discordforrad.models.learning.focus.ReadThroughFocus;


public class GreedyLanguageWordProposal implements LanguageWordProposal{

	private final static int TOTAL_NUMBER_OF_NEW_WORDS_EXPOSED_EVERY_SESSION=20; 


	private final Set<LanguageWord> proposal  = new HashSet<>();

	public static GreedyLanguageWordProposal newInstance() {
		return new GreedyLanguageWordProposal();
	}

	public Set<LanguageWord> getProposal() {
		return proposal;
	}

	public void addForConsideration(LanguageWord lw, Set<LanguageWord> mandatoryWords, VocabularyLearningStatus vls, ReadThroughFocus currentFocus2) {
		proposal.addAll(mandatoryWords);
		if(proposal.contains(lw))return;
		if(proposal.size()<TOTAL_NUMBER_OF_NEW_WORDS_EXPOSED_EVERY_SESSION)
		{
			proposal.add(lw);
			return;
		}

		getOptimalSwapWith(lw, mandatoryWords, vls, currentFocus2);

	}

	private void getOptimalSwapWith(LanguageWord lw, Set<LanguageWord> mandatoryWords, VocabularyLearningStatus vls, ReadThroughFocus currentFocus2) {

		LanguageWord toBeRemoved = null;
		Set<LanguageWord> nextProposal = new HashSet<>();
		nextProposal.addAll(mandatoryWords);
		if(mandatoryWords.size()>=TOTAL_NUMBER_OF_NEW_WORDS_EXPOSED_EVERY_SESSION)
		{
			proposal.clear();
			proposal.addAll(nextProposal);
			return;
		}
		nextProposal.addAll(proposal);
		
		for(LanguageWord wordInProposalToReplace: proposal)
		{
			if(mandatoryWords.contains(wordInProposalToReplace))
				continue;
			if(nextProposal.contains(lw))
				continue;
			Set<LanguageWord> alternativeProposal = new HashSet<>();
			alternativeProposal.addAll(nextProposal);
			alternativeProposal.remove(wordInProposalToReplace);
			alternativeProposal.add(lw);
			if(alternativeProposal.size()!=20)
				throw new Error();

			if(isBetter(alternativeProposal,nextProposal, vls, currentFocus2))
				nextProposal = alternativeProposal;
		}
		proposal.clear();
		proposal.addAll(nextProposal);

	}

	private boolean isBetter(Set<LanguageWord> p1, Set<LanguageWord> p2, VocabularyLearningStatus vls, ReadThroughFocus focus) {
		WordSelectionAssessment dp1 = WordSelectionAssessment.newInstance(p1, vls, focus);
		WordSelectionAssessment dp2 = WordSelectionAssessment.newInstance(p2, vls, focus);

		double scoreTheProposalWordsThatAreMoreFrequentInTheText = 
				dp1.getFrequencyPerWordOrTranslationInTheSelectionInTextDatabase()/(
						dp1.getFrequencyPerWordOrTranslationInTheSelectionInTextDatabase()+
						dp2.getFrequencyPerWordOrTranslationInTheSelectionInTextDatabase())-0.5;
				
		double scoreProgressOnCurrentFocus = dp1.getNumberOfOccurrenceOfWordsInTheSelectionInFocusText() 
				- dp2.getNumberOfOccurrenceOfWordsInTheSelectionInFocusText();
				
		double scoreAdditionOfGrundforms = dp1.getProportionOfGrundForms() -
				dp2.getProportionOfGrundForms();
		
		double proximityClosenessToQuotas = dp1.getProximityToQuotas()-dp2.getProximityToQuotas();
		
		double priorityProximityClosenessToQuotas = 1;
		if(dp1.getProximityToQuotas()<0.5)
			priorityProximityClosenessToQuotas*=2;

		return 
				(
						proximityClosenessToQuotas * priorityProximityClosenessToQuotas+
						scoreTheProposalWordsThatAreMoreFrequentInTheText+ 
						scoreProgressOnCurrentFocus+
						scoreAdditionOfGrundforms*0.1
						>0);

		//	throw new Error();
	}
}
