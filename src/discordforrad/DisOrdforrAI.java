package discordforrad;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import discordforrad.discordmanagement.DiscordFontedString;
import discordforrad.discordmanagement.DiscordManager;
import discordforrad.discordmanagement.audio.LocalAudioDatabase;
import discordforrad.discordmanagement.audio.LocalAudioPlayer;
import discordforrad.inputUtils.UserLearningTextManager;
import discordforrad.inputUtils.TextInputUtils;
import discordforrad.models.LanguageCode;
import discordforrad.models.LearningModel;
import discordforrad.models.VocabularyLearningStatus;
import discordforrad.models.VocabularyLearningStatus.LearningStatus;
import discordforrad.models.language.Dictionnary;
import discordforrad.models.language.LanguageText;
import discordforrad.models.language.LanguageWord;
import discordforrad.models.language.WordDescription;
import discordforrad.models.learning.focus.ReadThroughFocus;
import discordforrad.models.learning.session.Session;
import discordforrad.models.learning.session.WordSelectionAssessment;
import discordforrad.translation.Translator;

public enum DisOrdforrAI {
	INSTANCE;

	private final VocabularyLearningStatus vls;
	private ReadThroughFocus currentFocus = ReadThroughFocus.loadCurrentFocus();
	private Session currentSession=null;

	private LanguageWord lastWordAsked = null;
	private DisOrdforrAI()
	{
		try {
			vls = VocabularyLearningStatus.loadFromFile();
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error();
		}
	}


	public void processPostWordAttempt() {
		if(currentSession.isSessionOver()) {			
			DiscordManager.discussionChannel.sendMessage("No more words to ask for this session").queue();

			for(LanguageText lt: currentFocus.getLanguageTextList())
				discordforrad.discordmanagement.DiscordManager
				.printWithEmphasisOnWords(lt, vls);
			
			INSTANCE.displayStatistics();
			return;		
		}

		LanguageWord currentWordToAsk = currentSession.getNextWord();
		
		LocalAudioDatabase.playAsynchronouslyIfHasAudioFile(currentWordToAsk);		
		
		lastWordAsked = currentWordToAsk;


		LanguageCode translateTo = LanguageCode.EN;
		if(lastWordAsked.getCode()==LanguageCode.EN) translateTo = LanguageCode.SV;

		WordDescription description = WordDescription.getDescriptionFor(currentWordToAsk);
		
		int numberOfOccurrences = UserLearningTextManager.getAllOccurrencesOfEveryWordEverIncludedInUserText().getOrDefault(description.getWord().getWord(),0);
		int numberOfTranslations = UserLearningTextManager.getNumberOfOccurrencesInUserTextOfTheTranslationsOfThisWord(description.getWord());

		String toAsk = "**"+lastWordAsked.getCode()+"\t"+lastWordAsked.getWord()+"\t"+
				vls.getNumberOfSuccessLearning(lastWordAsked)+"\t"+
				currentSession.getStatisticsOnRemainingToLearn()+"**\t"+(numberOfOccurrences+numberOfTranslations)+"="+numberOfOccurrences+"+"+numberOfTranslations;
		
		DiscordManager.playSoundFor(currentWordToAsk);

		DiscordManager.discussionChannel.sendMessage(toAsk).queue();
		DiscordManager.print(description);

	}

