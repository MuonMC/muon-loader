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

package org.muonmc.loader.impl.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.quiltmc.config.implementor_api.ConfigEnvironment;
import org.quiltmc.config.api.Serializer;
import org.quiltmc.config.api.serializers.Json5Serializer;
import org.quiltmc.config.api.serializers.TomlSerializer;
import org.muonmc.loader.impl.MuonLoaderImpl;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;
import org.muonmc.loader.impl.util.SystemProperties;
import org.muonmc.loader.impl.util.log.Log;
import org.muonmc.loader.impl.util.log.LogCategory;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public final class MuonConfigImpl {
	private static ConfigEnvironment ENV;

	private MuonConfigImpl() {
	}

	public static void init() {
		Map<String, Serializer> serializerMap = new LinkedHashMap<>();

		serializerMap.put("toml", TomlSerializer.INSTANCE);
		serializerMap.put("json5", Json5Serializer.INSTANCE);

		for (Serializer serializer : MuonLoaderImpl.INSTANCE.getEntrypoints("config_serializer", Serializer.class)) {
			Serializer oldValue = serializerMap.put(serializer.getFileExtension(), serializer);

			if (oldValue != null) {
				Log.warn(LogCategory.CONFIG, "Replacing {} serializer {} with {}", serializer.getFileExtension(), oldValue.getClass(), serializer.getClass());
			}
		}

		String globalConfigExtension = System.getProperty(SystemProperties.GLOBAL_CONFIG_EXTENSION);
		String defaultConfigExtension = System.getProperty(SystemProperties.DEFAULT_CONFIG_EXTENSION);

		Serializer[] serializers = serializerMap.values().toArray(new Serializer[0]);

		if (globalConfigExtension != null && !serializerMap.containsKey(globalConfigExtension)) {
			throw new RuntimeException("Cannot use file extension " + globalConfigExtension + " globally: no matching serializer found");
		}

		if (defaultConfigExtension != null && !serializerMap.containsKey(defaultConfigExtension)) {
			throw new RuntimeException("Cannot use file extension " + defaultConfigExtension + " by default: no matching serializer found");
		}

		if (defaultConfigExtension == null) {
			ENV = new ConfigEnvironment(MuonLoaderImpl.INSTANCE.getConfigDir(), globalConfigExtension, serializers[0]);

			for (int i = 1; i < serializers.length; ++i) {
				ENV.registerSerializer(serializers[i]);
			}
		} else {
			ENV = new ConfigEnvironment(MuonLoaderImpl.INSTANCE.getConfigDir(), globalConfigExtension, serializerMap.get(defaultConfigExtension));

			for (Serializer serializer : serializers) {
				ENV.registerSerializer(serializer);
			}
		}
	}

	public static ConfigEnvironment getConfigEnvironment() {
		return ENV;
	}
}
