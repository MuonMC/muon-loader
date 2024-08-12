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

import org.muonmc.loader.api.plugin.MuonLoaderPlugin;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
class BuiltinPluginContext extends BasePluginContext {

	final MuonLoaderPlugin plugin;

	public BuiltinPluginContext(MuonPluginManagerImpl manager, String pluginId, MuonLoaderPlugin plugin) {
		super(manager, pluginId);
		this.plugin = plugin;
	}

	@Override
	public Path pluginPath() {
		throw new UnsupportedOperationException("Builtin plugins don't support pluginPath()");
	}

	@Override
	public MuonLoaderPlugin plugin() {
		return plugin;
	}
}
