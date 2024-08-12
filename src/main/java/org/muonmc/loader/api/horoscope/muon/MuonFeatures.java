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

package org.muonmc.loader.api.horoscope.muon;

import org.muonmc.loader.api.horoscope.ExperimentalApi;
import org.muonmc.loader.api.horoscope.OptInExperimentalApi;

/**
 * A utility class for referencing features in {@link OptInExperimentalApi}.
 */
@ExperimentalApi(MuonFeatures.FEATURES_CONSTANT)
public final class MuonFeatures {
	/**
	 * A placeholder feature ID that represents nothing.
	 */
	public static final String NONE = "";
	/**
	 * This feature refers to {@link MuonFeatures} (this class), which will always be considered experimental and subject to change.
	 */
	public static final String FEATURES_CONSTANT = "features_constant";
	/**
	 * @see org.muonmc.loader.api.horoscope
	 */
	public static final String HOROSCOPE = "horoscope";
	/**
	 * @see org.muonmc.loader.api.game.LogicalSide
	 */
	public static final String LOGICAL_SIDE = "logical_side";
}
