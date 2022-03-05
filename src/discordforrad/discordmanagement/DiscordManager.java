package discordforrad.discordmanagement;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.security.auth.login.LoginException;

import com.sedmelluq.discord.lavaplayer.container.mp3.Mp3AudioTrack;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.FunctionalResultHandler;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.local.LocalSeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BaseAudioTrack;

import discordforrad.AddStringResultContext;
import discordforrad.DisOrdforrAI;
import discordforrad.Main;
import discordforrad.discordmanagement.audio.PlayerManager;
import discordforrad.inputUtils.TextInputUtils;
import discordforrad.inputUtils.UserLearningTextManager;
import discordforrad.models.LanguageCode;
import discordforrad.models.VocabularyLearningStatus;
import discordforrad.models.language.Dictionnary;
import discordforrad.models.language.LanguageText;
import discordforrad.models.language.LanguageWord;
import discordforrad.models.language.StringPair;
import discordforrad.models.language.SuccessfulTranslationDescription;
import discordforrad.models.language.ResultOfTranslationAttempt;
import discordforrad.models.language.WordDescription;
import discordforrad.models.language.WordDescription.WordType;
import discordforrad.models.learning.focus.ReadThroughFocus;
import discordforrad.translation.Translator;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;

public class DiscordManager extends ListenerAdapter {

	
	public static JDA jda=null;
	public static TextChannel discussionChannel = null;
	//private static final AudioPlayer player;
	static {
		String token;
		try {
		//	System.setProperty("java.library.path", "C:/Users/loisv/Downloads/mpg123-1.28.0-x86-64/mpg123-1.28.0-x86-64/libmpg123-0.dll");
			token = Files.readString(Paths.get(Main.ROOT_DATABASE+"auth/discord_token.txt"));
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
		
		DiscordManager.discussionChannel = 
				//jda.getCategories().get(0).getChannels().get(0);
				jda.getTextChannelsByName("main", true).get(0);
		DiscordManager.discussionChannel.sendMessage("AI ready").queue();

		VoiceChannel channel = jda.getVoiceChannelByName("audio", false).iterator().next();

		AudioPlayerManager playerManager = new DefaultAudioPlayerManager();
		AudioSourceManagers.registerRemoteSources(playerManager);

		LocalSeekableInputStream localStream = new LocalSeekableInputStream(
				new File("C:\\Users\\loisv\\Downloads\\binary.mp3"));
		AudioTrack at = new Mp3AudioTrack(new AudioTrackInfo("", "", 1000, "", true, ""), localStream);
		//	AudioLoadResultHandler load = new FunctionalResultHandler(trackConsumer, playlistConsumer, emptyResultHandler, exceptionConsumer)

		//	playerManager.loadItem(reference, resultHandler)
		//	player = playerManager.createPlayer();


		AudioManager manager = jda.getGuilds().get(0).getAudioManager();
		// MySendHandler should be your AudioSendHandler implementation
		//manager.setSendingHandler(new MySendHandler(player));

		//       player.playTrack(track);
		// Here we finally connect to the target voice channel 
		// and it will automatically start pulling the audio from the MySendHandler instance
		manager.openAudioConnection(channel);

		//	player.playTrack(at);
		//	player.startTrack(at, true);


		System.out.println("end");

		PlayerManager.getInstance()
		.loadAndPlay(DiscordManager.discussionChannel, 
				//"data/cache/mp3/ENaccommodation.mp3"
				"https://www.youtube.com/watch?v=JiF3pbvR5G0"
				);
		
	}

	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		Thread.currentThread().setName("ProcessingIncomingDiscordMessage");
		if (event.getMessage().getContentRaw().equals("y")) {
			DisOrdforrAI.INSTANCE.confirm(false);
		}else if (event.getMessage().getContentRaw().equals("Y")) {
			DisOrdforrAI.INSTANCE.confirm(true);
		}
		else if (event.getMessage().getContentRaw().equals("F")) {
			DisOrdforrAI.INSTANCE.forbidLastWord();
			DisOrdforrAI.INSTANCE.processPostWordAttempt();
		}
		else if (event.getMessage().getContentRaw().equals("/new-session")) {
			DisOrdforrAI.INSTANCE.startNewSession();
		}
		else if (event.getMessage().getContentRaw().equals("/all-words")) {
			print(
			DisOrdforrAI.INSTANCE.getVocabularyLearningStatus().getAllLongTermWords()
			.stream()
			.sorted((x,y)->x.toString().compareTo(y.toString()))
			.collect(Collectors.toList()).toString());
		}
		else
			if (event.getMessage().getContentRaw().equalsIgnoreCase("n")) {	

				try {
					DisOrdforrAI.INSTANCE.recordFailedToRecallLastWord();
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

	public static void printWithEmphasisOnWords(LanguageText lt, 
			VocabularyLearningStatus vls) {

		String raw = lt.getText();
		LanguageCode languageCode = lt.getLanguageCode();
		List<FontedText> res = new LinkedList<>();
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
					LanguageWord lw = LanguageWord.newInstance(currentString, languageCode);
					if(vls.isLearnedWordWord(lw)||!Dictionnary.isInDictionnariesWithCrosscheck(lw)||vls.isForbiddenWord(lw))
						res.add(FontedText.newInstance(currentString, DiscordFont.BOLD_ITALICS_VISIBLE));
					else if(vls.isEarlyPhaseWord(lw))
						res.add(FontedText.newInstance(currentString, DiscordFont.ITALICS));
					else if(vls.isMidTermWord(lw))
						res.add(FontedText.newInstance(currentString, DiscordFont.BOLD));
					else 
						throw new Error();
					currentString="";
				}
				res.add(FontedText.newInstance(currentChar+"", DiscordFont.NO_FONT));
			}	
		}

		if(!currentString.isEmpty())
		{
			LanguageWord currentWord = LanguageWord.newInstance(currentString, languageCode);
			if(vls.isLearnedWordWord(currentWord)||
					!Dictionnary.isInDictionnariesWithCrosscheck(currentWord)||
					vls.isForbiddenWord(currentWord))
				res.add(FontedText.newInstance(currentString, DiscordFont.BOLD_ITALICS_VISIBLE));
			if(vls.isEarlyPhaseWord(currentWord))
				res.add(FontedText.newInstance(currentString, DiscordFont.ITALICS));
			else if(vls.isMidTermWord(currentWord))
				res.add(FontedText.newInstance(currentString, DiscordFont.BOLD));
			
		}
		
		print(DiscordFontedString.newInstance(res));
	}

