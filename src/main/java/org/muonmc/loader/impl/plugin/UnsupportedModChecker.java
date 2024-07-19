/*
 * Copyright 2023 QuiltMC
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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.SortedMap;

import org.objectweb.asm.ClassReader;
import org.muonmc.loader.api.FasterFiles;
import org.muonmc.loader.api.gui.MuonDisplayedError;
import org.muonmc.loader.api.gui.MuonLoaderGui;
import org.muonmc.loader.api.gui.MuonLoaderText;
import org.muonmc.loader.api.gui.MuonTreeNode;
import org.muonmc.loader.api.gui.MuonWarningLevel;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public class UnsupportedModChecker {

	static UnsupportedModDetails checkFolder(Path folder) throws IOException {
		return new UnknownMod();
	}

	static UnsupportedModDetails checkUnknownFile(Path file) throws IOException {
		return new UnknownMod();
	}

	static UnsupportedModDetails checkZip(Path zipFile, Path zipRoot) throws IOException {
		// (Neo)Forge check
		UnsupportedModDetails type = checkForForgeMod(zipRoot);
		if (type != null) {
			return type;
		}
		// ModLoader check
		type = checkForModloaderMod(zipRoot);
		if (type != null) {
			return type;
		}
		// TODO: Other checks!
		return new UnknownMod();
	}

	private static UnsupportedModDetails checkForModloaderMod(Path zipRoot) throws IOException {
		for (Path child : FasterFiles.getChildren(zipRoot)) {
			if (!FasterFiles.isRegularFile(child)) {
				continue;
			}
			String fileName = child.getFileName().toString();
			if (fileName.startsWith("mod_") && fileName.endsWith(".class") && !fileName.contains("$")) {
				try {
					ClassReader cr = new ClassReader(Files.readAllBytes(child));
					if ("BaseMod".equals(cr.getSuperName()) || "BaseModMP".equals(cr.getSuperName())) {
						return new RisugamisModLoaderMod();
					}
				} catch (IOException ignored) {
					// It's a bit odd, but if we can't read 
					continue;
				}
			}
		}
		return null;
	}

	private static UnsupportedModDetails checkForForgeMod(Path zipRoot) {
		// Older forge
		Path mcmodInfo = zipRoot.resolve("mcmod.info");
		if (FasterFiles.exists(mcmodInfo)) {
			return new UnsupportedForgeMod(false);
		}
		// Modern (neo)forge
		Path modsToml = zipRoot.resolve("META-INF/mods.toml");
		if (FasterFiles.exists(modsToml)) {
			boolean isNeoforge = false;
			try (BufferedReader br = Files.newBufferedReader(modsToml)) {
				String line;
				while ((line = br.readLine()) != null) {
					line = line.trim();
					int idxModId = line.indexOf("modId");
					int idxNeoforge = line.indexOf("\"neoforge\"");
					if (idxModId == 0 && idxNeoforge > 0) {
						// Just assume it's a dependency
						isNeoforge = true;
						break;
					}
				}
			} catch (IOException ignored) {
				// It's okay if we can't read it
			}
			return new UnsupportedForgeMod(isNeoforge);
		}
		return null;
	}

	@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
	public enum UnsupportedType {
		UNKNOWN("unknown"),
		RISUGAMIS_MODLOADER("risugamis_modloader") {
			@Override
			MuonDisplayedError createMessage(MuonPluginManagerImpl manager, SortedMap<String, UnsupportedModDetails> files) {
				MuonDisplayedError message = super.createMessage(manager, files);
				if (!manager.pluginsById.containsKey("rgml-quilt")) {
					message.addOpenLinkButton(MuonLoaderText.of("Check RGML Quilt"), "https://github.com/sschr15/rgml-quilt");
				}
				return message;
			}
		},
		FORGE("forge"),
		NEOFORGE("neoforge");

		final String type;

		private UnsupportedType(String type) {
			this.type = type;
		}

		MuonDisplayedError createMessage(MuonPluginManagerImpl manager, SortedMap<String, UnsupportedModDetails> files) {
			String key = "unsupported_mod." + type;
			MuonDisplayedError message = MuonLoaderGui.createError();
			message.title(MuonLoaderText.translate(key + ".title", files.size()));
			message.setIcon(MuonLoaderGui.iconLevelWarn());
			message.appendDescription(MuonLoaderText.translate(key + ".desc"), MuonLoaderText.of(" "));
			for (String file : files.keySet()) {
				message.appendDescription(MuonLoaderText.of(file));
			}
			return message;
		}
	}

	@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
	public static abstract class UnsupportedModDetails {
		// This is a class rather than just folded into the enum above
		// to allow individual files to have more specific description.
		final UnsupportedType type;

		UnsupportedModDetails(UnsupportedType type) {
			this.type = type;
		}

		void addToFilesNode(MuonTreeNode guiNode) {
			String key = "unsupported_mod." + type.type + ".guiNode";
			guiNode.addChild(MuonLoaderText.translate(key)).level(MuonWarningLevel.WARN);
		}
	}

	@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
	public static final class UnknownMod extends UnsupportedModDetails {
		UnknownMod() {
			super(UnsupportedType.UNKNOWN);
		}
	}

	@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
	public static final class RisugamisModLoaderMod extends UnsupportedModDetails {
		RisugamisModLoaderMod() {
			super(UnsupportedType.RISUGAMIS_MODLOADER);
		}
	}

	@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
	public static final class UnsupportedForgeMod extends UnsupportedModDetails {
		UnsupportedForgeMod(boolean neoforge) {
			super(neoforge ? UnsupportedType.NEOFORGE : UnsupportedType.FORGE);
		}
	}
}
