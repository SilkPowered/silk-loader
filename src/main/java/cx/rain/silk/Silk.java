package cx.rain.silk;

import net.fabricmc.loader.impl.launch.knot.Knot;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Silk {
	public static final String MOD_ID = "silk";
	public static final String NAME = "Silk";
	public static final String VERSION = "1.18.2-1.0.0";
	public static final String MC_VERSION = "1.18.2";

	public static final Path SILK_CACHE_DIR = Paths.get(".silk");	// relative to game dir
	public static final Path SILK_MOD_DIR = SILK_CACHE_DIR.resolve("mods");	// relative to cache dir
	public static final Path SILK_TEMP_DIR = SILK_CACHE_DIR.resolve("temp");	// relative to cache dir
	public static final Path SILK_SERVER_DIR = SILK_CACHE_DIR.resolve("server");	// relative to cache dir
	public static final Path SILK_SERVER_CACHE_DIR = SILK_CACHE_DIR.resolve("server-temp");	// relative to cache dir
	public static final Path SILK_PROCESSED_MODS_DIR = SILK_CACHE_DIR.resolve("processedMods");	// relative to cache dir
	public static final Path SILK_PROCESSED_PLUGINS_DIR = SILK_CACHE_DIR.resolve("processedPlugins");	// relative to cache dir

	public static RemapPhase lastPhase;

	private static String silkModFileName = "silk-" + VERSION + ".jar";
	private static File silkModFile = SILK_MOD_DIR.resolve(silkModFileName).toFile();

	public static Path getSilkModPath() {
		if (!silkModFile.exists()) {
			extractFile(silkModFileName);
		}

		return silkModFile.toPath();
	}

	private static void extractFile(String embedName) {
		try {
			File dir = SILK_MOD_DIR.toFile();
			if (!dir.exists()) {
				dir.mkdirs();
			}

			InputStream is = Silk.class.getResourceAsStream("/META-INF/silk/" + embedName);
			Files.copy(is, silkModFile.toPath());
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static boolean isNoRemap() {
		return Boolean.parseBoolean(System.getProperty("silk.noRemap", "false"));
	}

	public static void remapSpigot() {
		if (SILK_SERVER_DIR.toFile().exists() && SILK_SERVER_DIR.toFile().listFiles().length > 0) {
			Log.info(LogCategory.GAME_REMAP, "There is already a remapped jar.");
			return;
		}

		// Todo: auto detect outdated jars.
		Log.info(LogCategory.GAME_REMAP, "Remapping Spigot server jar file.");

		File serverCache = SILK_SERVER_CACHE_DIR.toFile();
		if (!serverCache.exists() || serverCache.listFiles().length == 0) {
			new SpigotJarRemapper()
					.withMappings(Paths.get("silk-1.18.2-s2o.tiny"), "spigot", "official")
					.withStrict()
					.addJars(Arrays.stream(new File("bundler/versions").listFiles()).map(File::toPath).collect(Collectors.toList()))
					.setOutput(SILK_SERVER_CACHE_DIR)
					.doRemap();
		} else {
			Log.info(LogCategory.GAME_REMAP, "Server with official mapping found, use it.");
		}

		new SpigotJarRemapper()
				.withMappings(Knot.class.getClassLoader().getResource("mappings/mappings.tiny"), "official", "intermediary")
				.addJars(Arrays.stream(serverCache.listFiles()).map(File::toPath).collect(Collectors.toList()))
				.setOutput(SILK_SERVER_DIR)
				.doRemap();
	}
}
