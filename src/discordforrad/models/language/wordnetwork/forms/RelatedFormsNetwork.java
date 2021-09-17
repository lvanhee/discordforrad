package discordforrad.models.language.wordnetwork.forms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import cachingutils.PlainObjectFileBasedCache;
import discordforrad.inputUtils.DatabaseProcessingOutcome;
import discordforrad.inputUtils.EntriesFoundWebscrappingOutcome;
import discordforrad.inputUtils.WebScrapping;
import discordforrad.inputUtils.WebScrapping.DataBaseEnum;
import discordforrad.models.language.LanguageWord;
import discordforrad.models.language.WordDescription.WordType;

public class RelatedFormsNetwork {
	
	private static final String PATH_TO_KNOWN_RELATED_NETWORK_TRANSITION_CACHE = "data/cache/related_forms_network.obj";
	private static final PlainObjectFileBasedCache<Map<LanguageWord, RelatedFormsTransition>> knownTransitionsPerWord =
			PlainObjectFileBasedCache.loadFromFile(new File(PATH_TO_KNOWN_RELATED_NETWORK_TRANSITION_CACHE), 
					()->new HashMap<>());

	
	public static RelatedFormsTransition getRelatedForms(LanguageWord lw)
	{
		if(knownTransitionsPerWord.get().containsKey(lw))return knownTransitionsPerWord.get().get(lw);
		
		RelatedFormsTransition res = RelatedFormsTransitionImpl.newInstance();
		
		for(DataBaseEnum base: getRelatedFormsDatabase(lw))
		{
			RelatedFormsTransition formForBase = getRelatedForms(lw,base);
			res = RelatedFormsTransitionImpl.mergeWithBase(res,formForBase);
		}
		
		for(WordType wt: res.getForms().keySet())
			for(LanguageWord lw2:res.getForms().get(wt).getRelatedWords())
				if(knownTransitionsPerWord.get().containsKey(lw2)&&knownTransitionsPerWord.get().get(lw2).equals(res))continue;
				else
				{
					if(! knownTransitionsPerWord.get().containsKey(lw2))
					{
						final RelatedFormsTransition tmp = res;
						knownTransitionsPerWord.get().put(lw2, tmp);
					}
					else 
						{
						final RelatedFormsTransition tmp = res;
						knownTransitionsPerWord.get().put(lw2, 
							RelatedFormsTransitionImpl.mergeWithBase(knownTransitionsPerWord.get().get(lw2),tmp));
						}
				}
		
		knownTransitionsPerWord.get().put(lw, res);
		
		
		return res;
	}
	
	public static void updateCache()
	{
		knownTransitionsPerWord.doAndUpdate(x->{});
	}

	private static Set<DataBaseEnum> getRelatedFormsDatabase(LanguageWord lw) {
		switch (lw.getCode()) {
		case SV: {
			return Arrays.asList(
					DataBaseEnum.SAOL,
					DataBaseEnum.SO).stream().collect(Collectors.toSet());
		}
		case EN: return new HashSet<>();
		default:
			throw new IllegalArgumentException("Unexpected value: " + lw.getCode());
		}
	}



	private static RelatedFormsTransition getRelatedForms(LanguageWord lw, DataBaseEnum base) {
		switch (base) {
		case SO: {
			return getRelatedFormsSo(lw);
		}
		case SAOL: return getRelatedFormsSaol(lw);
		default:
			throw new IllegalArgumentException("Unexpected value: " + base);
		}
	}

	private static RelatedFormsTransition getRelatedFormsSaol(LanguageWord lw) {
		RelatedFormsTransition res = RelatedFormsTransitionImpl.newInstance();
		
		DatabaseProcessingOutcome outcome = WebScrapping.getContentsFrom(lw, DataBaseEnum.SAOL); 
		
		Set<String> allContents = new HashSet<>();
		if(outcome instanceof SingleEntryWebScrapping)
			allContents.add(((SingleEntryWebScrapping)outcome).get());
		else if(outcome instanceof EntriesFoundWebscrappingOutcome)
			allContents.addAll(((EntriesFoundWebscrappingOutcome)outcome).getEntries());
		else throw new Error();
		
		for(String contents:allContents)
		{
			List<String> allForms = 
					Arrays.asList(contents.split("<a class=\"ordklass\">"))
					.stream().collect(Collectors.toList());
			allForms.remove(0);

			for(String wholeForm:allForms)
			{
				final String grund = wholeForm.substring(0,wholeForm.indexOf("</span>")).trim();
				WordType wt = WordType.parseRaw(wholeForm, DataBaseEnum.SAOL);

				RelatedForms rf = RelatedForms.parseFrom(lw,wholeForm,DataBaseEnum.SAOL);
				if(!rf.containsForm(lw))continue;

				res = RelatedFormsTransitionImpl.mergeWithBase(res, wt,rf);

			}
		}
		
		return res;
	}



	private static RelatedFormsTransition getRelatedFormsSo(LanguageWord lw) {
		DatabaseProcessingOutcome outcome = WebScrapping.getContentsFrom(lw, DataBaseEnum.SO);
		Set<String> entries = new HashSet<>();
		
		if(outcome instanceof EntriesFoundWebscrappingOutcome)
			entries.addAll(((EntriesFoundWebscrappingOutcome)outcome).getEntries());
		else if(outcome instanceof SingleEntryWebScrapping)
			entries.add(((SingleEntryWebScrapping)outcome).get());
		
		RelatedFormsTransition res = RelatedFormsTransitionImpl.newInstance();
		
		for(String entry:entries)
		{
			List<String> allFormsFromSO = 
					Arrays.asList(entry.split("<div class=\"superlemma\""))
					.stream().collect(Collectors.toList());
			allFormsFromSO.remove(0);

			
			for(String wholeForm:allFormsFromSO)
			{
				final String wordClassIndicator = "<div class=\"ordklass\">";
				String wordClassStart = wholeForm.substring(wholeForm.indexOf(wordClassIndicator)+wordClassIndicator.length());
				String wordClass = wordClassStart.substring(0, wordClassStart.indexOf("</div>")).trim();
				WordType wt = WordType.parse(wordClass);

				RelatedForms rf = RelatedForms.parseFrom(lw,wholeForm,DataBaseEnum.SO);
				res = RelatedFormsTransitionImpl.mergeWithBase(res, wt, rf);
			}
		}
		
		return res;
	}

}
