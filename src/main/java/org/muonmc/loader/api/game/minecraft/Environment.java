package org.muonmc.loader.api.game.minecraft;

import org.muonmc.loader.api.game.LogicalSide;

/**
 * A class that describes the type of environment.
 *
 * @see ClientOnly
 * @see DedicatedServerOnly
 */
public enum Environment {
	CLIENT(LogicalSide.CLIENT),
	DEDICATED_SERVER(LogicalSide.DEDICATED_SERVER);

	/**
	 * The logical side that this environment corresponds to.
	 */
	private final LogicalSide logicalSide;

	Environment(LogicalSide logicalSide) {
		this.logicalSide = logicalSide;
	}

	/**
	 * @inheritDoc
	 */
	public LogicalSide getLogicalSide() {
		return this.logicalSide;
	}
}
