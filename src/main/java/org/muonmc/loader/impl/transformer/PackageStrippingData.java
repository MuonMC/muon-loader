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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.muonmc.loader.api.game.minecraft.Environment;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;
import org.muonmc.loader.api.Requires;
import org.muonmc.loader.api.game.minecraft.ClientOnly;
import org.muonmc.loader.api.game.minecraft.DedicatedServerOnly;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

import net.fabricmc.api.EnvType;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public class PackageStrippingData extends AbstractStripData {

	private static final String CLIENT_ONLY_DESCRIPTOR = Type.getDescriptor(ClientOnly.class);
	private static final String SERVER_ONLY_DESCRIPTOR = Type.getDescriptor(DedicatedServerOnly.class);
	private static final String REQUIRES_DESCRIPTOR = Type.getDescriptor(Requires.class);

	public PackageStrippingData(int api, Environment environment, Map<String, String> modCodeSourceMap) {
		super(api, environment, new HashSet<>(modCodeSourceMap.keySet()));
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		if (CLIENT_ONLY_DESCRIPTOR.equals(descriptor)) {
			if (environment == Environment.DEDICATED_SERVER) {
				denyClientOnlyLoad();
			}
		} else if (SERVER_ONLY_DESCRIPTOR.equals(descriptor)) {
			if (environment == Environment.CLIENT) {
				denyDediServerOnlyLoad();
			}
		} else if (REQUIRES_DESCRIPTOR.equals(descriptor)) {
			return new AnnotationVisitor(api) {
				@Override
				public AnnotationVisitor visitArray(String name) {
					if ("value".equals(name)) {
						return new AnnotationVisitor(api) {

							final List<String> requiredMods = new ArrayList<>();

							@Override
							public void visit(String name, Object value) {
								requiredMods.add(String.valueOf(value));
							}

							@Override
							public void visitEnd() {
								checkHasAllMods(requiredMods);
							}
						};
					}
					else {
						return null;
					}
				}
			};
		}
		return null;
	}

	@Override
	protected String type() {
		return "package";
	}

	public boolean stripEntirePackage() {
		return denyLoadReasons.size() > 0;
	}
}
