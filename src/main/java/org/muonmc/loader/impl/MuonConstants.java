package org.muonmc.loader.impl;

import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

/**
 * A grab-bag of constants that we use.
 */
@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public final class MuonConstants {
	public static final String NAME = "Muon Loader";
	public static final String BRAND = "muon";
	/**
	 * The proper variant of {@link #BRAND}.
	 */
	public static final String CAPITAL_BRAND = "Muon";

	private MuonConstants() {}
}
