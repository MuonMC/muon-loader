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

package org.muonmc.loader.impl.language;

import java.io.IOException;

import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

@Deprecated
@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public interface LanguageAdapter {
	enum MissingSuperclassBehavior {
		RETURN_NULL,
		CRASH
	}

	default Object createInstance(String classString, Options options) throws ClassNotFoundException, LanguageAdapterException {
		try {
			Class<?> c = JavaLanguageAdapter.getClass(classString, options);

			if (c != null) {
				return createInstance(c, options);
			} else {
				return null;
			}
		} catch (IOException e) {
			throw new LanguageAdapterException("I/O error!", e);
		}
	}

	Object createInstance(Class<?> baseClass, Options options) throws LanguageAdapterException;

	class Options {
		private MissingSuperclassBehavior missingSuperclassBehavior;

		public MissingSuperclassBehavior getMissingSuperclassBehavior() {
			return missingSuperclassBehavior;
		}

		public static class Builder {
			private final Options options;

			private Builder() {
				options = new Options();
			}

			public static Builder create() {
				return new Builder();
			}

			public Builder missingSuperclassBehaviour(MissingSuperclassBehavior value) {
				options.missingSuperclassBehavior = value;
				return this;
			}

			public Options build() {
				return options;
			}
		}
	}
}