/*
 * Copyright 2022, 2023 QuiltMC
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

package org.muonmc.loader.impl.launch.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

import org.muonmc.loader.api.game.minecraft.Environment;
import org.muonmc.loader.impl.entrypoint.GameTransformer;
import org.muonmc.loader.api.ModContainer;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public interface MuonLauncher {
	MappingConfiguration getMappingConfiguration();

	void addToClassPath(Path path, String... allowedPrefixes);
	void addToClassPath(Path path, ModContainer mod, URL origin, String... allowedPrefixes);
	void setAllowedPrefixes(Path path, String... prefixes);
	void setTransformCache(URL insideTransformCache);
	void setHiddenClasses(Set<String> classes);
	void setHiddenClasses(Map<String, String> classes);
	void setPluginPackages(Map<String, ClassLoader> hiddenClasses);
	void hideParentUrl(URL hidden);
	void hideParentPath(Path obf);
	void validateGameClassLoader(Object gameInstance);

	Environment getEnvironmentType();

	boolean isClassLoaded(String name);

	/**
	 * Load a class into the game's class loader even if its bytes are only available from the parent class loader.
	 */
	Class<?> loadIntoTarget(String name) throws ClassNotFoundException;

	InputStream getResourceAsStream(String name);

	URL getResourceURL(String name);

	ClassLoader getTargetClassLoader();

	ClassLoader getClassLoader(ModContainer mod);

	/**
	 * Gets the byte array for a particular class.
	 *
	 * @param name The name of the class to retrieve
	 * @param runTransformers Whether to run all transformers <i>except mixin</i> on the class
	 */
	byte[] getClassByteArray(String name, boolean runTransformers) throws IOException;

	Manifest getManifest(Path originPath);

	boolean isDevelopment();

	String getEntrypoint();

	String getTargetNamespace();

	List<Path> getClassPath();

	GameTransformer getEntrypointTransformer();
}
