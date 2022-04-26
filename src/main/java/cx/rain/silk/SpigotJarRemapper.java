package cx.rain.silk;

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
import java.util.stream.Collectors;

public class SpigotJarRemapper extends RemapperBase {
	private Path outputDir;
	private Set<Path> jarsToRemap = new HashSet<>();

	protected TinyRemapper remapper;

	public SpigotJarRemapper(Path mappingsPath, String from, String to) {
		remapper = TinyRemapper.newRemapper()
				.withMappings(getMappings(mappingsPath, from, to))
				.build();

		// Todo
		for (Path p : Arrays.stream(Paths.get("bundler/versions").toFile().listFiles())
				.map(File::toPath)
				.collect(Collectors.toList())) {
			addJar(p);
		}
	}

	public SpigotJarRemapper clearJar() {
		jarsToRemap.clear();

		return this;
	}

	public SpigotJarRemapper addJar(Path jar) {
		jarsToRemap.add(jar);

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
		Path tmp = outputDir.resolve("server.jar");

		for (Path input : jarsToRemap) {
			try (OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(tmp).build()) {
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

	protected IMappingProvider getMappings(Path mappings, String from, String to) {
		try {
			URL url = mappings.toUri().toURL();
			URLConnection connection = url.openConnection();

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				TinyTree mappingsTree = TinyMappingFactory.loadWithDetection(reader);
				return TinyRemapperMappingsHelper.create(mappingsTree, from, to);
			}
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
}
