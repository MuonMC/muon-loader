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

package net.fabricmc.loader.impl.launch;


import net.fabricmc.loader.impl.launch.knot.Knot;

import net.fabricmc.api.EnvType;

import org.muonmc.loader.impl.launch.common.MuonLauncher;
import org.muonmc.loader.impl.launch.common.MuonLauncherBase;
import org.muonmc.loader.impl.util.deprecated.EnvTypeUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.jar.Manifest;

@Deprecated
public class FabricLauncherBase implements FabricLauncher {
	private final MuonLauncher delegate = MuonLauncherBase.getLauncher();

	private final MappingConfiguration mappingConfiguration = new MappingConfiguration(delegate.getMappingConfiguration());

	public static Class<?> getClass(String className) throws ClassNotFoundException {
		return Class.forName(className, true, getLauncher().getTargetClassLoader());
	}

	@Override
	public MappingConfiguration getMappingConfiguration() {
		return mappingConfiguration;
	}

	@Override
	public void addToClassPath(Path path, String... allowedPrefixes) {
		delegate.addToClassPath(path, allowedPrefixes);
	}

	@Override
	public void setAllowedPrefixes(Path path, String... prefixes) {
		delegate.setAllowedPrefixes(path, prefixes);
	}

//	@Override
//	public void setValidParentClassPath(Collection<Path> paths) {
//		delegate.setValidParentClassPath(paths);
//	}

	@Override
	public EnvType getEnvironmentType() {
		return EnvTypeUtil.toEnvType(delegate.getEnvironmentType());
	}

	@Override
	public boolean isClassLoaded(String name) {
		return delegate.isClassLoaded(name);
	}

	@Override
	public Class<?> loadIntoTarget(String name) throws ClassNotFoundException {
		return delegate.loadIntoTarget(name);
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		return delegate.getResourceAsStream(name);
	}

	@Override
	public ClassLoader getTargetClassLoader() {
		return delegate.getTargetClassLoader();
	}

	@Override
	public byte[] getClassByteArray(String name, boolean runTransformers) throws IOException {
		return new byte[0];
	}

	@Override
	public Manifest getManifest(Path originPath) {
		return delegate.getManifest(originPath);
	}

	@Override
	public boolean isDevelopment() {
		return delegate.isDevelopment();
	}

	@Override
	public String getEntrypoint() {
		return delegate.getEntrypoint();
	}

	@Override
	public String getTargetNamespace() {
		return delegate.getTargetNamespace();
	}

	@Override
	public List<Path> getClassPath() {
		return delegate.getClassPath();
	}

	public static FabricLauncher getLauncher() {
		return new Knot();
	}

	public static Map<String, Object> getProperties() {
		return MuonLauncherBase.getProperties();
	}

	public static boolean isMixinReady() {
		return MuonLauncherBase.isMixinReady();
	}
}
