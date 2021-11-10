package discordforrad;

import java.awt.AWTException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import javax.security.auth.login.LoginException;

import discordforrad.discordmanagement.DiscordManager;
import discordforrad.models.language.Dictionnary;
import discordforrad.models.language.LanguageWord;
import discordforrad.models.language.wordnetwork.WordNetwork;
import discordforrad.models.language.wordnetwork.forms.RelatedFormsNetwork;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;



public class Main {

	public static final String ROOT_DATABASE = "../databases/discordforrad/";
	public static final boolean PRELOAD_PURGE_MODE = false;

	// Main method
	public static void main(String[] args) throws LoginException, IOException, InterruptedException
	{
		DisOrdforrAI.INSTANCE.startNewSession();
		new Thread(()->{
			try {
				Thread.sleep(60000);
				Dictionnary.main(args);
			} catch (AWTException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
		DiscordManager.jda.addEventListener(new DiscordManager());
	}
}
