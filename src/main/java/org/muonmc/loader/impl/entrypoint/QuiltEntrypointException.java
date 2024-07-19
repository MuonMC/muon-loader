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

package org.muonmc.loader.impl.entrypoint;

import org.muonmc.loader.api.entrypoint.EntrypointException;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public class QuiltEntrypointException extends EntrypointException {

	private final String key;

	public QuiltEntrypointException(String key, Throwable cause) {
		super("Exception while loading entries for entrypoint '" + key + "'!", cause);
		this.key = key;
	}

	public QuiltEntrypointException(String key, String causingMod, Throwable cause) {
		super("Exception while loading entries for entrypoint '" + key + "' provided by '" + causingMod + "'", cause);
		this.key = key;
	}

	public QuiltEntrypointException(String s) {
		super(s);
		this.key = "";
	}

	public QuiltEntrypointException(Throwable t) {
		super(t);
		this.key = "";
	}

	@Override
	public String getKey() {
		return key;
	}
}
