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

public class Translator {
	
	private static final String ENGLISH_CODE = "en";
	private static final String SWEDISH_CODE = "sv";

   /* public static void main(String[] args) throws IOException {
        String text = "Hello world!";
        System.out.println("Translated text: " + translate(ENGLISH_CODE, SWEDISH_CODE, text));
    }*/

    private static String translate(String langFrom, String langTo, String text) throws IOException {
        // INSERT YOU URL HERE
        String urlStr = "https://script.google.com/macros/s/AKfycbxUDfPgQoUEHg37I0WBHkV9GVkyJdDe1NpCkRh5rMqbhw52Em5G/exec" +
                "?q=" + URLEncoder.encode(text, "UTF-8") +
                "&target=" + langTo +
                "&source=" + langFrom;
        URL url = new URL(urlStr);
        StringBuilder response = new StringBuilder();
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        String res = response.toString();
        res = res.replaceAll("Ã¤", "ä");
        res = res.replaceAll("Ã¶", "ö");
        res = res.replaceAll("Ã¥", "å");
        res = res.replaceAll("Ã–", "ö");
        return res;
    }

	public static Set<String> getTranslation(String word, LanguageCode code, LanguageCode translateTo) {
		try {
			return Arrays.asList(translate(code.toString().toLowerCase(), translateTo.toString().toLowerCase(),word)).stream().collect(Collectors.toSet());
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error();
		}
	}
}