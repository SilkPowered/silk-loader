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
import net.fabricmc.loader.util.version.SemanticVersionImpl;
import net.fabricmc.loader.util.version.SemanticVersionPredicateParser;

import org.spongepowered.asm.mixin.Mixins;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;

public class SilkModSetup implements Runnable {

	@Override
	public void run() {
		try {
			// Todo: Auto detect the mod which need not remap.
			for (ModContainer mod : listMods()) {
				for (Path path : mod.getRootPaths()) {
					ModInjector injector;
					File file = path.toFile();
					ClassCache cache = getClassCache(path);

					ClassTinkerers.addURL(file.toURI().toURL());
					BaseFixer fixer = getFixer(mod.getMetadata().getId());
					if (fixer == null) {
						continue;
					}

					injector = new ModInjector(cache, fixer);
					injector.setup();
				}
			}

		} catch (Throwable e) {
			e.printStackTrace();
			return; //Avoid crashing out any other Fabric ASM users
		}

		// Todo: form config files.
		if (isPresent("fabric-entity-events")) {
			Mixins.addConfiguration("silk.compat.fabric_api.entity_events.mixins.json");
		}
	}

	private BaseFixer getFixer(String id) {
		// Todo: from config files.
		switch (id) {
			case "fabric-entity-events":
				return new EntityEventsFixer();
			default:
				return null;
		}
	}

	private static ClassCache getClassCache(Path path) {
		try {
			return ClassCache.read(path.toFile());
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private static Collection<ModContainer> listMods() {
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
