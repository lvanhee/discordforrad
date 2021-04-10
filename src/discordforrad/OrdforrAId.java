package discordforrad;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class OrdforrAId extends ListenerAdapter {
		public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
			String[] args = event.getMessage().getContentRaw().split("\\s+");
			
			if (args[0].equalsIgnoreCase("test")) {
				/*EmbedBuilder info = new EmbedBuilder();
				info.setTitle("Television");
				info.setDescription("Completely useless information about a useless bot called 'Television'.");
				info.setColor(0xf45642);
				info.setFooter("Created by techtoolbox", event.getMember().getUser().getAvatarUrl());
				
				event.getChannel().sendTyping().queue();
				event.getChannel().sendMessage(info.build()).queue();*/
				//info.clear();
				
				event.getChannel().sendMessage("ack").queue();
			}
		}
	}
