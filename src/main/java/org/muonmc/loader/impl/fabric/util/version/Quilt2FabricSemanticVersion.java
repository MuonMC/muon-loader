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

package org.muonmc.loader.impl.fabric.util.version;

import java.util.Optional;

import org.muonmc.loader.api.Version;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.util.version.SemanticVersionImpl;

@Deprecated
@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public class Quilt2FabricSemanticVersion extends Quilt2FabricVersion implements SemanticVersion {

	final Version.Semantic quilt;

	public Quilt2FabricSemanticVersion(Version.Semantic quilt) {
		super(quilt);
		this.quilt = quilt;
	}

	public static net.fabricmc.loader.api.SemanticVersion toFabric(Version.Semantic quilt) {
		if (quilt == null) {
			return null;
		} else {
			return new SemanticVersionImpl(quilt);
		}
	}

	public static Version.Semantic fromFabric(net.fabricmc.loader.api.SemanticVersion from) {
		if (from == null) {
			return null;
		} else if (from instanceof Quilt2FabricSemanticVersion) {
			return ((Quilt2FabricSemanticVersion) from).quilt;
		} else if (from instanceof SemanticVersionImpl) {
			// Legacy fabric class
			return fromFabric(((SemanticVersionImpl) from).getParent());
		} else {
			throw new IllegalStateException("Unexpected version " + from.getClass());
		}
	}

	@Override
	public int getVersionComponentCount() {
		return quilt.versionComponentCount();
	}

	@Override
	public int getVersionComponent(int pos) {
		return quilt.versionComponent(pos);
	}

	@Override
	public Optional<String> getPrereleaseKey() {
		return quilt.isPreReleasePresent() ? Optional.of(quilt.preRelease()) : Optional.empty();
	}

	@Override
	public Optional<String> getBuildKey() {
		String build = quilt.buildMetadata();
		return build.isEmpty() ? Optional.empty() : Optional.of(build);
	}

	@Override
	public boolean hasWildcard() {
		return getVersionComponent(getVersionComponentCount() - 1) == COMPONENT_WILDCARD;
	}
}
