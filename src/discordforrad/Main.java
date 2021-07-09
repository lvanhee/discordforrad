package discordforrad;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import javax.security.auth.login.LoginException;

import discordforrad.discordmanagement.OrdforrAIListener;
import discordforrad.models.language.Dictionnary;
import discordforrad.models.language.LanguageWord;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;



public class Main {

	public static JDA jda=null;
	static {
		String token;
		try {
		//	System.setProperty("java.library.path", "C:/Users/loisv/Downloads/mpg123-1.28.0-x86-64/mpg123-1.28.0-x86-64/libmpg123-0.dll");
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



	// Main method
	public static void main(String[] args) throws LoginException, IOException, InterruptedException {
		DisOrdforrAI.INSTANCE.startNewSession();

		jda.addEventListener(new OrdforrAIListener());
	}
}
