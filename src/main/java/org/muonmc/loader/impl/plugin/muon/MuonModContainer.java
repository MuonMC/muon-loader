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

package org.muonmc.loader.impl.plugin.muon;

import java.nio.file.Path;

import org.muonmc.loader.api.plugin.ModMetadataExt;
import org.muonmc.loader.api.plugin.MuonPluginContext;
import org.muonmc.loader.impl.MuonConstants;
import org.muonmc.loader.impl.plugin.base.InternalModContainerBase;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public class MuonModContainer extends InternalModContainerBase {

	public MuonModContainer(MuonPluginContext pluginContext, ModMetadataExt metadata, Path from, Path resourceRoot) {
		super(pluginContext, metadata, from, resourceRoot);
	}

	@Override
	public String modType() {
		return MuonConstants.CAPITAL_BRAND;
	}

	@Override
	public BasicSourceType getSourceType() {
		return BasicSourceType.NORMAL_MUON;
	}
}
