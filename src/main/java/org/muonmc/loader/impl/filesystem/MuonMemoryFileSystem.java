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

package org.muonmc.loader.impl.filesystem;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.muonmc.loader.api.CachedFileSystem;
import org.muonmc.loader.api.FasterFiles;
import org.muonmc.loader.impl.util.FileUtil;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public abstract class MuonMemoryFileSystem extends MuonMapFileSystem<MuonMemoryFileSystem, MuonMemoryPath> implements CachedFileSystem {

	private static final Set<String> FILE_ATTRS = Collections.singleton("basic");

	enum OpenState {
		OPEN,
		/** Used by {@link ReadWrite#replaceWithReadOnly()} when moving from one filesystem to another - since we need
		 * to remove the old file system from the provider while still being able to read it. */
		MOVING,
		CLOSED;
	}

	volatile OpenState openState = OpenState.OPEN;

	private MuonMemoryFileSystem(String name, boolean uniquify) {
		super(MuonMemoryFileSystem.class, MuonMemoryPath.class, name, uniquify);
		QuiltMemoryFileSystemProvider.PROVIDER.register(this);
	}

	@Override
	@NotNull
	MuonMemoryPath createPath(@Nullable MuonMemoryPath parent, String name) {
		return new MuonMemoryPath(this, parent, name);
	}

	@Override
	public QuiltMemoryFileSystemProvider provider() {
		return QuiltMemoryFileSystemProvider.instance();
	}

	@Override
	public synchronized void close() {
		if (openState == OpenState.OPEN) {
			openState = OpenState.CLOSED;
			QuiltMemoryFileSystemProvider.PROVIDER.closeFileSystem(this);
		}
	}

	synchronized void beginMove() {
		if (openState == OpenState.OPEN) {
			openState = OpenState.MOVING;
			QuiltMemoryFileSystemProvider.PROVIDER.closeFileSystem(this);
		}
	}

	synchronized void endMove() {
		if (openState == OpenState.MOVING) {
			openState = OpenState.CLOSED;
		}
	}

	@Override
	public boolean isOpen() {
		return openState != OpenState.CLOSED;
	}

	@Override
	public Set<String> supportedFileAttributeViews() {
		return FILE_ATTRS;
	}

	public BasicFileAttributes readAttributes(MuonMemoryPath qmp) throws IOException {
		checkOpen();
		QuiltUnifiedEntry entry = getEntry(qmp);

		if (entry != null) {
			return entry.createAttributes();
		} else {
			throw new NoSuchFileException(qmp.toString());
		}
	}

	public <V extends FileAttributeView> V getFileAttributeView(MuonMemoryPath qmp, Class<V> type) {
		if (type == BasicFileAttributeView.class) {
			BasicFileAttributeView view = new BasicFileAttributeView() {
				@Override
				public String name() {
					return "basic";
				}

				@Override
				public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime)
					throws IOException {
					// Unsupported
					// Since we don't need to throw we won't
				}

				@Override
				public BasicFileAttributes readAttributes() throws IOException {
					return MuonMemoryFileSystem.this.readAttributes(qmp);
				}
			};
			return (V) view;
		}

		return null;
	}

	static final class DirBuildState {

		final MuonMemoryPath folder;
		final List<MuonMemoryPath> children = new ArrayList<>();

		public DirBuildState(MuonMemoryPath folder) {
			this.folder = folder;
		}
	}

	public static final class ReadOnly extends MuonMemoryFileSystem implements ReadOnlyFileSystem {

		private static final int STAT_UNCOMPRESSED = 0;
		private static final int STAT_USED = 1;
		private static final int STAT_MEMORY = 2;

		private final int uncompressedSize, usedSize, memorySize;
		private QuiltMemoryFileStore.ReadOnly fileStore;
		private Iterable<FileStore> fileStoreItr;

		/** Creates a new read-only {@link FileSystem} that copies every file in the given directory.
		 *
		 * @param compress if true then all files will be stored in-memory compressed.
		 * @throws IOException if any of the files in the given path could not be read. */
		public ReadOnly(String name, boolean uniquify, Path from, boolean compress) throws IOException {
			super(name, uniquify);

			int[] stats = new int[3];
			stats[STAT_MEMORY] = 60;

			if (!FasterFiles.isDirectory(from)) {
				throw new IOException(from + " is not a directory!");
			}

			final Deque<DirBuildState> stack = new ArrayDeque<>();

			Files.walkFileTree(from, new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					Path relative = from.relativize(dir);

					if (stack.isEmpty()) {
						stack.push(new DirBuildState(root));
					} else {
						String pathName = relative.getFileName().toString();
						MuonMemoryPath path = stack.peek().folder.resolve(pathName);
						stack.peek().children.add(path);
						stats[STAT_MEMORY] += pathName.length() + 28;
						stack.push(new DirBuildState(path));
					}

					return super.preVisitDirectory(dir, attrs);
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					DirBuildState state = stack.peek();
					String fileName = file.getFileName().toString();
					stats[STAT_MEMORY] += fileName.length() + 28;
					MuonMemoryPath childPath = state.folder.resolve(fileName);
					state.children.add(childPath);
					QuiltMemoryFile.ReadOnly qmf = QuiltMemoryFile.ReadOnly.create(childPath, Files.readAllBytes(file), compress);
					putFileStats(stats, qmf);
					addEntryWithoutParents(qmf);

					return super.visitFile(file, attrs);
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					DirBuildState state = stack.pop();
					MuonMemoryPath[] children = state.children.toArray(new MuonMemoryPath[0]);
					addEntryWithoutParents(new QuiltUnifiedEntry.QuiltUnifiedFolderReadOnly(state.folder, children));

					stats[STAT_MEMORY] += children.length * 4 + 12;

					return super.postVisitDirectory(dir, exc);
				}
			});

			if (!stack.isEmpty()) {
				throw new IllegalStateException("Stack is not empty!");
			}

			uncompressedSize = stats[STAT_UNCOMPRESSED];
			usedSize = stats[STAT_USED];
			memorySize = stats[STAT_MEMORY] + ((int) (getEntryCount() * 24 / 0.75f));
			fileStore = new QuiltMemoryFileStore.ReadOnly(name, usedSize);
			fileStoreItr = Collections.singleton(fileStore);
		}

		@Override
		protected boolean startWithConcurrentMap() {
			return false;
		}

		private static void putFileStats(int[] stats, QuiltMemoryFile.ReadOnly qmf) {
			stats[STAT_UNCOMPRESSED] += qmf.uncompressedSize;
			stats[STAT_USED] += qmf.byteArray().length;
			stats[STAT_MEMORY] += qmf.byteArray().length + 16;
		}

		/** Creates a new read-only file system that copies every entry of a {@link ZipInputStream} that starts with
		 * "zipPathPrefix". This is effectively the same as
		 * {@link MuonZipFileSystem#MuonZipFileSystem(String, Path, String)}, but doesn't open files from their
		 * original location.
		 * <p>
		 * Using this is likely to be faster than opening a zip via {@link FileSystems#newFileSystem(Path, ClassLoader)}
		 * and then copying it with {@link #ReadOnly(String, boolean, Path, boolean)}.
		 * 
		 * @param name The name for the new filesystem. This affects URLs that reference this file system.
		 * @param zipFrom The zip to read from.
		 * @param zipPathPrefix A prefix for all entries. This should be the empty string to get every entry.
		 * @param compress If true then entries will be compressed in-memory. Generally slow.
		 * @throws IOException if {@link ZipInputStream} threw an {@link IOException} while reading entries. */
		public ReadOnly(String name, ZipInputStream zipFrom, String zipPathPrefix, boolean compress) throws IOException {
			super(name, true);

			addEntryAndParents(new QuiltUnifiedEntry.QuiltUnifiedFolderWriteable(root));

			int[] stats = new int[3];
			stats[STAT_MEMORY] = 60;

			boolean anyEntries = false;
			ZipEntry entry;
			while ((entry = zipFrom.getNextEntry()) != null) {
				anyEntries = true;
				String entryName = entry.getName();

				if (!entryName.startsWith(zipPathPrefix)) {
					continue;
				}
				entryName = entryName.substring(zipPathPrefix.length());
				if (!entryName.startsWith("/")) {
					entryName = "/" + entryName;
				}

				MuonMemoryPath path = getPath(entryName);
				if (entryName.endsWith("/")) {
					createDirectories(path);
				} else {
					// File
					stats[STAT_MEMORY] += path.name.length() + 28;
					byte[] bytes = FileUtil.readAllBytes(zipFrom);
					QuiltMemoryFile.ReadOnly qmf = QuiltMemoryFile.ReadOnly.create(path, bytes, compress);
					putFileStats(stats, qmf);
					addEntryAndParents(qmf);
				}
			}

			if (!anyEntries) {
				// Files that aren't zip files don't throw exceptions
				// Instead they just return null from "ZipInputStream.getNextEntry()"
				// TODO: Check for the zip header constants INSTEAD, since empty zip files also don't pass this
				throw new IOException("No zip entries found!");
			}

			switchToReadOnly();

			uncompressedSize = stats[STAT_UNCOMPRESSED];
			usedSize = stats[STAT_USED];
			memorySize = stats[STAT_MEMORY] + ((int) (getEntryCount() * 24 / 0.75f));

			fileStore = new QuiltMemoryFileStore.ReadOnly(name, usedSize);
			fileStoreItr = Collections.singleton(fileStore);
		}

		private static void putParentFolders(Map<MuonMemoryPath, Set<MuonMemoryPath>> folders, MuonMemoryPath path) {
			if (path == null) {
				return;
			}
			MuonMemoryPath parent = path;
			MuonMemoryPath child = path;
			while ((parent = parent.getParent()) != null) {
				Set<MuonMemoryPath> children = folders.computeIfAbsent(parent, p -> new HashSet<>());
				if (!children.add(child)) {
					break;
				}
				child = parent;
			}
		}

		@Override
		public boolean isReadOnly() {
			return true;
		}

		/** @return The uncompressed size of all files stored in this file system. Since we store file data compressed
		 *         this doesn't reflect actual byte usage. */
		public int getUncompressedSize() {
			return uncompressedSize;
		}

		/** @return The raw number of bytes we store in byte arrays. */
		public int getUsedSize() {
			return usedSize;
		}

		/** @return An estimate of the memory footprint required in this JVM for this file system. Always bigger than
		 *         {@link #getUsedSize()}. */
		public int getEstimatedMemoryFootprint() {
			return memorySize;
		}

		@Override
		public Iterable<FileStore> getFileStores() {
			return fileStoreItr;
		}

		public MuonMemoryFileSystem.ReadWrite copyToWriteable(String newName) {
			MuonMemoryFileSystem.ReadWrite fs = new MuonMemoryFileSystem.ReadWrite(newName, true);
			copyPath(root, fs.root);
			return fs;
		}

		/** Replaces this filesystem with a writable version, at least from the perspective of URL handling. This
		 * {@link FileSystem} will be closed, although existing usages of this filesystem's {@link Path}s won't be
		 * modified. (In other words, this should only be called by the owner of this filesystem). */
		public MuonMemoryFileSystem.ReadWrite replaceWithWritable() {
			close();
			MuonMemoryFileSystem.ReadWrite fs = new MuonMemoryFileSystem.ReadWrite(name, false);
			copyPath(root, fs.root);
			return fs;
		}

		private void copyPath(MuonMemoryPath src, MuonMemoryPath dst) {
			QuiltUnifiedEntry entrySrc = src.fs.getEntry(src);
			if (entrySrc instanceof QuiltMemoryFile) {
				QuiltMemoryFile.ReadOnly fileSrc = (QuiltMemoryFile.ReadOnly) entrySrc;
				QuiltMemoryFile.ReadWrite fileDst = new QuiltMemoryFile.ReadWrite(dst);
				fileDst.copyFrom(fileSrc);
				dst.fs.addEntryWithoutParentsUnsafe(fileDst);
			} else {
				QuiltUnifiedEntry.QuiltUnifiedFolderReadOnly folderSrc = (QuiltUnifiedEntry.QuiltUnifiedFolderReadOnly) entrySrc;
				QuiltUnifiedEntry.QuiltUnifiedFolderWriteable folderDst = new QuiltUnifiedEntry.QuiltUnifiedFolderWriteable(dst);
				dst.fs.addEntryWithoutParentsUnsafe(folderDst);

				for (MuonMapPath<?, ?> pathSrc : folderSrc.children) {
					MuonMemoryPath pathDst = dst.resolve(pathSrc.name);
					folderDst.children.add(pathDst);
					copyPath((MuonMemoryPath) pathSrc, pathDst);
				}
			}
		}
	}

	public static final class ReadWrite extends MuonMemoryFileSystem {

		private QuiltMemoryFileStore.ReadWrite fileStore;
		private Iterable<FileStore> fileStoreItr;

		public ReadWrite(String name, boolean uniquify) {
			super(name, uniquify);
			fileStore = new QuiltMemoryFileStore.ReadWrite(name, this);
			fileStoreItr = Collections.singleton(fileStore);
			addEntryAndParentsUnsafe(new QuiltUnifiedEntry.QuiltUnifiedFolderWriteable(root));
		}

		@Override
		protected boolean startWithConcurrentMap() {
			return true;
		}

		public ReadWrite(String name, boolean uniquify, Path from) throws IOException {
			this(name, uniquify);

			if (!FasterFiles.isDirectory(from)) {
				throw new IOException(from + " is not a directory!");
			}

			final Deque<DirBuildState> stack = new ArrayDeque<>();

			Files.walkFileTree(from, new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					Path relative = from.relativize(dir);

					if (stack.isEmpty()) {
						stack.push(new DirBuildState(root));
					} else {
						String pathName = relative.getFileName().toString();
						stack.push(new DirBuildState(stack.peek().folder.resolve(pathName)));
					}

					return super.preVisitDirectory(dir, attrs);
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					DirBuildState state = stack.peek();
					String fileName = file.getFileName().toString();
					MuonMemoryPath childPath = state.folder.resolve(fileName);
					state.children.add(childPath);
					QuiltMemoryFile.ReadWrite qmf = new QuiltMemoryFile.ReadWrite(childPath);
					qmf.createOutputStream(false, false).write(Files.readAllBytes(file));
					addEntryWithoutParents(qmf);
					return super.visitFile(file, attrs);
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					DirBuildState state = stack.pop();
					QuiltUnifiedEntry.QuiltUnifiedFolderWriteable qmf = new QuiltUnifiedEntry.QuiltUnifiedFolderWriteable(state.folder);
					addEntryWithoutParents(qmf);
					qmf.children.addAll(state.children);
					return super.postVisitDirectory(dir, exc);
				}
			});

			if (!stack.isEmpty()) {
				throw new IllegalStateException("Stack is not empty!");
			}
		}

		/** Creates a new read-only version of this file system, copying the entire directory structure and file content
		 * into it. This also compresses the files if the compress argument is true (and if compression reduces the file
		 * size), and trims every byte array used to store files down to the minimum required length. */
		public MuonMemoryFileSystem.ReadOnly optimizeToReadOnly(String newName, boolean compress) {
			try {
				return new ReadOnly(newName, true, root, compress);
			} catch (IOException e) {
				throw new RuntimeException(
					"For some reason the in-memory file system threw an IOException while reading!", e
				);
			}
		}

		/** Creates a new read-only version of this file system, copying the entire directory structure and file content
		 * into it. This also compresses the files if the compress argument is true (and if compression reduces the file
		 * size), and trims every byte array used to store files down to the minimum required length. */
		public MuonMemoryFileSystem.ReadOnly replaceWithReadOnly(boolean compress) {
			beginMove();
			try {
				return new ReadOnly(name, false, root, compress);
			} catch (IOException e) {
				throw new RuntimeException(
					"For some reason the in-memory file system threw an IOException while reading!", e
				);
			} finally {
				endMove();
			}
		}

		@Override
		public Iterable<FileStore> getFileStores() {
			return fileStoreItr;
		}

		@Override
		public boolean isReadOnly() {
			return false;
		}

		@Override
		public boolean isPermanentlyReadOnly() {
			return false;
		}

		// FasterFileSystem

		@Override
		public Path createFile(Path path, FileAttribute<?>... attrs) throws IOException {
			// This is already fairly quick, so don't bother reimplementing it
			return super.createFile(path, attrs);
		}

		@Override
		public boolean isWritable(Path path) {
			return exists(path);
		}
	}
}
