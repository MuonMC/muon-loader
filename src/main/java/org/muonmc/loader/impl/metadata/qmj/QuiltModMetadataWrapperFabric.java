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

package org.muonmc.loader.impl.metadata.qmj;

import org.muonmc.loader.api.ModContainer;
import org.muonmc.loader.api.game.minecraft.Environment;
import org.muonmc.loader.api.plugin.ModContainerExt;
import org.muonmc.loader.impl.metadata.FabricLoaderModMetadata;
import org.muonmc.loader.impl.metadata.GeneralExt2FabricMetadata;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

import java.util.Collection;
import java.util.Collections;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public class QuiltModMetadataWrapperFabric extends GeneralExt2FabricMetadata implements FabricLoaderModMetadata {

	public QuiltModMetadataWrapperFabric(InternalModMetadata quiltMeta, ModContainer quiltContainer) {
		super(quiltMeta, (ModContainerExt) quiltContainer);
	}

	@Override
	public Collection<String> getMixinConfigs(Environment environment) {
		return Collections.emptyList();
	}
}
