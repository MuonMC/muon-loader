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
import java.net.URI;
import java.nio.file.ClosedFileSystemException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public abstract class MuonBaseFileSystem<FS extends MuonBaseFileSystem<FS, P>, P extends MuonBasePath<FS, P>>
	extends FileSystem {

	static {
		DelegatingUrlStreamHandlerFactory.load();
	}

	private static final Map<String, Integer> uniqueNames = new HashMap<>();
	private static final AtomicLong SYNC_ASSIGNMENT = new AtomicLong();

	/** This stores the "synchronization order" for "move" type operations where we need to synchronize on multiple
	 * filesystems at once. */
	final long syncOrder = SYNC_ASSIGNMENT.getAndIncrement();

	final Class<FS> filesystemClass;
	final Class<P> pathClass;

	final String name;
	final P root;

	MuonBaseFileSystem(Class<FS> filesystemClass, Class<P> pathClass, String name, boolean uniqueify) {
		this.filesystemClass = filesystemClass;
		this.pathClass = pathClass;
		this.name = uniqueOf(uniqueify, sanitizeName(name));
		this.root = createPath(null, MuonBasePath.NAME_ROOT);

		// Validate that our sanitising passes this.name through to
		// both the host and authority
		URI uri = root.toUri();
		if (!this.name.equals(uri.getHost())) {
			throw new RuntimeException(
				this.name + " wasn't found as the host of " + uri + " (host = '" + uri.getHost() + "').\n"
					+ "This is a bug with 'sanitizeName(\"" + name + "\")', not a filename issue!"
			);
		}
		if (uri.getAuthority() == null || !uri.getAuthority().contains(this.name)) {
			throw new RuntimeException(
				this.name + " wasn't found in the authority of " + uri + " (authority = " + uri.getAuthority() + "').\n"
					+ "This is a bug with 'sanitizeName(\"" + name + "\")', not a filename issue!"
			);
		}
	}

	private static String uniqueOf(boolean uniqueify, String name) {
		if (!uniqueify) {
			return name;
		}

		synchronized (MuonBaseFileSystem.class) {
			Integer current = uniqueNames.get(name);
			if (current != null) {
				current++;
			} else {
				current = 0;
			}
			uniqueNames.put(name, current);
			return name + ".i" + current;
		}
	}

	// Shamelessly stolen from UnixUriUtils
	private static final long LOW_MASK = 0x3ff600000000000L;
	private static final long HIGH_MASK = 0x07fffffe03fffffeL;

	private static String sanitizeName(String str) {
		byte[] path = str.getBytes();
		StringBuilder sb = new StringBuilder();

		boolean first = true;

		for (byte b : path) {
			char c = (char) (b & 255);

			if (first && (c == '-' || c == '_' || c == '~' || c == '.')) {
				continue;
			}

			if (matchesMagic(c)) {
				if (c == '.') {
					while (sb.length() > 0) {
						char previous = sb.charAt(sb.length() - 1);
						if (previous == '-' || previous == '.') {
							sb.deleteCharAt(sb.length() - 1);
						} else {
							break;
						}
					}
				}
				sb.append(c);
				first = c == '.';
			} else if (c == '_' || c == '~') {
				sb.append('-');
			}
			// Since we don't need to decode anything,
			// we just delete characters we don't like!
		}

		while (sb.length() > 0 && sb.charAt(sb.length() - 1) == '-') {
			sb.deleteCharAt(sb.length() - 1);
		}

		if (sb.length() == 0) {
			return "empty.file";
		}

		return sb.toString();
	}

	private static boolean matchesMagic(char c) {
		if (c < 64) {
			return (1L << c & LOW_MASK) != 0L;
		} else if (c < 128) {
			return (1L << c - 64 & HIGH_MASK) != 0L;
		} else {
			return false;
		}
	}

	abstract P createPath(@Nullable P parent, String name);

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + name + "]";
	}

	public String getName() {
		return name;
	}

	public P getRoot() {
		return root;
	}

	@Override
	public String getSeparator() {
		return MuonBasePath.NAME_ROOT;
	}

	@Override
	public Iterable<Path> getRootDirectories() {
		return Collections.singleton(root);
	}

	void checkOpen() throws ClosedFileSystemException {
		if (!isOpen()) {
			throw new ClosedFileSystemException();
		}
	}

	@Override
	public P getPath(String first, String... more) {
		if (first.isEmpty()) {
			return createPath(null, "");
		}

		if (more.length == 0) {
			P path = first.startsWith("/") ? root : null;
			for (String sub : first.split("/")) {
				if (path == null) {
					path = createPath(null, sub);
				} else {
					path = path.resolve(sub);
				}
			}
			return path;
		} else {
			P path = createPath(null, first);
			for (String sub : more) {
				path = path.resolve(sub);
			}
			return path;
		}
	}

	@Override
	public PathMatcher getPathMatcher(String syntaxAndPattern) {
		final String regex;
		if (syntaxAndPattern.startsWith("regex:")) {
			regex = syntaxAndPattern.substring("regex:".length());
		} else if (syntaxAndPattern.startsWith("glob:")) {
			regex = GlobToRegex.toRegex(syntaxAndPattern.substring("glob:".length()), "/");
		} else {
			throw new UnsupportedOperationException("Unsupported syntax or pattern: '" + syntaxAndPattern + "'");
		}

		Pattern pattern = Pattern.compile(regex);
		return path -> pattern.matcher(path.toString()).matches();
	}

	@Override
	public UserPrincipalLookupService getUserPrincipalLookupService() {
		throw new UnsupportedOperationException();
	}

	@Override
	public WatchService newWatchService() throws IOException {
		throw new UnsupportedOperationException();
	}
}
