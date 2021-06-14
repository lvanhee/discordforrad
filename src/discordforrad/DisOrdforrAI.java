package discordforrad;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import discordforrad.discordmanagement.OrdforrAIListener;
import discordforrad.inputUtils.RawLearningTextDatabaseManager;
import discordforrad.inputUtils.TextInputUtils;
import discordforrad.models.LearningModel;
import discordforrad.models.VocabularyLearningStatus;
import discordforrad.models.language.Dictionnary;
import discordforrad.models.language.LanguageText;
import discordforrad.models.language.LanguageWord;
import discordforrad.models.language.WordDescription;
import discordforrad.models.learning.focus.ReadThroughFocus;
import discordforrad.models.learning.session.EntryDrivenSMLLearningSession;

public enum DisOrdforrAI {
	INSTANCE;

	private final VocabularyLearningStatus vls;
	private ReadThroughFocus currentFocus = ReadThroughFocus.loadCurrentFocus();
	private EntryDrivenSMLLearningSession currentSession;

	private LanguageWord lastWordAsked = null;
	private DisOrdforrAI()
	{
		try {
			vls = VocabularyLearningStatus.loadFromFile();
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error();
		}

		currentSession = EntryDrivenSMLLearningSession.default3x3LearningSession(currentFocus,vls);
	}


	public void askForNextWord() {
		if(currentSession.isSessionOver()) {			
			OrdforrAIListener.discussionChannel.sendMessage("No more words to ask for this session").queue();

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
		
		OrdforrAIListener.discussionChannel.sendMessage(toAsk).queue();
		OrdforrAIListener.print(description);

	}

	public void displayStatistics() {

		for(LanguageText lt: currentFocus.getLanguageTextList())
		discordforrad.discordmanagement.OrdforrAIListener
		.printWithEmphasisOnWords(lt, vls);

		int vocabularySize = vls.getAllWords().size();
		int shortTerm = vls.getAllShortTermWords().size();
		int midTerm = vls.getAllMidTermWords().size();
		int longTerm = vls.getAllLongTermWords().size();
		String res = "The current size of encountered vocabulary is of "+ vocabularySize+" words\n";
		res = "There are "+shortTerm+" words unexplored; "+midTerm+" words being learned and "+longTerm+" words mastered.\n"
				+"Success rate. Short "+currentSession.getNbSuccessShortTerm()+"/"+currentSession.getNbShortTermWordsAsked()+" "
				+" mid:"+currentSession.getNbSuccessMidTerm()+"/"+currentSession.getNbMidTermWordsAsked()+" "
				+" long"+currentSession.getNbSuccessLongTerm()+"/"+currentSession.getNbLongTermWordsAsked();

		OrdforrAIListener.discussionChannel.sendMessage(res).queue();


		if(currentFocus != null && currentFocus instanceof ReadThroughFocus)
		{
			long shortTermT = currentFocus.getAllValidSortedWords().stream().collect(Collectors.toSet())
					.stream().filter(x->vls.isShortTermWord(x)).count();

			long midTermT = currentFocus.getAllValidSortedWords().stream().collect(Collectors.toSet())
					.stream().filter(x->vls.isMidTermWord(x)).count();

			long longTermT = currentFocus.getAllValidSortedWords().stream().collect(Collectors.toSet())
					.stream().filter(x->vls.isLongTermWord(x)).count();

			String ratioMastery = 
					currentFocus.getLanguageTextList()
					.stream()
					.map(x-> 
					currentFocus.getIndexOf(x)+": "+
					VocabularyLearningStatus.getNumberOfKnownWordsFromWordset(vls,x)+"/"+
					VocabularyLearningStatus.getNumberOfStudiedWordsFromWordset(vls,x)
							).reduce("",(x,y)->x+"\t"+y);

			int masteredTexts = currentFocus.getAllMasteredTexts(vls.getAllLongTermWords()).size();

			

			OrdforrAIListener.discussionChannel.sendMessage("In the current focus, there are: "
					+shortTermT+" words unexplored; "
					+midTermT+" words being learned and "
					+longTermT+" words mastered; for a total of "+
					ratioMastery +" mastered texts\n").queue();
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
		
		discordforrad.discordmanagement.OrdforrAIListener.printWithEmphasisOnWords(
				LanguageText.newInstance(LanguageCode.SV, languageText), vls);

		if(languageText.length()<2000)
			discordforrad.discordmanagement.OrdforrAIListener.discussionChannel.sendMessage(Translator.translate(languageText, LanguageCode.SV, LanguageCode.EN)).queue();
		else
			discordforrad.discordmanagement.OrdforrAIListener.discussionChannel.sendMessage("Text too long to be translated").queue();

		Runnable r = ()->{
			
			long nbShortTermBefore = vls.getAllShortTermWords().parallelStream().filter(x->c.getWords().contains(x)).count();
		
			vls.addFreeString(languageText,c, true,1);
			
		String index = RawLearningTextDatabaseManager.add(languageText);

		long nbShortTerm = vls.getAllShortTermWords().parallelStream().filter(x->c.getWords().contains(x)).count();
		long nbMidTerm = vls.getAllMidTermWords().parallelStream().filter(x->c.getWords().contains(x)).count();
		long nbLongTerm = vls.getAllLongTermWords().parallelStream().filter(x->c.getWords().contains(x)).count();

		List<String> l = TextInputUtils.toListOfWords(languageText);
		Set<String> allWords = l.stream().collect(Collectors.toSet());
		long nbShortTermT = vls.getAllShortTermWords().parallelStream().filter(x->allWords.contains(x.getWord())).count();
		long nbMidTermT = vls.getAllMidTermWords().parallelStream().filter(x->allWords.contains(x.getWord())).count();
		long nbLongTermT = vls.getAllLongTermWords().parallelStream().filter(x->allWords.contains(x.getWord())).count();

		//		discordforrad.discordmanagement.OrdforrAIListener.discussionChannel.sendMessage("Added "+c.getWords().size()+" new words: "+c.getWords()).queue();

		
		String statistics = "The text lead to the inclusion of "+c.getWords().size()
				+" new words.\n"
				;
		

		String toPrint = c.getWords()+"\n"+statistics;
		
		while(toPrint.length()>2000)
		{
			String toPrintNow = toPrint.substring(0,2000);
			toPrint = toPrint.substring(2000);
			discordforrad.discordmanagement.OrdforrAIListener.discussionChannel.sendMessage(toPrintNow).queue();
		}
		
		if(languageText.length()<2000)
			discordforrad.discordmanagement.OrdforrAIListener.discussionChannel.sendMessage(Translator.translate(languageText, LanguageCode.SV, LanguageCode.EN)).queue();
		else
			discordforrad.discordmanagement.OrdforrAIListener.discussionChannel.sendMessage("Text too long to be translated").queue();
		
		discordforrad.discordmanagement.OrdforrAIListener.discussionChannel.sendMessage(toPrint).queue();

		discordforrad.discordmanagement.OrdforrAIListener.discussionChannel.sendMessage("The text contains "+l.size()+" words; "+allWords.size()
		+" different words among which "+nbShortTermT+" are new; "+nbMidTermT+" are being assimilated and "+nbLongTermT+" have been been validated").queue();

		discordforrad.discordmanagement.OrdforrAIListener.discussionChannel.sendMessage("The name of this entry is: "+index).queue();

		};
		new Thread(r).start();
		//askForNextWord();
	}


	public void startNewSession()
	{

		currentSession = EntryDrivenSMLLearningSession.default3x3LearningSession(currentFocus, vls);


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

}
