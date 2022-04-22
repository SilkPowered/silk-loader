package cx.rain.mc.silk.remapper;

import net.fabricmc.accesswidener.AccessWidenerReader;
import net.fabricmc.accesswidener.AccessWidenerRemapper;
import net.fabricmc.accesswidener.AccessWidenerWriter;
import net.fabricmc.loader.impl.FormattedException;
import net.fabricmc.loader.impl.discovery.ModCandidate;
import net.fabricmc.loader.impl.discovery.RuntimeModRemapper;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.loader.impl.launch.MappingConfiguration;
import net.fabricmc.loader.impl.launch.MappingConfigurationSilk;
import net.fabricmc.loader.impl.util.FileSystemUtil;
import net.fabricmc.loader.impl.util.SystemProperties;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.fabricmc.loader.impl.util.mappings.TinyRemapperMappingsHelper;
import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.NonClassCopyMode;
import net.fabricmc.tinyremapper.OutputConsumerPath;
import net.fabricmc.tinyremapper.TinyRemapper;
import net.fabricmc.tinyremapper.extension.mixin.MixinExtension;

import org.objectweb.asm.commons.Remapper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SilkRemapper {
	public static void remap(List<String> plugins, Path backupDir,
							 String originNamespace, String targetNamespace) {
		MappingConfiguration mapping = new MappingConfigurationSilk();
		List<String> modsToRemap = new ArrayList<>();

		Path output = new File("plugins").toPath();

		for (String plugin : plugins) {
			modsToRemap.add(plugin);
		}

		if (modsToRemap.isEmpty()) return;

		TinyRemapper remapper = TinyRemapper.newRemapper()
				.withMappings(TinyRemapperMappingsHelper.create(mapping.getMappings(), originNamespace, targetNamespace))
				.renameInvalidLocals(false)
				.build();

		try {
			remapper.readClassPathAsync(getRemapClasspath().toArray(new Path[0]));
		} catch (IOException e) {
			throw new RuntimeException("Failed to populate remap classpath", e);
		}

		Map<String, RemapInfo> infoMap = new HashMap<>();

		try {
			for (String plugin : modsToRemap) {
				SilkRemapper.RemapInfo info = new SilkRemapper.RemapInfo();
				infoMap.put(plugin, info);

				InputTag tag = remapper.createInputTag();
				info.tag = tag;

				Path path = new File(plugin).toPath();
				Files.deleteIfExists(path);
				Files.copy(path, backupDir);

				info.inputIsTemp = true;
				info.outputPath = path;
				Files.deleteIfExists(info.outputPath);

				remapper.readInputsAsync(tag, info.inputPath);
			}

			for (String plugin : modsToRemap) {
				SilkRemapper.RemapInfo info = infoMap.get(plugin);
				OutputConsumerPath outputConsumer = new OutputConsumerPath.Builder(info.outputPath).build();

				FileSystemUtil.FileSystemDelegate delegate = FileSystemUtil.getJarFileSystem(info.inputPath, false);

				if (delegate.get() == null) {
					throw new RuntimeException("Could not open JAR file " + info.inputPath.getFileName() + " for NIO reading!");
				}

				Path inputJar = delegate.get().getRootDirectories().iterator().next();
				outputConsumer.addNonClassFiles(inputJar, NonClassCopyMode.FIX_META_INF, remapper);

				info.outputConsumerPath = outputConsumer;

				remapper.apply(outputConsumer, info.tag);
			}

			remapper.finish();
		} catch (Throwable t) {
			remapper.finish();

			for (SilkRemapper.RemapInfo info : infoMap.values()) {
				if (info.outputPath == null) {
					continue;
				}

				try {
					Files.deleteIfExists(info.outputPath);
				} catch (IOException e) {
					Log.warn(LogCategory.MOD_REMAP, "Error deleting failed output jar %s", info.outputPath, e);
				}
			}

			throw new FormattedException("Failed to remap mods!", t);
		} finally {
			for (SilkRemapper.RemapInfo info : infoMap.values()) {
				try {
					if (info.inputIsTemp) Files.deleteIfExists(info.inputPath);
				} catch (IOException e) {
					Log.warn(LogCategory.MOD_REMAP, "Error deleting temporary input jar %s", info.inputIsTemp, e);
				}
			}
		}
	}

	private static List<Path> getRemapClasspath() throws IOException {
		String remapClasspathFile = System.getProperty(SystemProperties.REMAP_CLASSPATH_FILE);

		if (remapClasspathFile == null) {
			throw new RuntimeException("No remapClasspathFile provided");
		}

		String content = new String(Files.readAllBytes(Paths.get(remapClasspathFile)), StandardCharsets.UTF_8);

		return Arrays.stream(content.split(File.pathSeparator))
				.map(Paths::get)
				.collect(Collectors.toList());
	}

	private static class RemapInfo {
		InputTag tag;
		Path inputPath;
		Path outputPath;
		boolean inputIsTemp;
		OutputConsumerPath outputConsumerPath;
	}
}
