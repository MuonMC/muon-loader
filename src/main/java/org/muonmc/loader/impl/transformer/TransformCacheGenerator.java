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

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.muonmc.loader.api.FasterFiles;
import org.muonmc.loader.api.MuonLoader;
import org.muonmc.loader.api.plugin.solver.ModLoadOption;
import org.muonmc.loader.impl.discovery.ModResolutionException;
import org.muonmc.loader.impl.filesystem.MuonMapFileSystem;
import org.muonmc.loader.impl.launch.common.MuonLauncherBase;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;
import org.muonmc.loader.impl.util.SystemProperties;

import net.fabricmc.accesswidener.AccessWidener;
import net.fabricmc.accesswidener.AccessWidenerReader;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
final class TransformCacheGenerator {


	static TransformCache generate(Path root, List<ModLoadOption> modList) throws ModResolutionException, IOException {
		TransformCache cache = new TransformCache(root, modList);
		MuonMapFileSystem.dumpEntries(root.getFileSystem(), "after-copy");

		// Transform time!
		// Load AWs
		AccessWidener accessWidener = loadAccessWideners(cache);
		// game provider transformer and QuiltTransformer
		cache.forEachClassFile((mod, name, file) -> {

			byte[] classBytes = MuonLauncherBase.getLauncher().getEntrypointTransformer().transform(name);

			if (classBytes == null) {
				classBytes = Files.readAllBytes(file);
			}

			return MuonTransformer.transform(
					MuonLoader.isDevelopmentEnvironment(),
					MuonLauncherBase.getLauncher().getEnvironmentType(),
					cache,
					accessWidener,
					name,
					mod,
					classBytes
			);
		});

		// chasm
		if (Boolean.getBoolean(SystemProperties.ENABLE_EXPERIMENTAL_CHASM)) {
			ChasmInvoker.applyChasm(cache);
		}
		InternalsHiderTransform internalsHider = new InternalsHiderTransform(InternalsHiderTransform.Target.MOD);
		Map<Path, ModLoadOption> classes = new HashMap<>();

		// internals hider
		// the double read is necessary to avoid storing all classes in memory at once, and thus having memory complexity
		// proportional to mod count
		cache.forEachClassFile((mod, name, file) -> {
			byte[] classBytes = Files.readAllBytes(file);
			classes.put(file, mod);
			internalsHider.scanClass(mod, file, classBytes);
			return null;
		});

		for (Map.Entry<Path, ModLoadOption> entry : classes.entrySet()) {
			byte[] classBytes = Files.readAllBytes(entry.getKey());
			byte[] newBytes = internalsHider.run(entry.getValue(), classBytes);
			if (newBytes != null) {
				Files.write(entry.getKey(), newBytes);
			}
		}

		internalsHider.finish();

		return cache;
	}

	private static AccessWidener loadAccessWideners(TransformCache cache) {
		AccessWidener ret = new AccessWidener();
		AccessWidenerReader accessWidenerReader = new AccessWidenerReader(ret);

		for (ModLoadOption mod : cache.getModsInCache()) {
			for (String accessWidener : mod.metadata().accessWideners()) {

				Path path = cache.getRoot(mod).resolve(accessWidener);

				if (!FasterFiles.isRegularFile(path)) {
					throw new RuntimeException("Failed to find accessWidener file from mod " + mod.metadata().id() + " '" + accessWidener + "'");
				}

				try (BufferedReader reader = Files.newBufferedReader(path)) {
					accessWidenerReader.read(reader, MuonLoader.getMappingResolver().getCurrentRuntimeNamespace());
				} catch (Exception e) {
					throw new RuntimeException("Failed to read accessWidener file from mod " + mod.metadata().id(), e);
				}
			}
		}

		return ret;
	}
}
