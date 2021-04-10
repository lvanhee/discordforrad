package discordforrad;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;



public class TestJDA {
	public static JDA jda;
	public static String prefix = "~";
	
	// Main method
	public static void main(String[] args) throws LoginException {
		// Have fun with my token ;D
		jda = JDABuilder.createLight("ODMwNTExNzUwMDIwOTg5MDI4.YHHwVw.X3NC6RGdffbEP6Lb0CObjY52AwE").build();
		jda.getPresence().setStatus(OnlineStatus.ONLINE);
		
		jda.addEventListener(new OrdforrAId());
	}
}
