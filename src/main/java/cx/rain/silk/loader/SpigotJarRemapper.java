package cx.rain.silk.loader;

import net.fabricmc.loader.impl.util.mappings.TinyRemapperMappingsHelper;
import net.fabricmc.mapping.tree.TinyMappingFactory;
import net.fabricmc.mapping.tree.TinyTree;
import net.fabricmc.tinyremapper.IMappingProvider;
import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SpigotJarRemapper {
	private static final Pattern MC_LV_PATTERN = Pattern.compile("\\$\\$\\d+");

	private Path outputDir;
	private Set<Path> jarsToRemap = new HashSet<>();

	protected TinyRemapper.Builder remapperBuilder;

	public SpigotJarRemapper() {
		remapperBuilder = TinyRemapper.newRemapper()
				.ignoreConflicts(true);
	}

	public SpigotJarRemapper withStrict() {
		remapperBuilder.renameInvalidLocals(true)
//				.rebuildSourceFilenames(true)
				.invalidLvNamePattern(MC_LV_PATTERN)
//				.inferNameFromSameLvIndex(true)
				.resolveMissing(true)
				.fixPackageAccess(true)
				.propagatePrivate(true);

		return this;
	}

	public SpigotJarRemapper withMappings(URL mappingsUrl, String from, String to) {
		remapperBuilder.withMappings(getMappings(mappingsUrl, from, to));
		return this;
	}

	public SpigotJarRemapper withMappings(Path mappingsPath, String from, String to) {
		try {
			remapperBuilder.withMappings(getMappings(mappingsPath.toUri().toURL(), from, to));
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		return this;
	}

	public SpigotJarRemapper clearJar() {
		jarsToRemap.clear();

		return this;
	}

	public SpigotJarRemapper addJar(Path jar) {
		jarsToRemap.add(jar);

		return this;
	}

	public SpigotJarRemapper addJars(List<Path> jar) {
		jarsToRemap.addAll(jar);

		return this;
	}

	public SpigotJarRemapper setOutput(Path output) {
		outputDir = output;

		return this;
	}

	protected List<Path> getClasspath() {
		// Todo
		List<Path> paths = new ArrayList<>();

		paths.addAll(Arrays.stream(Paths.get("bundler/libraries").toFile().listFiles())
				.map(File::toPath)
				.collect(Collectors.toList()));

		return paths;
	}

	public void doRemap() {
		TinyRemapper remapper = remapperBuilder.build();
		for (Path input : jarsToRemap) {
			Path outFile = outputDir.resolve(input.getFileName());
			try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(outFile).build()) {
				InputTag tag = remapper.createInputTag();
				outputConsumer.addNonClassFiles(input, NonClassCopyMode.FIX_META_INF, remapper);

				remapper.readInputs(tag, input);
				remapper.readClassPath(getClasspath().toArray(new Path[0]));

				remapper.apply(outputConsumer);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		remapper.finish();
	}

	protected IMappingProvider getMappings(URL mappings, String from, String to) {
		try {
			URLConnection connection = mappings.openConnection();

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				TinyTree mappingsTree = TinyMappingFactory.loadWithDetection(reader);
				return TinyRemapperMappingsHelper.create(mappingsTree, from, to);
			}
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
}