	public void displayStatistics() {



		int vocabularySize = vls.getAllWords().size();
		int shortTerm = vls.getAllValidShortTermWords().size();
		int midTerm = vls.getAllMidTermWords().size();
		int longTerm = vls.getAllLongTermWords().size();
		
		String res ="-----------------------------------\n";
		
		res+="**Total words learned: "+vls.getAllLongTermWords().size()+"**\n";
		res+="**English / Swedish learned:"+vls.getAllLongTermWords().stream().filter(x->x.getCode().equals(LanguageCode.EN)).count()+"/"+vls.getAllLongTermWords().stream().filter(x->x.getCode().equals(LanguageCode.SV)).count()+"**\n";
		
		res +=
				"**Success rate**:  \t"+
		((double)currentSession.getFinalBalance())/(double)VocabularyLearningStatus.NB_SUCCESSES_FOR_A_WORD_TO_BE_CONSIDERED_AS_LEARNED+"\n"
		+"First exposition:  "+currentSession.getNbSuccessShortTerm()+"/"+currentSession.getNbShortTermWordsAsked()+"\n"
						+"Consolidation: \t"+currentSession.getNbSuccessMidTerm()+"/"+currentSession.getNbMidTermWordsAsked()+"\n"
						+"Learned:         \t\t"+currentSession.getNbSuccessLongTerm()+"/"+currentSession.getNbLongTermWordsAsked()+"\n\n";
		
		res += "The current global vocabulary pool contains "+shortTerm+" / "+midTerm+" / "+longTerm+" = "+vls.getAllWords().size() +" words\n";

		DiscordManager.print(DiscordFontedString.newInstance(res));


		Set<LanguageWord> allWordsInCurrentFocus = currentFocus.getLanguageTextList().get(0).getSetOfValidWords();
		Set<LanguageWord> learnedWords = allWordsInCurrentFocus.stream()
				.filter(x->Dictionnary.isInDictionnariesWithCrosscheck(x))
				.filter(x->!vls.getLearningStatus(x).equals(LearningStatus.LEARNED))
				.collect(Collectors.toSet());
		
		if(currentFocus != null && currentFocus instanceof ReadThroughFocus)
		{
			long shortTermT = currentFocus.getAllValidWordsSortedByTheirOrderOfOccurrenceInFocusTexts().stream().collect(Collectors.toSet())
					.stream().filter(x->vls.isEarlyPhaseWord(x)).count();

			long midTermT = currentFocus.getAllValidWordsSortedByTheirOrderOfOccurrenceInFocusTexts().stream().collect(Collectors.toSet())
					.stream().filter(x->vls.isMidTermWord(x)).count();

			long longTermT = currentFocus.getAllValidWordsSortedByTheirOrderOfOccurrenceInFocusTexts().stream().collect(Collectors.toSet())
					.stream().filter(x->vls.isLearnedWordWord(x)).count();

			String ratioMastery = 
					currentFocus.getLanguageTextList()
					.stream()
					.map(x-> 
					currentFocus.getIndexOf(x)+": "+
					VocabularyLearningStatus.getNumberOfKnownWordsFromWordset(vls,x)+"/"+
					allWordsInCurrentFocus.size()
							).reduce("",(x,y)->x+"\t"+y);

			int masteredTexts = currentFocus.getAllMasteredTexts(vls.getAllLongTermWords()).size();



			DiscordManager.print(DiscordFontedString.newInstance("The current focus text contains "
					+shortTermT+" / "
					+midTermT+" / "
					+longTermT+" = "+
					ratioMastery +" words\n"));
			
			
			
			DiscordManager.print(DiscordFontedString.newInstance(learnedWords.toString()));
			
			DiscordManager.print(WordSelectionAssessment.newInstance(currentSession.getAllUnknownWords(), vls, this.currentFocus).toString());
		}

	}


	public void confirm(boolean strong) {
		if(lastWordAsked!=null)
		{
			
			int learningIncrement = 1;
			
			if(strong)
			{
				learningIncrement = vls.getIncrementScoreIfSetAsLearned(lastWordAsked);
				vls.strongIncrement(lastWordAsked);
				
			}
			else
				vls.incrementSuccess(lastWordAsked);
			
			currentSession.confirm(lastWordAsked, learningIncrement);
		}
		processPostWordAttempt();		
	}




	public void recordFailedToRecallLastWord() throws IOException {
		if(lastWordAsked!=null)
		{
			currentSession.recordFailedToRecallLastWord(vls.getNumberOfSuccessLearning(lastWordAsked)>0, vls.isWordAboutToBeRemovedOutOfLearnedWordsIfNotRecalledCorrectly(lastWordAsked));
			vls.decrementSuccessUpToZero(lastWordAsked);
			
		}

		processPostWordAttempt();
	}


