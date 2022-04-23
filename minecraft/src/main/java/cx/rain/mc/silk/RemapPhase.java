package cx.rain.mc.silk;

public enum RemapPhase {
	// Common.
	IntermediaryToOfficial("intermediary", "official"),

	// For prod.
	OfficialToSpigot("official", "spigot"),

	// For dev.
	SpigotToOfficial("spigot", "official"),
	OfficialToNamedOfficial("official", "namedOfficial"),
	;

	private String fromName;
	private String toName;
	private RemapPhase(String from, String to) {
		fromName = from;
		toName = to;
	}

	public String getFrom() {
		return fromName;
	}

	public String getTo() {
		return toName;
	}
}
