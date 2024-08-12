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

package org.muonmc.loader.api.plugin;

import org.muonmc.loader.impl.launch.common.MuonLauncherBase;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;
import org.muonmc.loader.api.ModContainer;

@MuonLoaderInternal(MuonLoaderInternalType.PLUGIN_API)
public interface ModContainerExt extends ModContainer {
	@Override
	ModMetadataExt metadata();

	/**
	 * @return the id of the plugin providing this mod. This method MUST return the actual id of the plugin.
	 */
	String pluginId();

	/**
	 * A user-friendly, unique string that describes the "type" of mod being loaded.
	 * <p>
	 * Values returned by Quilt Loader (and therefore shouldn't be used by external plugins!) include "Fabric",
	 * "Quilt", and "Builtin".
	 */
	String modType();

	/** @return True if quilt-loader should add {@link #rootPath()} to it's classpath, false otherwise. */
	boolean shouldAddToQuiltClasspath();

	@Override
	default ClassLoader getClassLoader() {
		return MuonLauncherBase.getLauncher().getClassLoader(this);
	}
}
