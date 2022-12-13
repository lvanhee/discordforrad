package discordforrad;

import java.awt.AWTException;
import java.io.IOException;
import javax.security.auth.login.LoginException;

import discordforrad.discordmanagement.DiscordManager;
import discordforrad.inputUtils.databases.DatabaseFiller;
import discordforrad.models.language.Dictionnary;



public class Main {

	public static final String ROOT_DATABASE = "../databases/discordforrad/";
	public static final boolean PRELOAD_PURGE_MODE = false;
	public static final boolean SHUTDOWN_WHEN_RUNNING_OUT_OF_GOOGLE_TRANSLATE = false;
	public static final boolean PRELOAD_BABLA_GRUNDFORMS = PRELOAD_PURGE_MODE;

	// Main method
	public static void main(String[] args) throws LoginException, IOException, InterruptedException
	{
	//	Translator.hasTranslationOrAGrundformRelatedTranslationThatIsNotFromGoogle(LanguageWord.newInstance("help", LanguageCode.EN));

		DiscOrdforrAI.INSTANCE.startNewSession();
		new Thread(()->{
			try {
				
				Thread.sleep(60000);
				DatabaseFiller.main(new String[0]);

			} catch (InterruptedException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}).start();
		DiscordManager.jda.addEventListener(new DiscordManager());

	}
}
