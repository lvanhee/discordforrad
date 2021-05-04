package discordforrad.discordmanagement;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ExecutionException;
import discordforrad.AddStringResultContext;
import discordforrad.DisOrdforrAI;
import discordforrad.LanguageCode;
import discordforrad.Main;
import discordforrad.inputUtils.TextInputUtils;
import discordforrad.languageModel.LanguageWord;
import discordforrad.models.VocabularyLearningStatus;
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

	public static void printWithEmphasisOnWords(String raw, VocabularyLearningStatus vls, LanguageCode languageCode) {
		
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
		
		OrdforrAIListener.discussionChannel.sendMessage(res).queue();
	}
}
