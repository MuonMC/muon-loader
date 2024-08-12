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

package org.muonmc.multiloader.api;

import org.muonmc.loader.impl.MuonConstants;

/**
 * Constants related to the mod loader loaded at runtime.
 *
 * <h1>API Guarantee</h1>
 * <p>
 * Do not depend on these constants to be static between loader versions. Constants may change from implementation to implementation. This class exists to ease
 * runtime logging by providing information about the <b>current implementation</b>, not about the current package.
 */
public final class LoaderConstants {
	/**
	 * The mod loader's full name. This includes the "Loader" part.
	 */
	public static final String NAME = MuonConstants.NAME;
	/**
	 * The loader brand. In the context of Minecraft, this is the string used to patch the server and client brand. This string must be completely lowercase.
	 */
	public static final String BRAND = MuonConstants.BRAND;
	/**
	 * The standard identifier of the loader. This must not change between non-breaking loader versions. This string must be completely lowercase.
	 */
	public static final String ID = "muon";

	private LoaderConstants() {}
}
