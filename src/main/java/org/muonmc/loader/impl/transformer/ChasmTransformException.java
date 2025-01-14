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

package org.muonmc.loader.impl.transformer;

import org.muonmc.loader.impl.discovery.ModResolutionException;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

/** Thrown when something goes wrong with chasm. */
@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public class ChasmTransformException extends ModResolutionException {

	public ChasmTransformException(String format, Object... args) {
		super(format, args);
	}

	public ChasmTransformException(String s, Throwable t) {
		super(s, t);
	}

	public ChasmTransformException(String s) {
		super(s);
	}

	public ChasmTransformException(Throwable t) {
		super(t);
	}
}