	public void addFreeString(String languageText, AddStringResultContext c) {
		
		LanguageText currentText = LanguageText.newInstance(LanguageCode.SV, languageText);

		discordforrad.discordmanagement.DiscordManager.printWithEmphasisOnWords(
				currentText, vls);

		if(languageText.length()<2000)
			discordforrad.discordmanagement.DiscordManager.discussionChannel.sendMessage(
					Translator.getGoogleTranslation(currentText, LanguageCode.EN,false).toString()).queue();
		else
			discordforrad.discordmanagement.DiscordManager.discussionChannel.sendMessage("Text too long to be translated").queue();

		Runnable r = ()->{
			String index = UserLearningTextManager.add(languageText);
			
			Set<LanguageWord> allWordsInCurrentText = currentText.getSetOfValidWords();
			Set<LanguageWord> shortWords = allWordsInCurrentText.parallelStream().filter(x->vls.isEarlyPhaseWord(x)).collect(Collectors.toSet());
			Set<LanguageWord> midWords = allWordsInCurrentText.parallelStream().filter(x->vls.isMidTermWord(x)).collect(Collectors.toSet());
			Set<LanguageWord> longWords = allWordsInCurrentText.parallelStream().filter(x->vls.isLearnedWordWord(x)).collect(Collectors.toSet());
			
			//discordforrad.discordmanagement.OrdforrAIListener.discussionChannel.sendMessage("Added "+c.getWords().size()+" new words: "+c.getWords()).queue();

			String statistics = "The text lead to the inclusion of "+c.getWords().size()
					+" new words.\n"
					;


			String toPrint = c.getWords()+"\n"+statistics;

			DiscordManager.print(toPrint);

			discordforrad.discordmanagement.DiscordManager.discussionChannel.sendMessage(
					"The text contains "+allWordsInCurrentText.size()+" different words, among which "+
					shortWords.size()+" are new; "+midWords.size()+" are being assimilated and "+longWords.size()+" were formerly learned").queue();
			
			DiscordManager.print("Short words:"+shortWords);
			DiscordManager.print("Mid words:"+midWords);
			DiscordManager.print("Long words:"+longWords);

			discordforrad.discordmanagement.DiscordManager.discussionChannel.sendMessage("The name of this entry is: "+index).queue();

		};
		new Thread(r).start();
		//askForNextWord();
	}


	public void startNewSession()
	{
		
		DiscordManager.printWithEmphasisOnWords(currentFocus.getLanguageTextList().get(0), vls);

	/*	new Thread(()->
		vls.getAllWords()
		.stream()
		.sorted((x,y)->x.toString().compareTo(y.toString()))
		.forEach(x->{
			if(Dictionnary.isInDictionnaries(x))
				System.out.println();
			System.out.println(WordDescription.getDescriptionFor(x));
		})).start();*/
		
	//	System.out.println(WordDescription.getDescriptionFor(LanguageWord.newInstance("fått", LanguageCode.SV)));
		
		
		Set<LanguageWord> mandatoryWordsToConsider = new HashSet<>();
		mandatoryWordsToConsider.addAll(currentFocus.getAllValidWordsSortedByTheirOrderOfOccurrenceInFocusTexts());
		mandatoryWordsToConsider.addAll(
				mandatoryWordsToConsider.stream()
				.map(x->Translator.getTranslationsOf(x)).reduce(new HashSet<>(), (x,y)->{x.addAll(y); return x;}));
		
		
		
	/*	String swedish = mandatoryWordsToConsider
				.stream()
				.filter(x->x.getCode().equals())*/
		String res = "digraph G {\r\n" + 
				"\r\n" + 
				"  subgraph cluster_en {\r\n" + 
				"    style=filled;\r\n" + 
				"    color=lightgrey;\r\n" + 
				"    node [style=filled,color=white];\r\n" + 
				"    a0 -> a1 -> a2 -> a3;\r\n" + 
				"    label = \"EN\";\r\n" + 
				"  }\r\n" + 
				"\r\n" + 
				"  subgraph cluster_se {\r\n" + 
				"    node [style=filled];\r\n" + 
				"    b0 -> b1 -> b2 -> b3;\r\n" + 
				"    label = \"SE\";\r\n" + 
				"    color=blue\r\n" + 
				"  }";
		
		res+=mandatoryWordsToConsider.stream()
				.map(x->{
					String res2 = x.toString().replaceAll(":", "");
					if(vls.isLearnedWordWord(x))
						res2+="[shape=Mdiamond]";
					return res2;
				}
				).reduce("",(x,y)->x+"\n"+y);
		
		res+="\n}";
		
		try {
			Files.writeString(Paths.get(Main.ROOT_DATABASE+"graphs/display.txt"),res);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		currentSession = Session.default3x3LearningSession(currentFocus, vls);
		INSTANCE.displayStatistics();
		processPostWordAttempt();
	}


	public void setFocus(ReadThroughFocus f) {
		/*	this.currentFocus = f;
		ReadThroughFocus.saveFocusOnFile(f);

		OrdforrAIListener.discussionChannel.sendMessage("Updated focus to learning vocabulary for reading through entry: "+f.getIndex()).queue();

		String raw = f.getRawText();

		discordforrad.discordmanagement.OrdforrAIListener.printWithEmphasisOnWords(raw, vls,f.getLanguageCode());

		startNewSession();*/
		throw new Error();

	}


	public void forbidLastWord() {
		vls.forbidWord(lastWordAsked);
	}


	public VocabularyLearningStatus getVocabularyLearningStatus() {
		return vls;
	}

}
