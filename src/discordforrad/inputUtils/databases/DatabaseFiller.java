package discordforrad.inputUtils.databases;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import discordforrad.discordmanagement.DiscordManager;
import discordforrad.discordmanagement.audio.LocalAudioDatabase;
import discordforrad.inputUtils.WebScrapping.DataBaseEnum;
import discordforrad.models.language.Dictionnary;
import discordforrad.models.language.LanguageCode;
import discordforrad.models.language.LanguageWord;
import discordforrad.models.language.wordnetwork.forms.RelatedFormsNetwork;
import discordforrad.models.learning.VocabularyLearningStatus;
import discordforrad.translation.ResultOfTranslationAttempt;
import discordforrad.translation.TranslationOutcomeFailure;
import discordforrad.translation.Translator;

public class DatabaseFiller {

	public static void main(String[] args) throws IOException
	{
		
		VocabularyLearningStatus vls = VocabularyLearningStatus.loadFromFile();
		Set<LanguageWord> allWords = vls.getAllLongTermWords();
		
		allWords.stream().forEach(x->{ System.out.println(x+" -> "+Translator.getTranslationsOf(x));});
		
		Dictionnary.getAllKnownWords().stream().sorted((x,y)->x.toString().compareTo(y.toString())).forEach(x->{
			System.out.println("Database filler: processing:"+x);
			System.out.println(Translator.getTranslationsOf(x));
			System.out.println(RelatedFormsNetwork.getRelatedForms(x));
		});
		/*
		VocabularyLearningStatus vls = VocabularyLearningStatus.loadFromFile();

		AtomicInteger i = new AtomicInteger();
		Set<LanguageWord> allWords = vls.getAllExposableNewLanguageWords(); 
		vls.getAllExposableNewLanguageWords()
		.stream().sorted((x,y)->x.toString().compareTo(y.toString()))
		.forEach(
				x->{System.out.println("\n\n"+i.incrementAndGet()+"/"+allWords.size()+" "+x+"\n");DiscordManager.getHiddenAnswerStringFor(x);});

		AtomicInteger nbGoogleTrads = new AtomicInteger();
		new Thread(
				()->{
					
					Dictionnary.getAllKnownWords().stream()
					.sorted((x,y)->x.toString().compareTo(y.toString()))
					.forEach(x->{
						if(Translator.hasRunOutOfGoogleTranslateForTheDay()) return;
						System.out.println("Google:"+nbGoogleTrads.incrementAndGet()+" "+Translator.getGoogleTranslation(x));});
				}
				)
		.start();
		;
		
		new Thread(
				()->{
					Dictionnary.getAllKnownWords().stream()
					.filter(x->x.getCode()==LanguageCode.SV)
					.sorted((x,y)->x.toString().compareTo(y.toString()))
					.forEach(x->{
						RelatedFormsNetwork.getRelatedForms(x, DataBaseEnum.SAOL);
						RelatedFormsNetwork.getRelatedForms(x, DataBaseEnum.SO);
						});
						System.out.println("All language words done!");
					}
					)				
		.start();
		;
		
		AtomicInteger nbWordReference = new AtomicInteger();
		new Thread(
				()->{
					Dictionnary.getAllKnownWords().stream()
					.sorted((x,y)->x.toString().compareTo(y.toString()))
					.forEach(x->{
						Set<ResultOfTranslationAttempt> res = Translator.getWordReferenceTranslation(x);
					//	ResultOfTranslationAttempt r = res.iterator().next();
						while(!res.isEmpty()&&(res.iterator().next() instanceof TranslationOutcomeFailure))
						{
							try {
								Thread.sleep(10000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							res = Translator.getWordReferenceTranslation(x);
						}
						System.out.println("WordReference:"+nbWordReference.incrementAndGet()+" "+x+" "+res);});
				}
				)
		.start();
		;
		
		new Thread(()->{BabLaProcessing.main(args);}).start();*/

	}

}
