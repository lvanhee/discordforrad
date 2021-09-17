package discordforrad.inputUtils;

import java.util.Set;

public class EntriesFoundWebscrappingOutcome implements DatabaseProcessingOutcome {
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
