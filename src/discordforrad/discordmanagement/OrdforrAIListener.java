package discordforrad.discordmanagement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import discordforrad.AddStringResultContext;
import discordforrad.DisOrdforrAI;
import discordforrad.LanguageCode;
import discordforrad.Main;
import discordforrad.Translator;
import discordforrad.inputUtils.TextInputUtils;
import discordforrad.models.VocabularyLearningStatus;
import discordforrad.models.language.LanguageText;
import discordforrad.models.language.LanguageWord;
import discordforrad.models.language.StringPair;
import discordforrad.models.language.WordDescription;
import discordforrad.models.learning.focus.ReadThroughFocus;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class OrdforrAIListener extends ListenerAdapter {


	public static TextChannel discussionChannel = null;
	static {
		OrdforrAIListener.discussionChannel = 
				//jda.getCategories().get(0).getChannels().get(0);
				Main.jda.getTextChannelsByName("main", true).get(0);
		if(OrdforrAIListener.discussionChannel.canTalk()) {
			OrdforrAIListener.discussionChannel.sendMessage("AI ready").queue();
		}
	}

	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {		
		if (event.getMessage().getContentRaw().equals("y")) {
				DisOrdforrAI.INSTANCE.confirm(false);
		}else if (event.getMessage().getContentRaw().equals("Y")) {
				DisOrdforrAI.INSTANCE.confirm(true);
		}
		else if (event.getMessage().getContentRaw().equals("/new-session")) {
				DisOrdforrAI.INSTANCE.startNewSession();
			
		}
		else
			if (event.getMessage().getContentRaw().equalsIgnoreCase("n")) {	

				try {
					DisOrdforrAI.INSTANCE.forgottenLastWord();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else if(event.getMessage().getContentRaw().startsWith("/set-learning-focus-on"))
			{
				String message = event.getMessage().getContentRaw();
				String[] bits = message.split(" "); 
				DisOrdforrAI.INSTANCE.setFocus(
						ReadThroughFocus.newInstance(bits[1], 
						LanguageCode.valueOf(bits[2])));
			}
			else
			{
				AddStringResultContext c = new AddStringResultContext(
					x->{}//event.getChannel().sendMessage("Adding "+ x).queue()
						);
				if(!event.getAuthor().isBot()) {
					if(event.getMessage().getAttachments().size()>0)
					{
						processInputAsFile(event.getMessage().getAttachments(),c);
					}
					else {
						String message = event.getMessage().getContentRaw();
						try {
							DisOrdforrAI.INSTANCE.addFreeString(message,c);
							event.getChannel().sendMessage("Added "+ c.getWords().size()+" words: "+c.getWords() ).queue();
							return;
						}
						catch(IllegalArgumentException e)
						{
							event.getChannel().sendMessage("The language code has not been recognized").queue();
						}
					}
				}
			}
	}

	private static void processInputAsFile(List<Attachment> attachments, AddStringResultContext c) {
		for(Attachment a : attachments)
		{
			try {
				BufferedReader br;

				br = new BufferedReader(new InputStreamReader(
						a.retrieveInputStream().get(), TextInputUtils.UTF8));
				String total = "";
				String line = null;
				while((line = br.readLine()) != null)
					total+=line+"\n";
				
				DisOrdforrAI.INSTANCE.addFreeString(total, c);
				br.close();
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void printWithEmphasisOnWords(LanguageText lt, VocabularyLearningStatus vls) {
		
		String raw = lt.getText();
		LanguageCode languageCode = lt.getLanguageCode();
		String res = "";
		int current = 0;
		String currentString="";
		boolean isParsingWord = false;
		for(int i = 0 ; i < raw.length(); i++)
		{
			char currentChar = raw.charAt(i);
			if(Character.isAlphabetic(currentChar))
				currentString+=raw.charAt(i);
			else
			{
				if(!currentString.isEmpty())
				{
					if(vls.isShortTermWord(LanguageWord.newInstance(currentString, languageCode)))
						res+="*"+currentString+"*";
					else if(vls.isMidTermWord(LanguageWord.newInstance(currentString, languageCode)))
						res+="**"+currentString+"**";
					else if(vls.isLongTermWord(LanguageWord.newInstance(currentString, languageCode)))
						res+="***"+currentString+"***";
					currentString="";
				}
				res+= raw.charAt(i);
				
			}	
		}
		
		if(!currentString.isEmpty())
		{
			if(vls.isShortTermWord(LanguageWord.newInstance(currentString, languageCode)))
				res+="*"+currentString+"*";
			else if(vls.isMidTermWord(LanguageWord.newInstance(currentString, languageCode)))
				res+="**"+currentString+"**";
			else if(vls.isLongTermWord(LanguageWord.newInstance(currentString, languageCode)))
				res+="***"+currentString+"***";
		}
		
		while(res.length()>1000)
		{
			String toPrint = res.substring(0, 1000);
			res = res.substring(1000);
			int indexSplit = res.indexOf(".")+1;
			toPrint+=res.substring(0,indexSplit);
			res = res.substring(indexSplit);
			OrdforrAIListener.discussionChannel.sendMessage(toPrint).queue();
		}
		try {
		if(!res.isEmpty())
			OrdforrAIListener.discussionChannel.sendMessage(res).queue();
		}
		catch(IllegalStateException e)
		{
			throw new Error();
		}
	}

	public static void print(WordDescription description) {
		String toPrint = "";
		
		String translationText = "";
		

		for(String s:description.getTranslations())
		{
			translationText+=s+"\n";
		}
		
		
		toPrint+=translationText+"\n\n";
		
		List<StringPair> contextSentences = 
				description.getContextSentences().stream()
				.collect(Collectors.toList());
		Collections.shuffle(contextSentences);
		
		for(int i = 0 ; i < 5 && i<contextSentences.size() ; i++)
		{
			StringPair p = contextSentences.get(i);
			toPrint+=p.getLeft().replaceAll("<strong>", "**")
					.replaceAll("</strong>", "**")
					+"\n"
					+p.getRight()+"\n\n";
		}
		
		toPrint  = toPrint.substring(0,toPrint.length()-2);
		if(toPrint.length()>2000)
			toPrint = toPrint.substring(0,1980);
		print("||"+toPrint+"||");
	}

	private static void print(String toPrint) {
		OrdforrAIListener.discussionChannel.sendMessage(toPrint).queue();
	}

}
