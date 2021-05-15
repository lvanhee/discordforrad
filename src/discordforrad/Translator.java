package discordforrad;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import discordforrad.inputUtils.TextInputUtils;

public class Translator {
	
	private static final String ENGLISH_CODE = "en";
	private static final String SWEDISH_CODE = "sv";

   /* public static void main(String[] args) throws IOException {
        String text = "Hello world!";
        System.out.println("Translated text: " + translate(ENGLISH_CODE, SWEDISH_CODE, text));
    }*/

    private static String translate(String langFrom, String langTo, String text) {
    	if(text.length()>2000) return null;
        try {
        	String encodedString = URLEncoder.encode(text, TextInputUtils.UTF8.displayName());
        String urlStr = "https://script.google.com/macros/s/AKfycbxUDfPgQoUEHg37I0WBHkV9GVkyJdDe1NpCkRh5rMqbhw52Em5G/exec" +
                "?q=" + encodedString +
                "&target=" + langTo +
                "&source=" + langFrom;
        URL url = new URL(urlStr);
        StringBuilder response = new StringBuilder();
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        BufferedReader in = new BufferedReader(
        		new InputStreamReader(con.getInputStream(), TextInputUtils.ISO_CHARSET));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
			in.close();
        String res = response.toString();
        res = TextInputUtils.Utf8ToIso(res);
        if(res.startsWith("<!DOCTYPE html>"))
        {
        	try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
        	return translate(langFrom, langTo, text);
//        	throw new Error();
        }
        if(res.contains("¿½"))
        	throw new Error();
        return res.toLowerCase();
        
        } catch (IOException e) {
			e.printStackTrace();
			throw new Error();
		}
        
    }

	public static Set<String> getTranslation(String word, LanguageCode code, LanguageCode translateTo) {
			return Arrays.asList(translate(code.toString().toLowerCase(), translateTo.toString().toLowerCase(),word)).stream().collect(Collectors.toSet());
	}

	public static String translate(String languageText, LanguageCode sv, LanguageCode en) {
		return translate(sv.toString().toLowerCase(), en.toString().toLowerCase(), languageText);
	}
}