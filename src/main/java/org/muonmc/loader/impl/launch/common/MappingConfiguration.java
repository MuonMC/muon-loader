/*
 * Copyright 2022, 2023, 2024 QuiltMC
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

package org.muonmc.loader.impl.launch.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.zip.ZipError;

import net.fabricmc.mapping.tree.TinyMappingFactory;
import net.fabricmc.mapping.tree.TinyTree;

import org.muonmc.loader.impl.MuonLoaderImpl;
import org.muonmc.loader.impl.game.GameProvider;
import org.muonmc.loader.impl.util.ManifestUtil;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;
import org.muonmc.loader.impl.util.log.Log;
import org.muonmc.loader.impl.util.log.LogCategory;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public final class MappingConfiguration {
	private boolean initialized;

	private String gameId;
	private String gameVersion;
	private String mappingsSource;
	private TinyTree mappings;

	public String getGameId() {
		initialize();

		return gameId;
	}

	public String getGameVersion() {
		initialize();

		return gameVersion;
	}

	public String getMappingsSource() {
		initialize();

		return mappingsSource;
	}

	public boolean matches(String gameId, String gameVersion) {
		initialize();

		return (this.gameId == null || gameId == null || gameId.equals(this.gameId))
				&& (this.gameVersion == null || gameVersion == null || gameVersion.equals(this.gameVersion));
	}

	public TinyTree getMappings() {
		initialize();

		return mappings;
	}

	public String getTargetNamespace() {
		GameProvider gameProvider = MuonLoaderImpl.INSTANCE.tryGetGameProvider();
		if (gameProvider != null)
			return gameProvider.getNamespace();
		// else
		// If the game provider doesn't exist yet, use the development flag to set the namespace
		return MuonLauncherBase.getLauncher().isDevelopment() ? "named" : "intermediary";
	}

	public boolean requiresPackageAccessHack() {
		// TODO
		return getTargetNamespace().equals("named");
	}

	private void initialize() {
		if (initialized) return;

		Enumeration<URL> urls;
		try {
			urls = MappingConfiguration.class.getClassLoader().getResources("mappings/mappings.tiny");
		} catch (IOException e) {
			throw new UncheckedIOException("Error trying to locate mappings", e);
		}

		while (urls.hasMoreElements()) {
			URL url = urls.nextElement();
			Log.info(LogCategory.MAPPINGS, "Loading mappings: %s", url);

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
					TinyTree mappings = TinyMappingFactory.loadWithDetection(reader);
					Log.debug(LogCategory.MAPPINGS, "Loading mappings took %d ms", System.currentTimeMillis() - time);

					if (mappings.getMetadata().getNamespaces().contains(getTargetNamespace())) {
						this.mappings = mappings;
						this.mappingsSource = url.toString();
						break;
					}

					Log.info(LogCategory.MAPPINGS, "Skipping mappings: Missing namespace '%s'", getTargetNamespace());
				}
			} catch (IOException | ZipError e) {
				throw new RuntimeException("Error reading "+url, e);
			}
		}

		if (mappings == null) {
			Log.info(LogCategory.MAPPINGS, "Mappings not present!");
			mappings = TinyMappingFactory.EMPTY_TREE;
		}

		initialized = true;
	}
}
