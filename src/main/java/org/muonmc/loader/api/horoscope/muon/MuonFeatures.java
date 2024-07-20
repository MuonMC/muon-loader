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
