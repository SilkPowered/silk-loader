package cx.rain.silk;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum RemapPhase {
	// Common.
	IntermediaryToOfficial("intermediary", "official", true, false, "i2o"),

	// For prod.
	OfficialToSpigot("official", "spigot", false, true, "o2s"),

	// For dev.
	SpigotToOfficial("spigot", "official", false, false, "s2o"),
	OfficialToNamedOfficial("official", "namedOfficial", false, true, "o2m"),
	;

	private String fromName;
	private String toName;
	private boolean shouldPrintModIds;
	private boolean shouldActuallyLoad;
	private String mappingsName;

	private RemapPhase(String from, String to, boolean printModIds, boolean actuallyLoad, String mappings) {
		fromName = from;
		toName = to;
		shouldPrintModIds = printModIds;
		shouldActuallyLoad = actuallyLoad;
		mappingsName = mappings;
	}

	public static List<RemapPhase> getModsPhases(boolean isDevelopment) {
		if (isDevelopment) {
			return Arrays.asList(IntermediaryToOfficial, OfficialToNamedOfficial);
		} else {
			return Arrays.asList(IntermediaryToOfficial, OfficialToSpigot);
		}
	}

	public static List<RemapPhase> getPluginsPhases(boolean isDevelopment) {
		if (isDevelopment) {
			return Arrays.asList(SpigotToOfficial);
		} else {
			return Collections.emptyList();
		}
	}

	public String getFrom() {
		return fromName;
	}

	public String getTo() {
		return toName;
	}

	public boolean shouldActuallyLoad() {
		return shouldActuallyLoad;
	}

	public boolean shouldPrintModIds() {
		return shouldPrintModIds;
	}

	public String getMappingsTypeName() {
		return mappingsName;
	}
}
