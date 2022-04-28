package cx.rain.silk;

import com.chocohead.mm.api.ClassTinkerers;
import com.google.common.base.MoreObjects;
import cx.rain.silk.compat.mods.fabric_api.entity_events.fixes.EntityEventsFixer;
import cx.rain.silk.patcher.BaseFixer;
import cx.rain.silk.patcher.ModInjector;
import cx.rain.silk.patcher.ClassCache;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.ModContainerImpl;
import net.fabricmc.loader.util.version.SemanticVersionImpl;
import net.fabricmc.loader.util.version.SemanticVersionPredicateParser;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixins;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.zip.ZipInputStream;

public class SilkModSetup implements Runnable {
	private Logger logger = LogManager.getLogger("Silk/Fixer");

	@Override
	public void run() {
		try {
			// Todo: Auto detect the mod which need not remap.
			for (ModContainer mod : listMods()) {
				if (mod instanceof ModContainerImpl) {
					for (Path path : ((ModContainerImpl) mod).getCodeSourcePaths()) {
						ModInjector injector;

						// Silk: well, here it is. Process jar in jar.
						File file;
						try {
							file = path.toFile();
						} catch (UnsupportedOperationException ex) {
							try (ZipInputStream in = new ZipInputStream(Files.newInputStream(path))) {
								file = File.createTempFile("tmp-", "");
								file.deleteOnExit();
								try (var out = new FileOutputStream(file)) {
									IOUtils.copy(in, out);
								}
							}
						}

						ClassCache cache;
						try {
							cache = getClassCache(path);
						} catch (FileNotFoundException ex) {
							logger.debug("Who said it is a file? ", ex);
							continue;	// qylsb.
						}

						ClassTinkerers.addURL(file.toURI().toURL());
						BaseFixer fixer = getFixer(mod.getMetadata().getId());
						if (fixer == null) {
							continue;
						}

						injector = new ModInjector(cache, fixer);
						injector.setup();
					}
				}
			}

		} catch (Throwable e) {
			e.printStackTrace();
			return; //Avoid crashing out any other Fabric ASM users
		}
	}

	private BaseFixer getFixer(String id) {
		// Todo: from config files.
		switch (id) {
			case "fabric-entity-events-v1":
				Mixins.addConfiguration("silk.compat.fabric_api.entity_events.mixins.json");
				return new EntityEventsFixer();
			default:
				return null;
		}
	}

	private static ClassCache getClassCache(Path path) throws IOException {
		return ClassCache.read(path.toFile());
	}

	private static Collection<ModContainer> listMods() {	// Todo: silk: used for internal. maybe better impl.
		return FabricLoader.getInstance().getAllMods();
	}

	private static boolean isPresent(String modID) {
		return FabricLoader.getInstance().isModLoaded(modID);
	}

	private static boolean isPresent(String modID, String versionRange) {
		return isPresent(modID, modMetadata -> compareVersions(versionRange, modMetadata));
	}

	private static boolean isPresent(String modID, Predicate<ModMetadata> extraChecks) {
		if (!isPresent(modID)) return false;

		Optional<ModContainer> modContainer = FabricLoader.getInstance().getModContainer(modID);
		ModMetadata modMetadata = modContainer.map(ModContainer::getMetadata).orElseThrow(() ->
				new RuntimeException("Failed to get mod container for " + modID + ", something has broke badly.")
		);

		return extraChecks.test(modMetadata);
	}

	private static boolean compareVersions(String versionRange, ModMetadata mod) {
		try {
			Predicate<SemanticVersionImpl> predicate = SemanticVersionPredicateParser.create(versionRange);
			SemanticVersionImpl version = new SemanticVersionImpl(mod.getVersion().getFriendlyString(), false);
			return predicate.test(version);
		} catch (@SuppressWarnings("deprecation") net.fabricmc.loader.util.version.VersionParsingException e) {
			System.err.println("Error comparing the version for ".concat(MoreObjects.firstNonNull(mod.getName(), mod.getId())));
			e.printStackTrace();
			return false; //Let's just gamble on the version not being valid also not being a problem
		}
	}
}
