package discordforrad.inputUtils;

import java.util.Set;

import cachingutils.advanced.failable.AttemptOutcome;

@Deprecated
public class EntriesFoundWebscrappingOutcome implements AttemptOutcome {
	private final Set<String> entries;
	
	public EntriesFoundWebscrappingOutcome(Set<String> entries) {
		this.entries = entries;
	}

	public Set<String> getEntries() {
		return entries;
	}

	public static EntriesFoundWebscrappingOutcome newInstance(Set<String> entries) {
		return new EntriesFoundWebscrappingOutcome(entries);
	}
	
	public String toString() { return entries.toString();}

}
