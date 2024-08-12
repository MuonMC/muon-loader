/*
 * Copyright 2023, 2024 QuiltMC
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

package org.muonmc.loader.impl.filesystem;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;

import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public class QuiltUnifiedFileSystemProvider extends QuiltMapFileSystemProvider<MuonUnifiedFileSystem, MuonUnifiedPath> {
	public QuiltUnifiedFileSystemProvider() {}

	public static final String SCHEME = "quilt.ufs";

	static final String READ_ONLY_EXCEPTION = "This FileSystem is read-only";
	static final QuiltFSP<MuonUnifiedFileSystem> PROVIDER = new QuiltFSP<>(SCHEME);

	public static QuiltUnifiedFileSystemProvider instance() {
		for (FileSystemProvider provider : FileSystemProvider.installedProviders()) {
			if (provider instanceof QuiltUnifiedFileSystemProvider) {
				return (QuiltUnifiedFileSystemProvider) provider;
			}
		}
		throw new IllegalStateException("Unable to load QuiltUnifiedFileSystemProvider via services!");
	}

	@Override
	public String getScheme() {
		return SCHEME;
	}

	@Override
	protected QuiltFSP<MuonUnifiedFileSystem> quiltFSP() {
		return PROVIDER;
	}

	@Override
	protected Class<MuonUnifiedFileSystem> fileSystemClass() {
		return MuonUnifiedFileSystem.class;
	}

	@Override
	protected Class<MuonUnifiedPath> pathClass() {
		return MuonUnifiedPath.class;
	}

	@Override
	public MuonUnifiedPath getPath(URI uri) {
		return PROVIDER.getFileSystem(uri).root.resolve(uri.getPath());
	}

	@Override
	public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
		throw new IOException("Only direct creation is supported");
	}
}
