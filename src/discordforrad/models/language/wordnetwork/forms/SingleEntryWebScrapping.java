package discordforrad.models.language.wordnetwork.forms;

import discordforrad.inputUtils.DatabaseProcessingOutcome;

public class SingleEntryWebScrapping implements DatabaseProcessingOutcome {
	private final String entry;

	public SingleEntryWebScrapping(String entry2) {
		this.entry = entry2;
	}

	public String get() {
		return entry;
	}

	public static SingleEntryWebScrapping newInstance(String entry) {
		return new SingleEntryWebScrapping(entry);
	}
}
