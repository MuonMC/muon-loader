package org.muonmc.loader.impl.util.deprecated;

import net.fabricmc.api.EnvType;

import org.muonmc.loader.api.game.minecraft.Environment;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

/**
 * Converts {@link Environment} to {@link net.fabricmc.api.EnvType}
 */
@SuppressWarnings("deprecation")
@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public final class EnvTypeUtil {
	private EnvTypeUtil() {}

	public static EnvType toEnvType(Environment environment) {
		if (environment.equals(Environment.CLIENT)) {
			return EnvType.CLIENT;
		} else if (environment.equals(Environment.DEDICATED_SERVER)) {
			return EnvType.SERVER;
		}
		throw new IllegalArgumentException("Illegal instance of org.muonmc.loader.api.minecraft.Environment");
	}
}
