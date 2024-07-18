/*
 * Copyright 2022, 2023 QuiltMC
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

import java.util.Map;

import org.muonmc.loader.api.LoaderValue;
import org.muonmc.loader.api.plugin.QuiltLoaderPlugin;
import org.muonmc.loader.api.plugin.QuiltPluginContext;
import org.muonmc.loader.impl.util.QuiltLoaderInternal;
import org.muonmc.loader.impl.util.QuiltLoaderInternalType;

@QuiltLoaderInternal(QuiltLoaderInternalType.NEW_INTERNAL)
public abstract class BuiltinQuiltPlugin implements QuiltLoaderPlugin {
	private QuiltPluginContext context;

	@Override
	public void load(QuiltPluginContext context, Map<String, LoaderValue> previousData) {
		this.context = context;
	}

	@Override
	public void unload(Map<String, LoaderValue> data) {
		throw new UnsupportedOperationException("Builtin plugins cannot be unloaded!");
	}

	public QuiltPluginContext context() {
		return context;
	}
}
