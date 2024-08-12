/*
 * Copyright 2016 FabricMC
 * Copyright 2022-2024 QuiltMC
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

package net.fabricmc.loader.impl;


import net.fabricmc.api.EnvType;
import net.fabricmc.loader.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.ObjectShare;
import net.fabricmc.loader.api.entrypoint.EntrypointContainer;
import net.fabricmc.loader.impl.entrypoint.EntrypointContainerImpl;

import org.jetbrains.annotations.Nullable;
import org.muonmc.loader.api.MuonLoader;
import org.muonmc.loader.api.entrypoint.EntrypointException;
import org.muonmc.loader.api.entrypoint.EntrypointUtil;
import org.muonmc.loader.api.game.minecraft.MinecraftMuonLoader;
import org.muonmc.loader.impl.util.deprecated.EnvTypeUtil;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Deprecated
public class FabricLoaderImpl extends FabricLoader {
	private FabricLoaderImpl() {}

	public static final FabricLoaderImpl INSTANCE = new FabricLoaderImpl();

	@Override
	public <T> List<T> getEntrypoints(String key, Class<T> type) {
		return MuonLoader.getEntrypoints(key, type);
	}

	@Override
	public <T> List<EntrypointContainer<T>> getEntrypointContainers(String key, Class<T> type) {
		List<org.muonmc.loader.api.entrypoint.EntrypointContainer<T>> from = MuonLoader.getEntrypointContainers(key, type);
		List<EntrypointContainer<T>> out = new ArrayList<>(from.size());
		try {
			for (org.muonmc.loader.api.entrypoint.EntrypointContainer<T> c : from) {
				out.add(new EntrypointContainerImpl<>(c));
			}
			return out;
		} catch (EntrypointException e) {
			throw new net.fabricmc.loader.api.EntrypointException(e.getKey(), e);
		}
	}

	@Override
	public <T> void invokeEntrypoints(String key, Class<T> type, Consumer<? super T> invoker) {
		EntrypointUtil.invoke(key, type, invoker);
	}

	@Override
	public ObjectShare getObjectShare() {
		return MuonLoader.getObjectShare();
	}

	@Override
	public MappingResolver getMappingResolver() {
		return new MappingResolverImpl(MuonLoader.getMappingResolver());
	}

	@Override
	public Optional<ModContainer> getModContainer(String id) {
		return MuonLoader.getModContainer(id).map(ModContainerImpl::new);
	}

	@Override
	public Optional<ModContainer> quilt_getModContainer(Class<?> clazz) {
		return MuonLoader.getModContainer(clazz).map(ModContainerImpl::new);
	}

	@Override
	public Collection<ModContainer> getAllMods() {
		Collection<ModContainer> out = new ArrayList<>();
		for (org.muonmc.loader.api.ModContainer mc : MuonLoader.getAllMods()) {
			out.add(new ModContainerImpl(mc));
		}
		return Collections.unmodifiableCollection(out);
	}

	@Override
	public boolean isModLoaded(String id) {
		return MuonLoader.isModLoaded(id);
	}

	@Override
	public boolean isDevelopmentEnvironment() {
		return MuonLoader.isDevelopmentEnvironment();
	}

	@Override
	public EnvType getEnvironmentType() {
		return EnvTypeUtil.toEnvType(MinecraftMuonLoader.getEnvironmentType());
	}

	@Override
	public @Nullable Object getGameInstance() {
		return MuonLoader.getGameInstance();
	}

	@Override
	public Path getGameDir() {
		return MuonLoader.getGameDir();
	}

	@Override
	@Deprecated
	public File getGameDirectory() {
		Path gameDir = getGameDir();
		return gameDir == null ? null : gameDir.toFile();
	}

	@Override
	public Path getConfigDir() {
		return MuonLoader.getConfigDir();
	}

	@Override
	@Deprecated
	public File getConfigDirectory() {
		Path configDir = getConfigDir();
		return configDir == null ? null : configDir.toFile();
	}

	@Override
	public String[] getLaunchArguments(boolean sanitize) {
		return MuonLoader.getLaunchArguments(sanitize);
	}

	/**
	 * Provides singleton for static init assignment regardless of load order.
	 */
	@Deprecated
	public static class InitHelper {
		private static FabricLoaderImpl instance;

		public static FabricLoaderImpl get() {
			if (instance == null) instance = new FabricLoaderImpl();

			return instance;
		}
	}
}
