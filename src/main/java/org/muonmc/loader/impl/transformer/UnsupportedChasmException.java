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

package org.muonmc.loader.impl.transformer;

import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

/** Indicates that the installed chasm version doesn't match what loader expects. Temporary exception since chasm will
 * eventually be built into loader. */
@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public class UnsupportedChasmException extends RuntimeException {

	public UnsupportedChasmException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnsupportedChasmException(String message) {
		super(message);
	}

	public UnsupportedChasmException(Throwable cause) {
		super(cause);
	}

}
