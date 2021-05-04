package discordforrad;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import discordforrad.discordmanagement.OrdforrAIListener;
import discordforrad.inputUtils.RawLearningTextDatabaseManager;
import discordforrad.inputUtils.TextInputUtils;
import discordforrad.languageModel.LanguageText;
import discordforrad.languageModel.LanguageWord;
import discordforrad.models.LearningModel;
import discordforrad.models.VocabularyLearningStatus;
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
		lastWordAsked = currentSession.getNextWord();
		
		LanguageCode translateTo = LanguageCode.EN;
		if(lastWordAsked.getCode()==LanguageCode.EN) translateTo = LanguageCode.SV;
		
		Set<String> translatedWord = Translator.getTranslation(lastWordAsked.getWord(), lastWordAsked.getCode(),translateTo);
		
		String toAsk = "**"+lastWordAsked.getCode()+"\t"+lastWordAsked.getWord()+"\t"+ currentSession.getStatisticsOnRemainingToLearn()+"**\n||"+translatedWord+"||";
		
		OrdforrAIListener.discussionChannel.sendMessage(toAsk).queue();
	}

	public void displayStatistics() {
		
		discordforrad.discordmanagement.OrdforrAIListener
		.printWithEmphasisOnWords(currentFocus.getRawText(), vls,currentFocus.getLanguageCode());

		int vocabularySize = vls.getAllWords().size();
		int shortTerm = vls.getAllShortTermWords().size();
		int midTerm = vls.getAllMidTermWords().size();
		int longTerm = vls.getAllLongTermWords().size();
		String res = "The current size of encountered vocabulary is of "+ vocabularySize+" words\n";
		res = "There are "+shortTerm+" words unexplored; "+midTerm+" words being learned and "+longTerm+" words mastered.\n";
		
		OrdforrAIListener.discussionChannel.sendMessage(res).queue();
		
		
		if(currentFocus != null && currentFocus instanceof ReadThroughFocus)
		{
			long shortTermT = currentFocus.getAllSortedWords().stream().collect(Collectors.toSet())
					.stream().filter(x->vls.isShortTermWord(x)).count();
			
			long midTermT = currentFocus.getAllSortedWords().stream().collect(Collectors.toSet())
					.stream().filter(x->vls.isMidTermWord(x)).count();
			
			long longTermT = currentFocus.getAllSortedWords().stream().collect(Collectors.toSet())
					.stream().filter(x->vls.isLongTermWord(x)).count();
			
			OrdforrAIListener.discussionChannel.sendMessage("In the current focus, there are: "
					+shortTermT+" words unexplored; "
					+midTermT+" words being learned and "
					+longTermT+" words mastered.\n").queue();
		}

	}


	public void confirm(boolean strong) {
		if(lastWordAsked!=null)
			if(strong)
				vls.strongIncrement(lastWordAsked);
			else
				vls.incrementSuccess(lastWordAsked);
		askForNextWord();		
	}
	
	


	public void forgottenLastWord() throws IOException {
		if(lastWordAsked!=null)
		{
			vls.decrementSuccessUpToOne(lastWordAsked);
		}
		
		askForNextWord();
	}


	public void addFreeString(String languageText, AddStringResultContext c) {
		vls.addFreeString(languageText,c, true);
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

		discordforrad.discordmanagement.OrdforrAIListener.discussionChannel.sendMessage("Added "+c.getWords().size()+" new words: "+c.getWords()
		+" among which: "+nbShortTerm+" are new; "+nbMidTerm+" are being assimilated and "+nbLongTerm+" have been been validated").queue();
		
		discordforrad.discordmanagement.OrdforrAIListener.discussionChannel.sendMessage("The text contains "+l.size()+" words; "+allWords.size()
		+" different words among which "+nbShortTermT+" are new; "+nbMidTermT+" are being assimilated and "+nbLongTermT+" have been been validated").queue();
		
		discordforrad.discordmanagement.OrdforrAIListener.discussionChannel.sendMessage("The name of this entry is: "+index).queue();
		
		askForNextWord();
	}

	
	public void startNewSession()
	{
		
		currentSession = EntryDrivenSMLLearningSession.default3x3LearningSession(currentFocus, vls);
		
		
		askForNextWord();
	}


	public void setFocus(ReadThroughFocus f) {
		this.currentFocus = f;
		ReadThroughFocus.saveFocusOnFile(f);
		
		OrdforrAIListener.discussionChannel.sendMessage("Updated focus to learning vocabulary for reading through entry: "+f.getIndex()).queue();
		
		String raw = f.getRawText();
		
		discordforrad.discordmanagement.OrdforrAIListener.printWithEmphasisOnWords(raw, vls,f.getLanguageCode());
		
		startNewSession();
		
	}
	
}