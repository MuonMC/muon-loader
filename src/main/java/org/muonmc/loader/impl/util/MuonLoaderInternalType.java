/*
 * Copyright 2024 QuiltMC
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

package org.muonmc.loader.impl.util;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public enum MuonLoaderInternalType {
	/** Permits loader plugins to access the class, but not mods. */
	PLUGIN_API,

	/**
	 * Indicates that mods and plugins are prohibited from accessing the type.
	 * @apiNote Before, this indicated the class was added since quilt-loader 0.18.0, which meant both mods and plugins weren't allowed to use them.
	 */
	INTERNAL,

	/**
	 * Indicates that all App/Game (e.g. Minecraft) classes may access the class, but mods and plugins may not access the class.
	 * <p>
	 * This is intended for patches and hooks to use to grant the App/Game internal access.
	 */
	INTERNAL_HOOK
}
