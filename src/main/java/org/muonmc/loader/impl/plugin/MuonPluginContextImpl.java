/*
 * Copyright 2022, 2023, 2024 QuiltMC
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

package org.muonmc.loader.impl.plugin;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.muonmc.loader.api.LoaderValue;
import org.muonmc.loader.api.plugin.LoaderValueFactory;
import org.muonmc.loader.api.plugin.ModMetadataExt.ModPlugin;
import org.muonmc.loader.api.plugin.MuonLoaderPlugin;
import org.muonmc.loader.api.plugin.solver.ModLoadOption;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public class MuonPluginContextImpl extends BasePluginContext {

	final ModLoadOption optionFrom;
	final Path pluginPath;
	final MuonPluginClassLoader classLoader;
	final MuonLoaderPlugin plugin;

	public MuonPluginContextImpl(//
		MuonPluginManagerImpl manager, ModLoadOption from, Map<String, LoaderValue> previousData //
	) throws ReflectiveOperationException {

		super(manager, from.id());
		this.optionFrom = from;
		this.pluginPath = from.resourceRoot();

		ClassLoader parent = Thread.currentThread().getContextClassLoader();
		ModPlugin pluginMeta = from.metadata().plugin();
		if (pluginMeta == null) {
			throw new IllegalArgumentException("No plugin metadata!");
		}
		classLoader = new MuonPluginClassLoader(this, parent, pluginPath, pluginMeta);

		Class<?> cls = classLoader.loadClassDirectly(pluginMeta.pluginClass(), true);
		Object obj = cls.getDeclaredConstructor().newInstance();
		this.plugin = (MuonLoaderPlugin) obj;

		plugin.load(this, previousData);
	}

	@Override
	public MuonLoaderPlugin plugin() {
		return plugin;
	}

	@Override
	public Path pluginPath() {
		return pluginPath;
	}

	Map<String, LoaderValue> unload() {
		Map<String, LoaderValue> data = new HashMap<>();

		plugin.unload(data);

		// Just to ensure the resulting map is not empty
		data.put("quilt.plugin.reloaded", LoaderValueFactory.getFactory().bool(true));

		return data;
	}
}
