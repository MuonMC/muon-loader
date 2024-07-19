/*
 * Copyright 2016 FabricMC
 * Copyright 2022-2023 QuiltMC
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

package org.muonmc.loader.impl.game;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.muonmc.loader.impl.entrypoint.GameTransformer;
import org.muonmc.loader.impl.launch.common.MuonLauncher;
import org.muonmc.loader.impl.metadata.qmj.InternalModMetadata;
import org.muonmc.loader.impl.util.Arguments;
import org.muonmc.loader.impl.util.LoaderUtil;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public interface GameProvider {
	String getGameId();
	String getGameName();
	String getRawGameVersion();
	String getNormalizedGameVersion();
	Collection<BuiltinMod> getBuiltinMods();

	String getEntrypoint();
	Path getLaunchDirectory();
	boolean isObfuscated();
	default String getNamespace() {
		return isObfuscated()? "intermediary": "named";
	};
	boolean requiresUrlClassLoader();

	boolean isEnabled();
	boolean locateGame(MuonLauncher launcher, String[] args);
	void initialize(MuonLauncher launcher);
	GameTransformer getEntrypointTransformer();
	void unlockClassPath(MuonLauncher launcher);
	void launch(ClassLoader loader);
	default boolean isGameClass(String name) {
		return true;
	}

	default boolean displayCrash(Throwable exception, String context) {
		return false;
	}

	Arguments getArguments();
	String[] getLaunchArguments(boolean sanitize);

	default boolean canOpenGui() {
		return true;
	}

	default boolean hasAwtSupport() {
		return LoaderUtil.hasAwtSupport();
	}

	class BuiltinMod {
		public BuiltinMod(List<Path> paths, InternalModMetadata metadata) {
			Objects.requireNonNull(paths, "null paths");
			Objects.requireNonNull(metadata, "null metadata");

			this.paths = paths;
			this.metadata = metadata;
		}

		public final List<Path> paths;
		public final InternalModMetadata metadata;
	}
}
