package discordforrad.inputUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class WebpageReader {

	private static final Path CACHE_FILEPATH = Paths.get("data/cache/");

	private static WebClient webClient;
	public static String downloadWebPage(String webpage, String uniqueID)
	{
		uniqueID = uniqueID.replaceAll(":", "");
		String cacheLocation = null;
		if(webpage.contains("wordreference.com"))
			cacheLocation = "\\wordReference\\";
		else if(webpage.contains("bab.la"))
			cacheLocation = "\\babla\\";
		String cacheFileName = CACHE_FILEPATH.toString()
				+cacheLocation
				+uniqueID+".html";

		if(new File(cacheFileName).exists())
			try {
				// default StandardCharsets.UTF_8
				String content = Files.readString(Paths.get(cacheFileName), TextInputUtils.ISO_CHARSET);
				return content;

			} catch (IOException e) {
				e.printStackTrace();
			}


		String res = null;

		if(webpage.contains("wordreference.com"))
			res = traditionalWebPageContents(webpage);
		else 
		{
			res = getPageContentsWebClient(webpage);
		}

		BufferedWriter writer;
		try {
			new File(cacheFileName).createNewFile();
			writer = new BufferedWriter(new FileWriter(cacheFileName));

			writer.write(res);

			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Trying to save at:"+cacheFileName);
		}
		return res;
	}

	private synchronized static String getPageContentsWebClient(String webpage) {
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
		webpage = webpage.replaceAll(" ", "%20");
		try {
			// Create URL object
			URL url = new URL(webpage);
			URLConnection conn = url.openConnection();
			String codeType = TextInputUtils.ISO_CHARSET.displayName();
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
				res+=TextInputUtils.Utf8ToIso(line)+"\n";
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
