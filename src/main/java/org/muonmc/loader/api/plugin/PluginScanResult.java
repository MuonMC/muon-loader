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

package org.muonmc.loader.api.plugin;

import org.muonmc.loader.api.plugin.gui.PluginGuiTreeNode;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

/** Used as a result from {@link MuonLoaderPlugin#scanZip(java.nio.file.Path, boolean, PluginGuiTreeNode)} and
 * {@link MuonLoaderPlugin#scanUnknownFile(java.nio.file.Path, boolean, PluginGuiTreeNode)}. */
@MuonLoaderInternal(MuonLoaderInternalType.PLUGIN_API)
public enum PluginScanResult {
	/** Indicates that the plugin didn't find anything useful. */
	IGNORED,

	/** Indicates that the plugin has loaded the file as a mod. */
	FOUND;
}
