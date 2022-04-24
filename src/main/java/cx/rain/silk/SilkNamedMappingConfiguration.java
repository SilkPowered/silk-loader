/*
 * Copyright 2016 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cx.rain.silk;

import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.fabricmc.loader.impl.launch.MappingConfiguration;
import net.fabricmc.loader.impl.util.ManifestUtil;
import net.fabricmc.loader.impl.util.log.Log;
import net.fabricmc.loader.impl.util.log.LogCategory;
import net.fabricmc.mapping.tree.TinyMappingFactory;
import net.fabricmc.mapping.tree.TinyTree;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.zip.ZipError;

public class SilkNamedMappingConfiguration extends MappingConfiguration {
	private boolean initialized;

	private String gameId;
	private String gameVersion;
	private TinyTree mappings;

	private RemapPhase mappingPhase;
	private String mappingsFile;

	protected static Map<RemapPhase, SilkNamedMappingConfiguration> mappingsDictionary = new HashMap<>();

	public static SilkNamedMappingConfiguration get(RemapPhase phase) {
		if (!mappingsDictionary.containsKey(phase)) {
			mappingsDictionary.put(phase, new SilkNamedMappingConfiguration(phase));
		}
		return mappingsDictionary.get(phase);
	}

	protected SilkNamedMappingConfiguration(RemapPhase phase) {
		this(phase, "silk-" + Silk.MC_VERSION + "-" + phase.getMappingsTypeName() + ".tiny");
	}

	protected SilkNamedMappingConfiguration(RemapPhase phase, String filename) {
		mappingPhase = phase;
		mappingsFile = filename;
	}

	@Override
	public String getTargetNamespace() {
		return mappingPhase.getTo();
	}

	// Todo
//	public boolean requiresPackageAccessHack() {
//		return getTargetNamespace().equals("bukkit");	// Silk: Change named to Bukkit.
//	}

	@Override
	public String getGameId() {
		initialize();

		return gameId;
	}

	@Override
	public String getGameVersion() {
		initialize();

		return gameVersion;
	}

	@Override
	public boolean matches(String gameId, String gameVersion) {
		initialize();

		return (this.gameId == null || gameId == null || gameId.equals(this.gameId))
				&& (this.gameVersion == null || gameVersion == null || gameVersion.equals(this.gameVersion));
	}

	@Override
	public TinyTree getMappings() {
		initialize();

		return mappings;
	}

	private void initialize() {
		if (initialized) return;

		// Silk: Get mappings from file.
		URL url = null;
		File file = new File(mappingsFile);
		if (file.exists()) {
			try {
				url = file.toURI().toURL();
			} catch (MalformedURLException ex) {
				ex.printStackTrace();
			}
		} else {
			url = this.getClass().getResource("/" + mappingsFile);
		}

		// Silk end.

		if (url != null) {
			try {
				URLConnection connection = url.openConnection();

				if (connection instanceof JarURLConnection) {
					Manifest manifest = ((JarURLConnection) connection).getManifest();

					if (manifest != null) {
						gameId = ManifestUtil.getManifestValue(manifest, new Name("Game-Id"));
						gameVersion = ManifestUtil.getManifestValue(manifest, new Name("Game-Version"));
					}
				}

				try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
					long time = System.currentTimeMillis();
					mappings = TinyMappingFactory.loadWithDetection(reader);
					Log.debug(LogCategory.MAPPINGS, "Loading mappings took %d ms", System.currentTimeMillis() - time);
				}
			} catch (IOException | ZipError e) {
				throw new RuntimeException("Error reading " + url, e);
			}
		}

		if (mappings == null) {
			Log.info(LogCategory.MAPPINGS, "Mappings not present!");
			mappings = TinyMappingFactory.EMPTY_TREE;
		}

		initialized = true;
	}
}
