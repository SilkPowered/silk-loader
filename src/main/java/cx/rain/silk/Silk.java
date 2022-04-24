package cx.rain.silk;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Silk {
	public static final String MOD_ID = "silk";
	public static final String NAME = "Silk";
	public static final String VERSION = "1.18.2-1.0.0";
	public static final String MC_VERSION = "1.18.2";

	public static final String SILK_CACHE_DIR = ".silk";	// relative to game dir
	public static final String SILK_MOD_DIR = ".silk/mods/";	// relative to game dir
	public static final String SILK_TEMP_DIR = "temp";	// relative to output dir
	public static final String SILK_PROCESSED_MODS_DIR = "processedMods";	// relative to cache dir
	public static final String SILK_PROCESSED_PLUGINS_DIR = "processedPlugins";	// relative to cache dir

	public static RemapPhase lastPhase;

	private static String silkModFileName = "silk-" + VERSION + ".jar";
	private static File silkModFile = new File(SILK_MOD_DIR + silkModFileName);

	public static Path getSilkModPath() {
		if (!silkModFile.exists()) {
			extractFile(silkModFileName);
		}

		return silkModFile.toPath();
	}

	private static void extractFile(String embedName) {
		try {
			InputStream is = Silk.class.getResourceAsStream("/META-INF/silk/" + embedName);
			Files.copy(is, silkModFile.toPath());
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}
}
