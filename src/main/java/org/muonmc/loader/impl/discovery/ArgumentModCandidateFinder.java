/*
 * Copyright 2016 FabricMC
 * Copyright 2022-2023 QuiltMC
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

package org.muonmc.loader.impl.discovery;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.muonmc.loader.api.FasterFiles;
import org.muonmc.loader.api.gui.MuonDisplayedError;
import org.muonmc.loader.api.gui.MuonLoaderText;
import org.muonmc.loader.api.plugin.MuonPluginContext;
import org.muonmc.loader.api.plugin.gui.PluginGuiTreeNode;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;
import org.muonmc.loader.impl.util.log.Log;
import org.muonmc.loader.impl.util.log.LogCategory;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public class ArgumentModCandidateFinder {

	public static void addMods(MuonPluginContext ctx, String list, String source) {
		PluginGuiTreeNode argModsNode = ctx.manager().getRootGuiNode().addChild(MuonLoaderText.translate("gui.text.arg_mods"));
		for (String pathStr : list.split(File.pathSeparator)) {
			if (pathStr.isEmpty()) continue;

			if (pathStr.startsWith("@")) {
				Path path = Paths.get(pathStr.substring(1));

				if (!Files.isRegularFile(path)) {
					Log.warn(
						LogCategory.DISCOVERY, "Skipping missing/invalid %s provided mod list file %s", source, path
					);
					continue;
				}

				try (BufferedReader reader = Files.newBufferedReader(path)) {
					String fileSource = String.format("%s file %s", source, path);
					String line;

					while ((line = reader.readLine()) != null) {
						line = line.trim();
						if (line.isEmpty()) continue;

						addMod(ctx, line, source, fileSource, argModsNode);
					}
				} catch (IOException e) {
					throw new RuntimeException(
						String.format("Error reading %s provided mod list file %s", source, path), e
					);
				}
			} else {
				addMod(ctx, pathStr, source, null, argModsNode);
			}
		}
	}

	private static void addMod(MuonPluginContext ctx, String pathStr, String original, String source, PluginGuiTreeNode argModsNode) {

		final boolean folder = pathStr.endsWith(File.separator + "*") || pathStr.endsWith("/*");

		if (folder) {
			pathStr = pathStr.substring(0, pathStr.length() - 2);
		}

		Path path = Paths.get(pathStr).toAbsolutePath().normalize();

		if (!FasterFiles.exists(path)) { // missing
			MuonDisplayedError error = ctx.reportError(
				MuonLoaderText.translate("error.arg_mods.missing.title", path.getFileName())
			);
			if (source == null) {
				error.appendDescription(MuonLoaderText.translate("error.arg_mods.missing.desc", original, path));
			} else {
				error.appendDescription(
					MuonLoaderText.translate("error.arg_mods.missing.by.desc", original, source, path)
				);
			}
			error.appendReportText("The file " + path + " is missing!");
			error.appendReportText(" (It is specified by " + original + ")");
			if (source != null) {
				error.appendReportText(" (Inside the file " + source + ")");
			}
			return;
		}

		if (folder) {
			if (Files.isDirectory(path)) {
				ctx.addFolderToScan(path);
				return;
			}
			MuonDisplayedError error = ctx.reportError(
				MuonLoaderText.translate("error.arg_mods.not_folder.title", path.getFileName())
			);
			if (source == null) {
				error.appendDescription(MuonLoaderText.translate("error.arg_mods.not_folder.desc", original, path));
			} else {
				error.appendDescription(
					MuonLoaderText.translate("error.arg_mods.not_folder.by.desc", original, source, path)
				);
			}
			error.appendReportText("The folder " + path + " is missing!");
			error.appendReportText(" (It is specified by " + original + ")");
			if (source != null) {
				error.appendReportText(" (Inside the file " + source + ")");
			}
		} else {
			ctx.addFileToScan(path, argModsNode.addChild(MuonLoaderText.of(pathStr)), true);
		}
	}
}
