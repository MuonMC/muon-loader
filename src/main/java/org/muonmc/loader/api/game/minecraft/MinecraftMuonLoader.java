/*
 * Copyright 2016 FabricMC
 * Copyright 2022-2024 QuiltMC
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

package org.muonmc.loader.api.game.minecraft;

import org.muonmc.loader.impl.MuonLoaderImpl;

/** Public access for some minecraft-specific functionality in Muon loader. */
public final class MinecraftMuonLoader {
	private MinecraftMuonLoader() {}

	/**
	 * Get the current environment type.
	 *
	 * @return the current environment type
	 */
	public static Environment getEnvironmentType() {
		// TODO: Get this from a plugin instead!
		MuonLoaderImpl impl = MuonLoaderImpl.INSTANCE;
		if (impl == null) {
			throw new IllegalStateException("Accessed MuonLoader too early!");
		}
		return impl.getEnvironmentType();
	}
}
