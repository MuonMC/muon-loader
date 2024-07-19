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

package org.muonmc.loader.impl.transformer;

import java.util.HashMap;

import org.objectweb.asm.AnnotationVisitor;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

import net.fabricmc.api.EnvType;

/** Deprecated. All stuff were moved to {@link PackageStrippingData}. */
@Deprecated
@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public class PackageEnvironmentStrippingData extends PackageStrippingData {

	public PackageEnvironmentStrippingData(int api, EnvType envType) {
		super(api, envType, new HashMap<>());
	}

	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		return super.visitAnnotation(descriptor, visible);
	}

	@Override
	public boolean stripEntirePackage() {
		return super.stripEntirePackage();
	}
}
