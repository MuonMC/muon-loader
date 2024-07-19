/*
 * Copyright 2023 QuiltMC
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

package org.muonmc.loader.impl.transformer;

import java.util.Map;

import org.muonmc.loader.impl.filesystem.MuonZipPath;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public class TransformCacheResult {
	public final MuonZipPath transformCacheRoot;
	public final boolean isNewlyGenerated;
	public final Map<String, String> hiddenClasses;

	TransformCacheResult(MuonZipPath transformCacheRoot, boolean isNewlyGenerated, Map<String, String> hiddenClasses) {
		this.isNewlyGenerated = isNewlyGenerated;
		this.transformCacheRoot = transformCacheRoot;
		this.hiddenClasses = hiddenClasses;
	}
}
