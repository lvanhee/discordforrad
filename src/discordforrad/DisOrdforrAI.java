package discordforrad;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import discordforrad.learningModel.LearningModel;

public enum DisOrdforrAI {
	INSTANCE;
	private final VocabularyLearningStatus vls;
	
	private static final List<LanguageWord> shortTermWordsToTeachInThisSession = new ArrayList<>();
	private static final List<LanguageWord> midTermWordsToTeachInThisSession = new ArrayList<>();
	private static final List<LanguageWord> longTermWordsToTeachInThisSession = new ArrayList<>();
	
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
	
	
	public static void askForNextWord() {
		
		
		LanguageWord word = null;
		if(!shortTermWordsToTeachInThisSession.isEmpty())
		{word = shortTermWordsToTeachInThisSession.get(0); shortTermWordsToTeachInThisSession.remove(0);}
		else if(!midTermWordsToTeachInThisSession.isEmpty())
		{word = midTermWordsToTeachInThisSession.get(0); midTermWordsToTeachInThisSession.remove(0);}
		else if(!longTermWordsToTeachInThisSession.isEmpty())
		{word = longTermWordsToTeachInThisSession.get(0); longTermWordsToTeachInThisSession.remove(0);}
		else {
			Main.discussionChannel.sendMessage("No more words to ask for this session").queue();
			INSTANCE.displayStatistics();
			return;
		}
		
		LanguageCode translateTo = LanguageCode.EN;
		if(word.getCode()==LanguageCode.EN) translateTo = LanguageCode.SV;
		
		Set<String> translatedWord = Translator.getTranslation(word.getWord(), word.getCode(),translateTo);
		
		String toAsk = "**"+word.getCode()+"\t"+word.getWord()+"\t"+ shortTermWordsToTeachInThisSession.size()
		+"/"+midTermWordsToTeachInThisSession.size()+"/"+longTermWordsToTeachInThisSession.size()+"**\n||"+translatedWord+"||";
		
		INSTANCE.lastWordAsked = word;
		
		Main.discussionChannel.sendMessage(toAsk).queue();
	}

	public void displayStatistics() {
		
		int vocabularySize = vls.getAllWords().size();
		int shortTerm = vls.getAllShortTermWords().size();
		int midTerm = vls.getAllMidTermWords().size();
		int longTerm = vls.getAllLongTermWords().size();
		String res = "The current size of encountered vocabulary is of "+ vocabularySize+" words\n";
		res = "There are "+shortTerm+" words unexplored; "+midTerm+" words being learned and "+longTerm+" words mastered.\n";
		
		Main.discussionChannel.sendMessage(res).queue();
	}


	public void confirm() throws IOException {
		if(lastWordAsked!=null)
		{
			vls.incrementSuccess(lastWordAsked);
		}
		askForNextWord();		
		
	}


	public void forgottenLastWord() throws IOException {
		if(lastWordAsked!=null)
		{
			vls.decrementSuccessUpToOne(lastWordAsked);
		}
		
		askForNextWord();
	}


	public void addFreeString(LanguageText languageText, AddStringResultContext c) {
		vls.addFreeString(languageText,c, true);
		askForNextWord();
	}

	
	public void startNewSession()
	{
		shortTermWordsToTeachInThisSession.clear();
		List<LanguageWord> allShortTermWordsReadyToBeAsked = vls.getAllShortTermWords().stream().filter(x->LearningModel.isTimeForLearning(x, vls)).collect(Collectors.toList());
		List<LanguageWord> allMidTermWordsReadyToBeAsked = vls.getAllMidTermWords().stream().filter(x->LearningModel.isTimeForLearning(x, vls)).collect(Collectors.toList());
		List<LanguageWord> allLongTermWordsReadyToBeAsked = vls.getAllLongTermWords().stream().filter(x->LearningModel.isTimeForLearning(x, vls)).collect(Collectors.toList());
		Random r = new Random();
		List<LanguageWord> allShortTermWordsPreviouslyFailed = vls.getAllShortTermWords().stream()
				.filter(x->vls.getLastSuccessOf(x).isAfter(LocalDateTime.MIN))
				.filter(x->LearningModel.isTimeForLearning(x, vls)).collect(Collectors.toList());
		
		for(int i = 0 ; i < 10 ; i++)
		{
			if(allShortTermWordsPreviouslyFailed.isEmpty())break;
			int index = r.nextInt(allShortTermWordsPreviouslyFailed.size());
			shortTermWordsToTeachInThisSession.add(allShortTermWordsPreviouslyFailed.get(index));
			allShortTermWordsPreviouslyFailed.remove(index);
		}
		
		while(shortTermWordsToTeachInThisSession.size() < 10)
		{
			if(allShortTermWordsReadyToBeAsked.isEmpty())break;
			int index = r.nextInt(allShortTermWordsReadyToBeAsked.size());
			shortTermWordsToTeachInThisSession.add(allShortTermWordsReadyToBeAsked.get(index));
			allShortTermWordsReadyToBeAsked.remove(index);
		}
		
		midTermWordsToTeachInThisSession.clear();
		for(int i = 0 ; i < 10 ; i++)
		{
			if(allMidTermWordsReadyToBeAsked.isEmpty())break;
			int index = r.nextInt(allMidTermWordsReadyToBeAsked.size());
			midTermWordsToTeachInThisSession.add(allMidTermWordsReadyToBeAsked.get(index));
			allMidTermWordsReadyToBeAsked.remove(index);
		}
		
		longTermWordsToTeachInThisSession.clear();
		for(int i = 0 ; i < 10 ; i++)
		{
			if(allLongTermWordsReadyToBeAsked.isEmpty())break;
			int index = r.nextInt(allLongTermWordsReadyToBeAsked.size());
			midTermWordsToTeachInThisSession.add(allLongTermWordsReadyToBeAsked.get(index));
			allLongTermWordsReadyToBeAsked.remove(index);
		}
		
		
		DisOrdforrAI.askForNextWord();
	}
	
	
}
