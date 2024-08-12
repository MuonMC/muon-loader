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
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.NotLinkException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

import org.jetbrains.annotations.Nullable;
import org.muonmc.loader.api.CachedFileSystem;
import org.muonmc.loader.api.ExtendedFileSystem;
import org.muonmc.loader.api.MountOption;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

/** General-purpose {@link FileSystem}, used when building the transform cache. Also intended to replace the various
 * zip/memory file systems currently in use. */
@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public class MuonUnifiedFileSystem extends MuonMapFileSystem<MuonUnifiedFileSystem, MuonUnifiedPath> implements ExtendedFileSystem {

	private boolean readOnly = false;

	public MuonUnifiedFileSystem(String name, boolean uniqueify) {
		super(MuonUnifiedFileSystem.class, MuonUnifiedPath.class, name, uniqueify);
		addEntryAndParentsUnsafe(new QuiltUnifiedEntry.QuiltUnifiedFolderWriteable(root));
	}

	@Override
	protected boolean startWithConcurrentMap() {
		return true;
	}

	@Override
	MuonUnifiedPath createPath(@Nullable MuonUnifiedPath parent, String name) {
		return new MuonUnifiedPath(this, parent, name);
	}

	@Override
	public QuiltUnifiedFileSystemProvider provider() {
		return QuiltUnifiedFileSystemProvider.instance();
	}

	/** Disallows all modification. */
	@Override
	public void switchToReadOnly() {
		super.switchToReadOnly();
		readOnly = true;
	}

	@Override
	public boolean isPermanentlyReadOnly() {
		return readOnly;
	}

	@Override
	public void close() throws IOException {

	}

	@Override
	public boolean isOpen() {
		return true;
	}

	@Override
	public boolean isReadOnly() {
		return isPermanentlyReadOnly();
	}

	@Override
	public Path copyOnWrite(Path source, Path target, CopyOption... options) throws IOException {
		FileSystem srcFS = source.getFileSystem();
		if (srcFS instanceof CachedFileSystem) {
			CachedFileSystem cached = (CachedFileSystem) srcFS;
			if (!cached.isPermanentlyReadOnly()) {
				return copy(source, target, options);
			}
		} else {
			return copy(source, target, options);
		}
		MuonUnifiedPath dst = provider().toAbsolutePath(target);
		boolean canExist = false;

		for (CopyOption option : options) {
			if (option == StandardCopyOption.REPLACE_EXISTING) {
				canExist = true;
			}
		}

		synchronized (this) {
			if (canExist) {
				provider().deleteIfExists(dst);
			} else if (getEntry(dst) != null) {
				throw new FileAlreadyExistsException(dst.toString());
			}
			addEntryRequiringParent(new QuiltUnifiedEntry.QuiltUnifiedCopyOnWriteFile(dst, source));
		}
		return dst;
	}

	@Override
	public Path mount(Path source, Path target, MountOption... options) throws IOException {
		MuonUnifiedPath dst = provider().toAbsolutePath(target);

		boolean canExist = false;
		boolean readOnly = false;
		boolean copyOnWrite = false;

		for (MountOption option : options) {
			switch (option) {
				case REPLACE_EXISTING: {
					canExist = true;
					break;
				}
				case COPY_ON_WRITE: {
					copyOnWrite = true;
					break;
				}
				case READ_ONLY: {
					readOnly = true;
					break;
				}
				default: {
					throw new IllegalStateException("Unknown MountOption " + option);
				}
			}
		}

		if (copyOnWrite && readOnly) {
			throw new IllegalArgumentException("Can't specify both READ_ONLY and COPY_ON_WRITE : " + Arrays.toString(options));
		}

		synchronized (this) {
			QuiltUnifiedEntry dstEntry = getEntry(dst);

			if (canExist) {
				provider().deleteIfExists(dst);
			} else if (dstEntry != null) {
				throw new FileAlreadyExistsException(dst.toString());
			}

			if (copyOnWrite) {
				dstEntry = new QuiltUnifiedEntry.QuiltUnifiedCopyOnWriteFile(dst, source);
			} else {
				dstEntry = new QuiltUnifiedEntry.QuiltUnifiedMountedFile(dst, source, readOnly);
			}
			addEntryRequiringParent(dstEntry);
			return dst;
		}
	}

	@Override
	public boolean isMountedFile(Path file) {
		return getEntry(file) instanceof QuiltUnifiedEntry.QuiltUnifiedMountedFile;
	}

	@Override
	public boolean isCopyOnWrite(Path file) {
		return getEntry(file) instanceof QuiltUnifiedEntry.QuiltUnifiedCopyOnWriteFile;
	}

	@Override
	public Path readMountTarget(Path file) throws IOException {
		QuiltUnifiedEntry entry = getEntry(file);
		if (entry instanceof QuiltUnifiedEntry.QuiltUnifiedMountedFile) {
			return ((QuiltUnifiedEntry.QuiltUnifiedMountedFile) entry).to;
		} else {
			throw new NotLinkException(file.toString() + " is not a mounted file!");
		}
	}
}
