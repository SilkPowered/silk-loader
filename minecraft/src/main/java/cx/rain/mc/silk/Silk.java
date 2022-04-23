package cx.rain.mc.silk;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Silk {
	public static final String MOD_ID = "silk";
	public static final String NAME = "Silk";
	public static final String VERSION = "1.18.2-1.0.0";

	private static String silkModFileName = "silk-" + VERSION + ".jar";
	private static File silkModFile = new File(".silk/mods/" + silkModFileName);

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
