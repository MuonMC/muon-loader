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

package org.muonmc.loader.impl.metadata.qmj;

import java.util.Collection;

import org.muonmc.loader.api.ModMetadata;
import org.muonmc.loader.api.plugin.ModMetadataExt;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

/** Internal mod metadata interface which stores implementation detail. */
@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public interface InternalModMetadata
	extends ModMetadata, ModMetadataExt, ConvertibleModMetadata {

	@Override
	default boolean shouldQuiltDefineDependencies() {
		return true;
	}

	@Override
	default boolean shouldQuiltDefineProvides() {
		return true;
	}

	Collection<String> jars();

	Collection<String> repositories();

	String intermediateMappings();

	@Override
	default InternalModMetadata asQuiltModMetadata() {
		return this;
	}
}
