package org.muonmc.loader.impl.util.deprecated;

import net.fabricmc.api.EnvType;

import org.muonmc.loader.api.minecraft.Environment;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

/**
 * Converts {@link org.muonmc.loader.api.minecraft.Environment} to {@link net.fabricmc.api.EnvType}
 */
@SuppressWarnings("deprecation")
@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public final class EnvTypeUtil {
	private EnvTypeUtil() {}

	public static EnvType toEnvType(Environment environment) {
		switch (environment) {
		case CLIENT:
			return EnvType.CLIENT;
		case DEDICATED_SERVER:
			return EnvType.SERVER;
		}
		throw new IllegalArgumentException("Illegal instance of org.muonmc.loader.api.minecraft.Environment");
	}
}
