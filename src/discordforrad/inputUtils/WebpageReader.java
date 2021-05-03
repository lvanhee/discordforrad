package discordforrad.inputUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.logging.Level;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class WebpageReader {
	
	private static WebClient webClient;
	public static String downloadWebPage(String webpage)
    {
		if(webpage.contains("wordreference.com"))
			return traditionalWebPageContents(webpage);

		try {
			if(webClient==null)
			{
				webClient = new WebClient();

				webClient.getJavaScriptEngine().shutdown();
				java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
			}

	        final HtmlPage page = webClient.getPage(webpage);
	       
	        final String pageAsXml = page.asXml();
	        
	      //  final String pageAsText = page.asText();
	        return pageAsXml;
	    }
	 catch(Exception e)
	 {
		 e.printStackTrace(); 
		 throw new Error();
	 }
    }

	private static String traditionalWebPageContents(String webpage) {
		try {
			// Create URL object
			URL url = new URL(webpage);
			URLConnection conn = url.openConnection();
			String codeType = conn.getContentEncoding();
			if(codeType == null) codeType = SpecialCharacterManager.ISO_CHARSET.displayName();
			InputStreamReader isr = 
					new InputStreamReader(url.openStream(),
							codeType
							);
			
			BufferedReader readr = 
					new BufferedReader(isr);

			String line = null;
			String res = "";
            while ((line = readr.readLine()) != null) 
            {
                res+=SpecialCharacterManager.Utf8ToIso(line)+"\n";
                }
            
  
            readr.close();
            return res;
        }
  
        // Exceptions
        catch (MalformedURLException mue) {
           // System.out.println("Malformed URL Exception raised");
        }
        catch (IOException ie) {
          //  System.out.println("IOException raised");
        }
		if(webpage.contains("bab.la"))return null;
        throw new Error();
	}
}
