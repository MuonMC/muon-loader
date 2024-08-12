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

import org.muonmc.loader.impl.MuonConstants;
import org.muonmc.loader.impl.metadata.qmj.InternalModMetadata;
import org.muonmc.loader.impl.plugin.base.InternalModOptionBase;
import org.muonmc.loader.api.gui.MuonLoaderIcon;
import org.muonmc.loader.api.plugin.ModContainerExt;
import org.muonmc.loader.api.plugin.MuonPluginContext;
import org.muonmc.loader.impl.gui.GuiManagerImpl;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public class MuonModOption extends InternalModOptionBase {

	public MuonModOption(
			MuonPluginContext pluginContext, InternalModMetadata meta, Path from, MuonLoaderIcon fileIcon,
		Path resourceRoot, boolean mandatory, boolean requiresRemap) {

		super(pluginContext, meta, from, fileIcon, resourceRoot, mandatory, requiresRemap);
	}

	@Override
	public MuonLoaderIcon modTypeIcon() {
		return GuiManagerImpl.ICON_MUON;
	}

	@Override
	public ModContainerExt convertToMod(Path transformedResourceRoot) {
		return new MuonModContainer(pluginContext, metadata, from, transformedResourceRoot);
	}

	@Override
	protected String nameOfType() {
		return MuonConstants.BRAND;
	}
}
