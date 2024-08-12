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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.muonmc.loader.impl.launch.knot.IllegalMuonInternalAccessError;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;
import org.muonmc.loader.impl.util.log.Log;
import org.muonmc.loader.impl.util.log.LogCategory;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public class MuonInternalExceptionUtil {

	private static final ConcurrentMap<String, Boolean> WARNED_ENTRIES = new ConcurrentHashMap<>();

	public static void throwInternalAccess(String msg) {
		throw new IllegalMuonInternalAccessError(msg);
	}

	public static void warnInternalAccess(String msg) {
		if (WARNED_ENTRIES.put(msg, Boolean.TRUE) == null) {
			Log.warn(LogCategory.GENERAL, msg);
		}
	}
}
