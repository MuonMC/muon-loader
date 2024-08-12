/*
 * Copyright 2016 FabricMC
 * Copyright 2022-2024 QuiltMC
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

package org.muonmc.loader.impl.util.log;

import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;
import org.muonmc.loader.impl.util.SystemProperties;

import java.util.Locale;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public enum LogLevel {
	ERROR, WARN, INFO, DEBUG, TRACE;

	public boolean isLessThan(LogLevel level) {
		return ordinal() > level.ordinal();
	}

	public static LogLevel getDefault() {
		String val = System.getProperty(SystemProperties.LOG_LEVEL);
		if (val == null) return INFO;

		return LogLevel.valueOf(val.toUpperCase(Locale.ENGLISH));
	}
}