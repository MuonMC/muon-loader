/*
 * Copyright 2016 FabricMC
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
package org.muonmc.loader.impl.game.minecraft;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.ModInitializer;

import org.muonmc.loader.impl.MuonLoaderImpl;
import org.muonmc.loader.impl.entrypoint.EntrypointUtils;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;
import org.muonmc.loader.impl.util.log.Log;
import org.muonmc.loader.impl.util.log.LogCategory;

import java.io.File;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL_HOOK)
public final class Hooks {
	public static final String INTERNAL_NAME = Hooks.class.getName().replace('.', '/');

	public static String appletMainClass;

	public static final String MUON = "muon";
	public static final String VANILLA = "vanilla";

	public static String insertBranding(final String brand) {
		if (brand == null || brand.isEmpty()) {
			Log.warn(LogCategory.GAME_PROVIDER, "Null or empty branding found!", new IllegalStateException());
			return MUON;
		}

		return VANILLA.equals(brand) ? MUON : brand + ',' + MUON;
	}

	public static void startClient(File runDir, Object gameInstance) {
		if (runDir == null) {
			runDir = new File(".");
		}

		MuonLoaderImpl.INSTANCE.prepareModInit(runDir.toPath(), gameInstance);
		EntrypointUtils.invoke("main", ModInitializer.class, it -> it.onInitialize());
		EntrypointUtils.invoke("client", ClientModInitializer.class, it -> it.onInitializeClient());
	}

	public static void startServer(File runDir, Object gameInstance) {
		if (runDir == null) {
			runDir = new File(".");
		}

		MuonLoaderImpl.INSTANCE.prepareModInit(runDir.toPath(), gameInstance);
		EntrypointUtils.invoke("main", ModInitializer.class, it -> it.onInitialize());
		EntrypointUtils.invoke("server", DedicatedServerModInitializer.class, it -> it.onInitializeServer());
	}

	/**
	 * @see MuonLoaderImpl#setGameInstance(Object)
	 */
	@Deprecated
	public static void setGameInstance(Object gameInstance) {
		MuonLoaderImpl.INSTANCE.setGameInstance(gameInstance);
	}
}
