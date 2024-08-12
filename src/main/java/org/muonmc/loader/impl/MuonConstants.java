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

package org.muonmc.loader.impl;

import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

/**
 * A grab-bag of constants that we use.
 *
 * @see org.muonmc.multiloader.api.LoaderConstants
 */
@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public final class MuonConstants {
	public static final String NAME = "Muon Loader";
	public static final String BRAND = "muon";
	/**
	 * The proper variant of {@link #BRAND}.
	 */
	public static final String CAPITAL_BRAND = "Muon";
	public static final String ORGANIZATION = "MuonMC";
	public static final String MOD_ID = "muon_loader";

	private MuonConstants() {}
}