	public static void print(WordDescription description) {
		print("||"+getHiddenAnswerStringFor(description)+"||");
	}
	
	public static String getHiddenAnswerStringFor(LanguageWord lw){
		return getHiddenAnswerStringFor(WordDescription.getDescriptionFor(lw));
	}

	public static String getHiddenAnswerStringFor(WordDescription description) {
		
		String toPrint = "";//description.toString();

		String wordTypesToPrint = "";

		for(WordType wt:description.getWordTypes())
		{
			wordTypesToPrint+=wt+" "+description.getAlternativesFor(wt)+"\n";
		}


		if(!wordTypesToPrint.isBlank())
			toPrint += wordTypesToPrint+"\n\n";
		
		toPrint+=Translator.getNiceTranslationString(description.getWord(),true)+"\n\n";

		toPrint+="Subforms:\n";

		for(LanguageWord lw:description.getSubforms().getWords())
		{
			toPrint+=lw.getWord()+" "+
					Translator.getNiceTranslationString(lw,false)+"\n";
		}

		toPrint+="\n";

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
		return toPrint;
	}

	public static void print(DiscordFontedString toPrint) {		
		if(toPrint.isBlank())return;
				
		for(DiscordFontedString dfs: toPrint.splitInRawString(2000))
		{
			
			DiscordManager.discussionChannel.sendMessage(dfs.toRawDiscordText()).queue();
		}
	}

	private static String compressedFormattedString(String toPrint) {
		for(String formatFreeString: Arrays.asList(" ","\n","\n\n"))
		{
			toPrint = toPrint.replaceAll("\\*\\*\\*"+formatFreeString+"\\*\\*\\*", formatFreeString);
			toPrint = toPrint.replaceAll("\\*\\*"+formatFreeString+"\\*\\*", formatFreeString);
			toPrint = toPrint.replaceAll("\\*"+formatFreeString+"\\*", formatFreeString);
		}
		
		return toPrint;
		
	}

	public static void playSoundFor(LanguageWord currentWordToAsk) {
		//throw new Error();
	}

	public static void print(String toPrint) {
		print(DiscordFontedString.newInstance(toPrint));
	}

}
