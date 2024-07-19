/*
 * Copyright 2022, 2023 QuiltMC
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
	/** Indicates the class is both legacy, and was originally considered "api" - so no warnings should be printed on
	 * callers.
	 *
	 * @deprecated use {@link #INTERNAL} instead.
	 */
	@SuppressWarnings("all") // deprecated but used internally for compatibility
	@Deprecated
	LEGACY_NO_WARN,

	/** Indicates the class is legacy, and not considered "api" at any point - so warnings should be printed when
	 * callers try to access it.
	 *
	 * @deprecated use {@link #INTERNAL} instead.
	 */
	@SuppressWarnings("all") // deprecated but used internally for compatibility
	@Deprecated
	LEGACY_EXPOSED,

	/** Permits loader plugins to access the class, but not mods. */
	PLUGIN_API,

	/**
	 * Indicates that mods and plugins are prohibited from accessing the type.
	 * @apiNote Used to indicate the class was added since quilt-loader 0.18.0, which meant both mods and plugins weren't allowed to use them.
	 */
	INTERNAL;

	/**
	 * For use in {@link org.muonmc.loader.impl.transformer.InternalsHiderTransform}. Do not use elsewhere.
	 */
	public static MuonLoaderInternalType getLegacyNoWarn() {
		return LEGACY_NO_WARN;
	}

	/**
	 * For use in {@link org.muonmc.loader.impl.transformer.InternalsHiderTransform}. Do not use elsewhere.
	 */
	public static MuonLoaderInternalType getLegacyExposed() {
		return LEGACY_EXPOSED;
	}
}
