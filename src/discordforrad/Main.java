package discordforrad;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.TextChannel;
import sun.jvm.hotspot.memory.FreeChunk;



public class Main {

	public static JDA jda=null;
	static {
		String token;
		try {
			token = Files.readString(Paths.get("data/auth/discord_token.txt"));
			jda = JDABuilder.createLight(token).build();
			jda.getPresence().setStatus(OnlineStatus.ONLINE);
			jda.awaitReady();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (LoginException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	public static TextChannel discussionChannel = null;
	static {
		discussionChannel = 
				//jda.getCategories().get(0).getChannels().get(0);
				jda.getTextChannelsByName("main", true).get(0);
		if(discussionChannel.canTalk()) {
			discussionChannel.sendMessage("AI ready").queue();
		}
	}

	// Main method
	public static void main(String[] args) throws LoginException, IOException, InterruptedException {
		DisOrdforrAI.INSTANCE.startNewSession();
		

		jda.addEventListener(new OrdforrAIListener());
	}
}
