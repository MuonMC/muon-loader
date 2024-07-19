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

package org.muonmc.loader.impl.filesystem;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;
import org.muonmc.loader.api.CachedFileSystem;
import org.muonmc.loader.api.FasterFiles;

/** A {@link FileSystem} that exposes multiple {@link Path}s in a single {@link FileSystem}. */
@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public class MuonJoinedFileSystem extends MuonBaseFileSystem<MuonJoinedFileSystem, MuonJoinedPath> implements CachedFileSystem {

	final Path[] from;
	final boolean[] shouldCloseFroms;
	/** True if every {@link Path} is from a {@link CachedFileSystem}. */
	final boolean allCached;
	boolean isOpen = true;

	public MuonJoinedFileSystem(String name, List<Path> from) {
		this(name, from, null);
	}

	public MuonJoinedFileSystem(String name, List<Path> from, List<Boolean> shouldClose) {
		super(MuonJoinedFileSystem.class, MuonJoinedPath.class, name, true);

		this.from = from.toArray(new Path[0]);
		this.shouldCloseFroms = new boolean[from.size()];
		for (int i = 0; i < shouldCloseFroms.length; i++) {
			shouldCloseFroms[i] = shouldClose != null && shouldClose.get(i);
		}
		boolean allCached = true;
		for (Path p : from) {
			if (!(p.getFileSystem() instanceof CachedFileSystem)) {
				allCached = false;
				break;
			}
		}
		this.allCached = allCached;
		QuiltJoinedFileSystemProvider.register(this);
	}

	@Override
	MuonJoinedPath createPath(@Nullable MuonJoinedPath parent, String name) {
		return new MuonJoinedPath(this, parent, name);
	}

	@Override
	public FileSystemProvider provider() {
		return QuiltJoinedFileSystemProvider.instance();
	}

	@Override
	public synchronized void close() throws IOException {
		if (isOpen) {
			isOpen = false;
			QuiltJoinedFileSystemProvider.closeFileSystem(this);
			for (int i = 0; i < from.length; i++) {
				if (shouldCloseFroms[i]) {
					from[i].getFileSystem().close();
				}
			}
		}
	}

	@Override
	public boolean isOpen() {
		return isOpen;
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public boolean isPermanentlyReadOnly() {
		if (!allCached) {
			return false;
		}

		for (Path p : from) {
			FileSystem fs = p.getFileSystem();
			if (fs instanceof CachedFileSystem) {
				if (!((CachedFileSystem) fs).isPermanentlyReadOnly()) {
					return false;
				}
			} else {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean exists(Path path, LinkOption... options) {
		MuonJoinedPath qjp = (MuonJoinedPath) path;
		for (int i = 0; i < from.length; i++) {
			Path backingPath = getBackingPath(i, qjp);
			if (FasterFiles.exists(backingPath, options)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Iterable<FileStore> getFileStores() {
		// TODO Auto-generated method stub
		throw new AbstractMethodError("// TODO: Implement this!");
	}

	@Override
	public Set<String> supportedFileAttributeViews() {
		Set<String> supported = new HashSet<>();
		for (Path path : from) {
			Set<String> set = path.getFileSystem().supportedFileAttributeViews();
			if (supported.isEmpty()) {
				supported.addAll(set);
			} else {
				supported.retainAll(set);
			}
		}
		return supported;
	}

	public int getBackingPathCount() {
		return from.length;
	}

	public Path getBackingPath(int index, MuonJoinedPath thisPath) {
		Path other = from[index];
		if (getSeparator().equals(other.getFileSystem().getSeparator())) {
			String thisPathStr = thisPath.toString();
			if (thisPathStr.startsWith("/")) {
				thisPathStr = thisPathStr.substring(1);
			}
			return other.resolve(thisPathStr);
		} else {
			for (String segment : thisPath.names()) {
				other = other.resolve(segment);
			}
			return other;
		}
	}
}
