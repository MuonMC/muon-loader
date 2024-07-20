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
