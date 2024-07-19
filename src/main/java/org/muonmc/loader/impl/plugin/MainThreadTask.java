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

import java.nio.file.Path;

import org.muonmc.loader.impl.gui.MuonStatusNode;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

/** A task that must be completed by the main quilt thread. */
@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
abstract class MainThreadTask {

	abstract void execute(MuonPluginManagerImpl manager);

	static final class ScanModFolderTask extends MainThreadTask {
		final Path folder;
		final String pluginSrc;

		public ScanModFolderTask(Path folder, String pluginSrc) {
			this.folder = folder;
			this.pluginSrc = pluginSrc;
		}

		@Override
		void execute(MuonPluginManagerImpl manager) {
			manager.scanModFolder(folder, pluginSrc);
		}
	}

	static final class ScanFolderAsModTask extends MainThreadTask {
		final Path folder;
		final ModLocationImpl location;
		final MuonStatusNode guiNode;

		public ScanFolderAsModTask(Path folder, ModLocationImpl location, MuonStatusNode guiNode) {
			this.folder = folder;
			this.location = location;
			this.guiNode = guiNode;
		}

		@Override
		void execute(MuonPluginManagerImpl manager) {
			manager.scanFolderAsMod(folder, location, guiNode);
		}
	}

	static final class ScanZipTask extends MainThreadTask {
		final Path zipFile;
		final Path zipRoot;
		final ModLocationImpl location;
		final MuonStatusNode guiNode;

		public ScanZipTask(Path zipFile, Path zipRoot, ModLocationImpl location, MuonStatusNode guiNode) {
			this.zipFile = zipFile;
			this.zipRoot = zipRoot;
			this.location = location;
			this.guiNode = guiNode;
		}

		@Override
		void execute(MuonPluginManagerImpl manager) {
			manager.scanZip(zipFile, zipRoot, location, guiNode);
		}
	}

	static final class ScanUnknownFileTask extends MainThreadTask {
		final Path file;
		final ModLocationImpl location;
		final MuonStatusNode guiNode;

		public ScanUnknownFileTask(Path file, ModLocationImpl location, MuonStatusNode guiNode) {
			this.file = file;
			this.location = location;
			this.guiNode = guiNode;
		}

		@Override
		void execute(MuonPluginManagerImpl manager) {
			manager.scanUnknownFile(file, location, guiNode);
		}
	}
}
