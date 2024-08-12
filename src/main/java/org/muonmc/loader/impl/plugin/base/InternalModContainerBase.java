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

package org.muonmc.loader.impl.plugin.base;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.muonmc.loader.api.plugin.ModContainerExt;
import org.muonmc.loader.api.plugin.ModMetadataExt;
import org.muonmc.loader.api.plugin.MuonPluginContext;
import org.muonmc.loader.api.plugin.MuonPluginManager;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public abstract class InternalModContainerBase implements ModContainerExt {

	private final String pluginId;
	private final ModMetadataExt metadata;
	private final Path resourceRoot;
	private final List<List<Path>> sourcePaths;

	public InternalModContainerBase(MuonPluginContext pluginContext, ModMetadataExt metadata, Path from, Path resourceRoot) {
		this.pluginId = pluginContext.pluginId();
		this.metadata = metadata;
		this.resourceRoot = resourceRoot;

		sourcePaths = pluginContext.manager().convertToSourcePaths(from);
	}

	public static List<List<Path>> walkSourcePaths(MuonPluginContext pluginContext, Path from) {
		return walkSourcePaths(pluginContext.manager(), from);
	}

	public static List<List<Path>> walkSourcePaths(MuonPluginManager pluginManager, Path from) {

		if (from.getFileSystem() == FileSystems.getDefault()) {
			return Collections.singletonList(Collections.singletonList(from));
		}

		Path fromRoot = from.getFileSystem().getPath("/");
		Collection<Path> joinedPaths = pluginManager.getJoinedPaths(fromRoot);

		if (joinedPaths != null) {
			// TODO: Test joined paths!
			List<List<Path>> paths = new ArrayList<>();
			for (Path path : joinedPaths) {
				for (List<Path> upper : walkSourcePaths(pluginManager, path)) {
					List<Path> fullList = new ArrayList<>();
					fullList.addAll(upper);
					fullList.add(from);
					paths.add(Collections.unmodifiableList(fullList));
				}
			}
			return Collections.unmodifiableList(paths);
		}

		Path parent = pluginManager.getParent(fromRoot);
		if (parent == null) {
			// That's not good
			return Collections.singletonList(Collections.singletonList(from));
		} else {
			List<List<Path>> paths = new ArrayList<>();
			for (List<Path> upper : walkSourcePaths(pluginManager, parent)) {
				List<Path> fullList = new ArrayList<>();
				fullList.addAll(upper);
				fullList.add(from);
				paths.add(Collections.unmodifiableList(fullList));
			}
			return Collections.unmodifiableList(paths);
		}
	}

	@Override
	public String pluginId() {
		return pluginId;
	}

	@Override
	public ModMetadataExt metadata() {
		return metadata;
	}

	@Override
	public Path rootPath() {
		return resourceRoot;
	}

	@Override
	public List<List<Path>> getSourcePaths() {
		return sourcePaths;
	}

	@Override
	public boolean shouldAddToQuiltClasspath() {
		return true;
	}
}
