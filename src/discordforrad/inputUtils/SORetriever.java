package discordforrad.inputUtils;


import java.io.File;
import java.util.Arrays;

import cachingutils.FileBasedStringSetCache;
import discordforrad.Main;
import discordforrad.models.language.LanguageWord;
import webscrapping.RobotBasedPageReader;

public class SORetriever {
	
	public static void main(String[] args) {
		RobotBasedPageReader.clickOn(100, 100);
		String s = RobotBasedPageReader.getFullPageAsText();
		Arrays.asList(s)
		System.out.println(s);
		
		
	}

}
