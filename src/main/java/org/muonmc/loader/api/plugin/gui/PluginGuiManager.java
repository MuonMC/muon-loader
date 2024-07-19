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

package org.muonmc.loader.api.plugin.gui;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Map;

import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;
import org.muonmc.loader.api.gui.MuonLoaderGui;
import org.muonmc.loader.api.gui.MuonLoaderIcon;

/**
 * @deprecated Replaced with {@link MuonLoaderGui}, kept only until we clear out all uses of this from quilt's codebase.
 */
@MuonLoaderInternal(value = MuonLoaderInternalType.PLUGIN_API, replacements = MuonLoaderGui.class)
@Deprecated
public interface PluginGuiManager {

	// Icons

	default MuonLoaderIcon allocateIcon(BufferedImage image) {
		return allocateIcon(Collections.singletonMap(image.getWidth(), image));
	}

	MuonLoaderIcon allocateIcon(Map<Integer, BufferedImage> image);

	// Builtin icons

	MuonLoaderIcon iconFolder();

	MuonLoaderIcon iconUnknownFile();

	MuonLoaderIcon iconTextFile();

	MuonLoaderIcon iconZipFile();

	MuonLoaderIcon iconJarFile();

	MuonLoaderIcon iconJsonFile();

	MuonLoaderIcon iconJavaClassFile();

	MuonLoaderIcon iconPackage();

	MuonLoaderIcon iconJavaPackage();

	MuonLoaderIcon iconDisabled();

	MuonLoaderIcon iconQuilt();

	MuonLoaderIcon iconFabric();

	MuonLoaderIcon iconTick();

	MuonLoaderIcon iconCross();

	MuonLoaderIcon iconLevelFatal();

	MuonLoaderIcon iconLevelError();

	MuonLoaderIcon iconLevelWarn();

	MuonLoaderIcon iconLevelConcern();

	MuonLoaderIcon iconLevelInfo();
}
