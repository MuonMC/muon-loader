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

package org.muonmc.loader.impl.plugin.fabric;

import java.io.IOException;
import java.nio.file.Path;

import org.muonmc.loader.impl.metadata.FabricLoaderModMetadata;
import org.muonmc.loader.impl.metadata.NestedJarEntry;
import org.muonmc.loader.api.FasterFiles;
import org.muonmc.loader.api.MuonLoader;
import org.muonmc.loader.api.gui.MuonDisplayedError;
import org.muonmc.loader.api.gui.MuonLoaderGui;
import org.muonmc.loader.api.gui.MuonLoaderIcon;
import org.muonmc.loader.api.gui.MuonLoaderText;
import org.muonmc.loader.api.plugin.ModLocation;
import org.muonmc.loader.api.plugin.gui.PluginGuiTreeNode;
import org.muonmc.loader.api.plugin.gui.PluginGuiTreeNode.SortOrder;
import org.muonmc.loader.api.plugin.gui.PluginGuiTreeNode.WarningLevel;
import org.muonmc.loader.api.plugin.solver.ModLoadOption;
import org.muonmc.loader.impl.fabric.metadata.FabricModMetadataReader;
import org.muonmc.loader.impl.fabric.metadata.ParseMetadataException;
import org.muonmc.loader.impl.plugin.BuiltinMuonPlugin;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;
import org.muonmc.loader.impl.util.log.Log;
import org.muonmc.loader.impl.util.log.LogCategory;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public class StandardFabricPlugin extends BuiltinMuonPlugin {

	@Override
	public ModLoadOption[] scanZip(Path root, ModLocation location, PluginGuiTreeNode guiNode) throws IOException {

		Path parent = context().manager().getParent(root);

		if (!parent.getFileName().toString().endsWith(".jar")) {
			return null;
		}

		return scan0(root, MuonLoaderGui.iconJarFile(), location, true, guiNode);
	}

	@Override
	public ModLoadOption[] scanFolder(Path folder, ModLocation location, PluginGuiTreeNode guiNode) throws IOException {
		return scan0(folder, MuonLoaderGui.iconFolder(), location, false, guiNode);
	}

	private ModLoadOption[] scan0(Path root, MuonLoaderIcon fileIcon, ModLocation location, boolean isZip, PluginGuiTreeNode guiNode) throws IOException {
		Path fmj = root.resolve("fabric.mod.json");
		if (!FasterFiles.isRegularFile(fmj)) {
			return null;
		}

		try {
			FabricLoaderModMetadata meta = FabricModMetadataReader.parseMetadata(fmj);

			Path from = root;
			if (isZip) {
				from = context().manager().getParent(root);
			}

			jars: for (NestedJarEntry jarEntry : meta.getJars()) {
				String jar = jarEntry.getFile();
				Path inner = root;
				for (String part : jar.split("/")) {
					if ("..".equals(part)) {
						continue jars;
					}
					inner = inner.resolve(part);
				}

				if (inner == from) {
					continue;
				}

				if (!FasterFiles.exists(inner)) {
					Log.warn(LogCategory.DISCOVERY, "Didn't find nested jar " + inner + " in " + context().manager().describePath(from));
					PluginGuiTreeNode missingJij = guiNode.addChild(MuonLoaderText.of(inner.toString()), SortOrder.ALPHABETICAL_ORDER);
					missingJij.mainIcon(MuonLoaderGui.iconJarFile());
					missingJij.addChild(MuonLoaderText.translate("fabric.jar_in_jar.missing"))//
						.setDirectLevel(WarningLevel.CONCERN);
					continue;
				}

				PluginGuiTreeNode jarNode = guiNode.addChild(MuonLoaderText.of(jar), SortOrder.ALPHABETICAL_ORDER);
				context().addFileToScan(inner, jarNode, false);
			}

			boolean mandatory = location.isDirect();
			// a mod needs to be remapped if we are in a development environment, and the mod
			// did not come from the classpath
			boolean requiresRemap = !location.onClasspath() && MuonLoader.isDevelopmentEnvironment();
			return new ModLoadOption[] { new FabricModOption(context(), meta, from, fileIcon, root, mandatory, requiresRemap) };
		} catch (ParseMetadataException parse) {
			MuonLoaderText title = MuonLoaderText.translate("gui.text.invalid_metadata.title", "fabric.mod.json", parse.getMessage());
			MuonDisplayedError error = context().reportError(title);
			String describedPath = context().manager().describePath(fmj);
			error.appendReportText("Invalid 'fabric.mod.json' metadata file:" + describedPath);
			error.appendDescription(MuonLoaderText.translate("gui.text.invalid_metadata.desc.0", describedPath));
			error.appendThrowable(parse);
			context().manager().getRealContainingFile(root).ifPresent(real ->
					error.addFileViewButton(MuonLoaderText.translate("button.view_file"), real)
					.icon(MuonLoaderGui.iconJarFile().withDecoration(MuonLoaderGui.iconFabric()))
			);

			guiNode.addChild(MuonLoaderText.translate("gui.text.invalid_metadata", parse.getMessage()))//TODO: translate
				.setError(parse, error);
			return null;
		}
	}
}
