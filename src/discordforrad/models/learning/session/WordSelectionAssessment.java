package discordforrad.models.learning.session;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import discordforrad.inputUtils.UserLearningTextManager;
import discordforrad.models.LanguageCode;
import discordforrad.models.VocabularyLearningStatus;
import discordforrad.models.language.LanguageWord;
import discordforrad.models.language.WordDescription;
import discordforrad.models.learning.focus.ReadThroughFocus;
import discordforrad.translation.Translator;

public class WordSelectionAssessment {

	private static final double TOTAL_NUMBER_OF_NEW_WORDS_EXPOSED_EVERY_SESSION = 20;
	private final Set<LanguageWord> selection;
	private final double averageFrequencyOfTheWordsOrTheirTranslationsInTheSelectionInTheTextDatabase;
	private final double ratioWordsInSelectionInFocusText;
	private final double proportionGrundforms;
	private final Map<LanguageCode, Double> priorityRaise; 
	private final double  complianceToLanguageDistributionQuotas;
	private final Set<LanguageWord> wordsInFocusText;
	
	private final Map<LanguageWord, Integer> numberOfDirectOccurrencesInText;
	private final Map<LanguageWord, Integer> numberOfTranslationsInText;
	
	private Map<LanguageCode, Double> proportionPerLanguageCode;


	public WordSelectionAssessment(Set<LanguageWord> selection, VocabularyLearningStatus vls, ReadThroughFocus focus) {
		this.selection = selection;
		for(LanguageWord lw: selection)
			if(!vls.isEarlyPhaseWord(lw))
				throw new Error();
		
		numberOfDirectOccurrencesInText = selection.stream()
				.collect(Collectors.toMap(Function.identity(),x->
			UserLearningTextManager.getAllOccurrencesOfEveryWordEverIncludedInUserText().getOrDefault(x.getWord(),0)));
		
		numberOfTranslationsInText = selection.stream()
				.collect(Collectors.toMap(Function.identity(),
						x->
				UserLearningTextManager.getNumberOfOccurrencesInUserTextOfTheTranslationsOfThisWord(x)));
						
			//	Translator.getTranslationsOf(x).stream()
			//	.map(y->UserLearningTextManager.getAllOccurrencesOfEveryWordEverIncludedInUserText().getOrDefault(y.getWord(),0))
	//			.reduce(0,(y,z)->y+z));

		averageFrequencyOfTheWordsOrTheirTranslationsInTheSelectionInTheTextDatabase = selection.stream().map(x->
		numberOfDirectOccurrencesInText.get(x)+numberOfTranslationsInText.get(x)
				).reduce(0, (x,y)->x+y)/(double)selection.size();
		
	/*	if(selection.contains(LanguageWord.newInstance("where", LanguageCode.EN)))
			System.out.println();*/

		wordsInFocusText = selection.stream().filter(x->focus.isInCurrentFocus(x)).collect(Collectors.toSet());
		double sizeOfSelection = (double)selection.size();

		ratioWordsInSelectionInFocusText = 
				wordsInFocusText.size()/sizeOfSelection;

		proportionGrundforms = getNumberOfGrundforms(selection);

		Map<LanguageCode, Double> proportionPerLanguageCode = vls.getProportionOfMasteryPerLanguageCode();
		priorityRaise = proportionPerLanguageCode.keySet().stream().collect(Collectors.toMap(Function.identity(), x->1d-proportionPerLanguageCode.get(x)));

		complianceToLanguageDistributionQuotas = getProximityToQuotasFor(selection, priorityRaise);

	}

	public static WordSelectionAssessment newInstance(Set<LanguageWord> selection, VocabularyLearningStatus vls,
			ReadThroughFocus focus) {
		return new WordSelectionAssessment(selection, vls,focus);
	}

	public double getFrequencyPerWordOrTranslationInTheSelectionInTextDatabase() {
		return averageFrequencyOfTheWordsOrTheirTranslationsInTheSelectionInTheTextDatabase;
	}

	public double getNumberOfOccurrenceOfWordsInTheSelectionInFocusText() {
		return ratioWordsInSelectionInFocusText;
	}
	
	private double getNumberOfGrundforms(Set<LanguageWord> p1) {
		return (double) p1.stream().filter(x->WordDescription.isGrundForm(x)).count()/(double)p1.size();
	}

	public double getProportionOfGrundForms() {
		return proportionGrundforms;
	}
	
	private double getProximityToQuotasFor(Set<LanguageWord> p1, Map<LanguageCode, Double> priorityRaise) {
		Map<LanguageCode, Long> numberPerCode = priorityRaise.keySet().stream().collect(Collectors.toMap(
				Function.identity(),
				x->p1.stream().filter(y->y.getCode().equals(x)).count()));

		long total = numberPerCode.values().stream().reduce(0l, (x,y)->x+y);

		proportionPerLanguageCode = numberPerCode.keySet().stream().collect(Collectors.toMap(Function.identity(), x->(double)numberPerCode.get(x)/(double)total));

		double error = proportionPerLanguageCode.keySet().stream().map(x->Math.abs(proportionPerLanguageCode.get(x)-priorityRaise.get(x))).reduce(0d, (x,y)->x+y);

		return 1-error/proportionPerLanguageCode.size();
	}

	public double getProximityToQuotas() {
		return complianceToLanguageDistributionQuotas;
	}
	
	public String toString()
	{
	    final DecimalFormat df = new DecimalFormat("0.00");
		return "Average frequency (whole corpus):"+df.format(averageFrequencyOfTheWordsOrTheirTranslationsInTheSelectionInTheTextDatabase)+" "+numberOfDirectOccurrencesInText+" "+numberOfTranslationsInText+
				"\nProportion belonging (focus):"+df.format(ratioWordsInSelectionInFocusText)+" ("+ wordsInFocusText.size()+")"+
		"\nProportion belonging (grundforms):"+
	    		df.format(proportionGrundforms)+"\nCompliance to language distribution quotas:"+df.format(complianceToLanguageDistributionQuotas)+" distribution:"+proportionPerLanguageCode+" target:"+priorityRaise+"\n"+wordsInFocusText;
	}

}
