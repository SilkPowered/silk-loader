package cx.rain.mc.silk;

import net.fabricmc.loader.impl.launch.MappingConfiguration;

import java.io.File;
import java.nio.file.Path;

public class LoaderRemapper {
	protected static final Path SILK_CACHE = new File(".silk").toPath();
	protected static final Path TEMP_DIR = SILK_CACHE.resolve("temp");

	private RemapPhase remapPhase;
	private MappingConfiguration mappingConfig;

	private LoaderRemapper(RemapPhase phase, MappingConfiguration mappings) {
		remapPhase = phase;
		mappingConfig = mappings;
	}

	public static LoaderRemapper getRemapper(RemapPhase phase) {
		return new LoaderRemapper(phase, new SilkNamedMappingConfiguration(phase));
	}

	public void doRemap(Path inputDir, Path outputDir) {

	}
}
