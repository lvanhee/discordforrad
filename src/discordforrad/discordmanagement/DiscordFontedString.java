package discordforrad.discordmanagement;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import discordforrad.models.language.WordDescription;

public class DiscordFontedString {
	private final List<FontedText> items = new ArrayList<>();
	
	private DiscordFontedString(List<FontedText> items)
	{
		items = items.stream().filter(x->!x.getText().isEmpty()).collect(Collectors.toList());
		if(items.isEmpty())return;
		DiscordFont previousFont = items.get(0).getFont();
		String previousText = "";
		for(FontedText ft:items)
		{
			if(ft.getText().isEmpty()) continue;
			if(previousFont!=null && ft.getFont().equals(previousFont) ||
					FontedText.isSensitiveToFonting(ft.getText()))
				previousText+=ft.getText()+" ";
			else
			{
				this.items.add(FontedText.newInstance(previousText,previousFont));
				previousText = "";
			}	
		}
		if(!items.isEmpty())
		{
			this.items.add(FontedText.newInstance(previousText,previousFont));
		}
	}

	public static DiscordFontedString parse(String toPrint) {
		/*List<FontedText> res = new LinkedList<>();
		boolean isBoldItalicsMode=false;
		boolean isBoldMode = false;
		boolean isItalicsMode = false;
		boolean isHiddenMode = false;
		
		String current = "";
		while(!toPrint.isEmpty())
		{
			if(isFontingToken(to))
			current+=toPrint.charAt(0);
			toPrint=toPrint.substring(1);
		}*/
		throw new Error();
	}

	public boolean isBlank() {
		return items.isEmpty();
	}

	public String toRawDiscordText() {
		if(items.isEmpty())return "";
		String res = "";
		DiscordFont previous = null;
		for(FontedText f: items)
		{
			if(previous!=null && previous != DiscordFont.NO_FONT)
				res+=" ";
			previous = f.getFont();
			
			res+= DiscordFont.getDiscordStringSymbol(f.getFont())+f.getText()+DiscordFont.getDiscordStringSymbol(f.getFont());
		}
		return res;
	}

	public String substring(int i, int j) {
		throw new Error();
	}

	public List<DiscordFontedString> splitInRawString(int numChar) {
		if(this.toRawDiscordText().length()<numChar)return Arrays.asList(this);
		
		int crossedNumItems = 0;
		List<FontedText> listOfItemsOnTheFirstSplitOfTheCurrentStringFromThePreviousIteration = new LinkedList<>();
		int indexCurrentList = 0;
		for(FontedText ft: items)
		{
			List<FontedText> nextList = new ArrayList<>();
			nextList.addAll(listOfItemsOnTheFirstSplitOfTheCurrentStringFromThePreviousIteration);
			nextList.add(ft);
			DiscordFontedString dft = DiscordFontedString.newInstance(nextList);
			if(dft.toRawDiscordText().length()>=numChar)
			{
				String textToAddThatLeadsToOverflow = ft.getText();
				List<FontedText> lastNextListOnTheLeftSideOfTheSplit = new ArrayList<>();
				lastNextListOnTheLeftSideOfTheSplit.addAll(listOfItemsOnTheFirstSplitOfTheCurrentStringFromThePreviousIteration);
				for(int i = 1 ; i < textToAddThatLeadsToOverflow.length(); i++)
				{
					List<FontedText> nextList2 = new ArrayList<>();
					nextList2.addAll(listOfItemsOnTheFirstSplitOfTheCurrentStringFromThePreviousIteration);
					nextList2.add(FontedText.newInstance(textToAddThatLeadsToOverflow.substring(0,i), ft.getFont()));
					DiscordFontedString dft2 = DiscordFontedString.newInstance(nextList2);
					if(dft2.toRawDiscordText().length()>=numChar)
					{
						List<DiscordFontedString> res = new LinkedList<>();
						res.add(DiscordFontedString.newInstance(lastNextListOnTheLeftSideOfTheSplit));
						String remainingString = textToAddThatLeadsToOverflow.substring(i-1);
						List<FontedText>listOfFutureItemsToProcess = new ArrayList<>();
						listOfFutureItemsToProcess.add(FontedText.newInstance(remainingString, ft.getFont()));
						listOfFutureItemsToProcess.addAll(items.subList(indexCurrentList+1, items.size()));
						DiscordFontedString nextFontedStringToPRocess = DiscordFontedString.newInstance(listOfFutureItemsToProcess);
						res.addAll(nextFontedStringToPRocess.splitInRawString(numChar));
						return res;						
					}
					else {lastNextListOnTheLeftSideOfTheSplit = nextList2;}
				}
				throw new Error();
			}
			
			indexCurrentList++;
			listOfItemsOnTheFirstSplitOfTheCurrentStringFromThePreviousIteration.add(ft);
		}
		throw new Error();
	}

	public DiscordFontedString removeFirstInRawString(int i) {
		throw new Error();
	}

	public static DiscordFontedString newInstance(String string) {
		String currentString = "";
		while(!string.isEmpty())
		{
			char current = string.charAt(0);
			if(current=='*'||string.startsWith("||"))
			{
				DiscordFont df = null;
				if(current=='*')
				{
					df = DiscordFont.ITALICS;
					if(string.charAt(1)=='*') 
					{ df = DiscordFont.BOLD;
					if(string.charAt(2)=='*') df = DiscordFont.BOLD_ITALICS_VISIBLE;
					}

					if(df.equals(DiscordFont.ITALICS)){string = string.substring(1);}
					else if(df.equals(DiscordFont.BOLD)){string = string.substring(2);}
					else if(df.equals(DiscordFont.BOLD_ITALICS_VISIBLE)){string = string.substring(3);}
				}
				else {
					df = DiscordFont.INVISIBLE;
					string = string.substring(2);
				}
				
				int endIndex = string.indexOf(DiscordFont.getDiscordStringSymbol(df));
				String affectedString = string.substring(0, endIndex);
				String remainingString = string.substring(endIndex+DiscordFont.getDiscordStringSymbol(df).length());
				List<FontedText> res = new ArrayList<>();
				res.add(FontedText.newInstance(currentString, DiscordFont.NO_FONT));
				DiscordFontedString withinString = newInstance(affectedString);
				withinString = withinString.applyModificator(df);
				res.addAll(withinString.getFontedText());
				DiscordFontedString endString = newInstance(remainingString);
				res.addAll(endString.getFontedText());
				
				return DiscordFontedString.newInstance(res);
			}
			currentString+=current;
			string = string.substring(1);
		}
		List<FontedText> l = new LinkedList<>();
		l.add(FontedText.newInstance(currentString, DiscordFont.NO_FONT));
		return new DiscordFontedString(l);
	}

	private List<FontedText> getFontedText() {
		return items;
	}

	private DiscordFontedString applyModificator(DiscordFont df) {
		return DiscordFontedString.newInstance(items.stream().map(x->
		FontedText.newInstance(x.getText(),
				DiscordFont.applyModificator(x.getFont(),df))).collect(Collectors.toList()));
	}

	public static DiscordFontedString newInstance(List<FontedText> res) {
		return new DiscordFontedString(res);
	}
	
	public String toString()
	{
		return items.toString();
	}

}
