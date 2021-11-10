package discordforrad;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import discordforrad.discordmanagement.DiscordFontedString;
import discordforrad.discordmanagement.DiscordManager;
import discordforrad.inputUtils.UserLearningTextManager;
import discordforrad.inputUtils.TextInputUtils;
import discordforrad.models.LearningModel;
import discordforrad.models.VocabularyLearningStatus;
import discordforrad.models.VocabularyLearningStatus.LearningStatus;
import discordforrad.models.language.Dictionnary;
import discordforrad.models.language.LanguageText;
import discordforrad.models.language.LanguageWord;
import discordforrad.models.language.WordDescription;
import discordforrad.models.learning.focus.ReadThroughFocus;
import discordforrad.models.learning.session.Session;

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


	public void askForNextWord() {
		if(currentSession.isSessionOver()) {			
			DiscordManager.discussionChannel.sendMessage("No more words to ask for this session").queue();

			for(LanguageText lt: currentFocus.getLanguageTextList())
				discordforrad.discordmanagement.DiscordManager
				.printWithEmphasisOnWords(lt, vls);
			
			INSTANCE.displayStatistics();
			return;		
		}

		LanguageWord currentWordToAsk = currentSession.getNextWord();

		lastWordAsked = currentWordToAsk;


		LanguageCode translateTo = LanguageCode.EN;
		if(lastWordAsked.getCode()==LanguageCode.EN) translateTo = LanguageCode.SV;

		WordDescription description = WordDescription.getDescriptionFor(currentWordToAsk);


		String toAsk = "**"+lastWordAsked.getCode()+"\t"+lastWordAsked.getWord()+"\t"+
				vls.getNumberOfSuccessLearning(lastWordAsked)+"\t"+
				currentSession.getStatisticsOnRemainingToLearn()+"**";
		
		DiscordManager.playSoundFor(currentWordToAsk);

		DiscordManager.discussionChannel.sendMessage(toAsk).queue();
		DiscordManager.print(description);

	}

	public void displayStatistics() {



		int vocabularySize = vls.getAllWords().size();
		int shortTerm = vls.getAllValidShortTermWords().size();
		int midTerm = vls.getAllMidTermWords().size();
		int longTerm = vls.getAllLongTermWords().size();
		String res = "The current size of encountered vocabulary is of "+ vocabularySize+" words\n";
		res = "There are "+shortTerm+" words unexplored; "+midTerm+" words being learned and "+longTerm+" words mastered. "
				+ "For a total of: "+vls.getAllWords().size() +" words\n"
				+"Success rate. Short "+currentSession.getNbSuccessShortTerm()+"/"+currentSession.getNbShortTermWordsAsked()+" "
				+" mid:"+currentSession.getNbSuccessMidTerm()+"/"+currentSession.getNbMidTermWordsAsked()+" "
				+" long"+currentSession.getNbSuccessLongTerm()+"/"+currentSession.getNbLongTermWordsAsked();

		DiscordManager.discussionChannel.sendMessage(res).queue();


		Set<LanguageWord> learnedWords = currentFocus.getLanguageTextList().get(0).getSetOfValidWords().stream()
				.filter(x->Dictionnary.isInDictionnaries(x))
				.filter(x->!vls.getLearningStatus(x).equals(LearningStatus.LEARNED))
				.collect(Collectors.toSet());
		
		if(currentFocus != null && currentFocus instanceof ReadThroughFocus)
		{
			long shortTermT = currentFocus.getAllValidWordsSortedByTheirOrderOfOccurrenceInFocusTexts().stream().collect(Collectors.toSet())
					.stream().filter(x->vls.isEarlyPhaseWord(x)).count();

			long midTermT = currentFocus.getAllValidWordsSortedByTheirOrderOfOccurrenceInFocusTexts().stream().collect(Collectors.toSet())
					.stream().filter(x->vls.isMidTermWord(x)).count();

			long longTermT = currentFocus.getAllValidWordsSortedByTheirOrderOfOccurrenceInFocusTexts().stream().collect(Collectors.toSet())
					.stream().filter(x->vls.isLongTermWord(x)).count();

			String ratioMastery = 
					currentFocus.getLanguageTextList()
					.stream()
					.map(x-> 
					currentFocus.getIndexOf(x)+": "+
					VocabularyLearningStatus.getNumberOfKnownWordsFromWordset(vls,x)+"/"+
					learnedWords.size()
							).reduce("",(x,y)->x+"\t"+y);

			int masteredTexts = currentFocus.getAllMasteredTexts(vls.getAllLongTermWords()).size();



			DiscordManager.discussionChannel.sendMessage("In the current focus, there are: "
					+shortTermT+" words unexplored; "
					+midTermT+" words being learned and "
					+longTermT+" words mastered; for a total of "+
					ratioMastery +" mastered texts\n").queue();
			
			
			
			DiscordManager.print(DiscordFontedString.newInstance(learnedWords.toString()));
		}

	}


	public void confirm(boolean strong) {
		if(lastWordAsked!=null)
		{
			currentSession.confirm(lastWordAsked);
			if(strong)
				vls.strongIncrement(lastWordAsked);
			else
				vls.incrementSuccess(lastWordAsked);
		}
		askForNextWord();		
	}




	public void forgottenLastWord() throws IOException {
		if(lastWordAsked!=null)
		{
			vls.decrementSuccessUpToZero(lastWordAsked);
		}

		askForNextWord();
	}


	public void addFreeString(String languageText, AddStringResultContext c) {
		
		LanguageText currentText = LanguageText.newInstance(LanguageCode.SV, languageText);

		discordforrad.discordmanagement.DiscordManager.printWithEmphasisOnWords(
				currentText, vls);

		if(languageText.length()<2000)
			discordforrad.discordmanagement.DiscordManager.discussionChannel.sendMessage(
					Translator.getGoogleTranslation(currentText, LanguageCode.EN).toString()).queue();
		else
			discordforrad.discordmanagement.DiscordManager.discussionChannel.sendMessage("Text too long to be translated").queue();

		Runnable r = ()->{
			String index = UserLearningTextManager.add(languageText);
			
			Set<LanguageWord> allWordsInCurrentText = currentText.getSetOfValidWords();
			Set<LanguageWord> shortWords = allWordsInCurrentText.parallelStream().filter(x->vls.isEarlyPhaseWord(x)).collect(Collectors.toSet());
			Set<LanguageWord> midWords = allWordsInCurrentText.parallelStream().filter(x->vls.isMidTermWord(x)).collect(Collectors.toSet());
			Set<LanguageWord> longWords = allWordsInCurrentText.parallelStream().filter(x->vls.isLongTermWord(x)).collect(Collectors.toSet());
			
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
		
		currentSession = Session.default3x3LearningSession(currentFocus, vls);
		INSTANCE.displayStatistics();
		askForNextWord();
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

}
