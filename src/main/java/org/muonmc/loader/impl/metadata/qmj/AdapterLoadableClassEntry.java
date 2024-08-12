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

import org.muonmc.loader.api.plugin.ModMetadataExt.ModEntrypoint;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

/**
 * Represents a class entry inside of that specifies a language adapter to use to load the class.
 */
@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public final class AdapterLoadableClassEntry implements ModEntrypoint {
	private final String adapter;
	private final String value;

	public AdapterLoadableClassEntry(String value) {
		this.adapter = "default";
		this.value = value;
	}

	public AdapterLoadableClassEntry(String adapter, String value) {
		this.adapter = adapter;
		this.value = value;
	}

	public String getAdapter() {
		return this.adapter;
	}

	public String getValue() {
		return this.value;
	}
}