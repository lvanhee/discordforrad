package discordforrad;

import java.io.IOException;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class OrdforrAIListener extends ListenerAdapter {
		
	
		public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
						
			if (event.getMessage().getContentRaw().equalsIgnoreCase("y")) {
				System.out.println(event.getChannel());
				/*EmbedBuilder info = new EmbedBuilder();
				info.setTitle("Television");
				info.setDescription("Completely useless information about a useless bot called 'Television'.");
				info.setColor(0xf45642);
				info.setFooter("Created by techtoolbox", event.getMember().getUser().getAvatarUrl());
				
				event.getChannel().sendTyping().queue();
				event.getChannel().sendMessage(info.build()).queue();*/
				//info.clear();
				try {
					DisOrdforrAId.confirm();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
