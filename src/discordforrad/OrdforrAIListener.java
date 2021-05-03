package discordforrad;

import java.io.IOException;

import discordforrad.languageModel.LanguageText;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class OrdforrAIListener extends ListenerAdapter {


	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {

		if (event.getMessage().getContentRaw().equalsIgnoreCase("y")) {
			try {
				DisOrdforrAI.INSTANCE.confirm();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else
			if (event.getMessage().getContentRaw().equalsIgnoreCase("n")) {	

				try {
					DisOrdforrAI.INSTANCE.forgottenLastWord();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else
				if(!event.getAuthor().isBot()) {
					String message = event.getMessage().getContentRaw();
					String messageStart = message.substring(0,2); 
					try {
						AddStringResultContext c = new AddStringResultContext(
								x->event.getChannel().sendMessage("Adding "+ x).queue()
								);
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
