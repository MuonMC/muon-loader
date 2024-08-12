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

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.muonmc.loader.impl.metadata.qmj.InternalModMetadata;
import org.muonmc.loader.api.gui.MuonLoaderIcon;
import org.muonmc.loader.api.plugin.ModContainerExt;
import org.muonmc.loader.api.plugin.MuonPluginContext;
import org.muonmc.loader.impl.gui.GuiManagerImpl;
import org.muonmc.loader.impl.plugin.base.InternalModOptionBase;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public class BuiltinModOption extends InternalModOptionBase {

	public BuiltinModOption(MuonPluginContext pluginContext, InternalModMetadata meta, Path from, Path resourceRoot) {
		super(pluginContext, meta, from, GuiManagerImpl.ICON_JAVA_PACKAGE, resourceRoot, true, false);
	}

	@Override
	public MuonLoaderIcon modTypeIcon() {
		return GuiManagerImpl.ICON_JAVA_PACKAGE;
	}

	@Override
	public MuonLoaderIcon modCompleteIcon() {
		return GuiManagerImpl.ICON_JAVA_PACKAGE;
	}

	@Override
	protected String nameOfType() {
		return "builtin";
	}

	@Override
	public ModContainerExt convertToMod(Path transformedResourceRoot) {
		if (!transformedResourceRoot.equals(resourceRoot) && resourceRoot.getFileSystem() != FileSystems.getDefault()) {
			try {
				resourceRoot.getFileSystem().close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return new BuiltinModContainer(pluginContext, metadata, from, transformedResourceRoot, needsTransforming());
	}
}
