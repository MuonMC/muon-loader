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

import java.util.Collection;
import java.util.HashSet;

import net.fabricmc.accesswidener.AccessWidener;

import org.jetbrains.annotations.Nullable;
import org.muonmc.loader.api.game.minecraft.Environment;
import org.muonmc.loader.api.plugin.solver.ModLoadOption;
import org.muonmc.loader.impl.MuonLoaderImpl;
import org.muonmc.loader.impl.launch.common.MuonLauncherBase;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import net.fabricmc.accesswidener.AccessWidenerClassVisitor;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
final class MuonTransformer {
	public static byte @Nullable [] transform(boolean isDevelopment, Environment environment, TransformCache cache, AccessWidener accessWidener, String name, ModLoadOption mod, byte[] bytes) {
		boolean isGameClass = mod.id().equals(MuonLoaderImpl.INSTANCE.getGameProvider().getGameId());
		boolean transformAccess = isGameClass && MuonLauncherBase.getLauncher().getMappingConfiguration().requiresPackageAccessHack();
		boolean strip = !isGameClass || isDevelopment;
		boolean applyAccessWidener = isGameClass && accessWidener.getTargets().contains(name);

		if (!transformAccess && !strip && !applyAccessWidener) {
			return bytes;
		}

		ClassReader classReader = new ClassReader(bytes);
		ClassWriter classWriter = null;
		ClassVisitor visitor = null;
		int visitorCount = 0;

		if (strip) {
			ClassStrippingData data = new ClassStrippingData(MuonLoaderImpl.ASM_VERSION, environment, cache.getAllMods());
			classReader.accept(data, ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);

			if (data.stripEntireClass()) {
				cache.hideClass(name, data.summarizeDenyLoadReasons());
				return null;
			}

			Collection<String> stripMethods = data.getStripMethods();

			boolean stripAnyLambdas = false;

			if (!data.getStripMethodLambdas().isEmpty()) {
				LambdaStripCalculator calc = new LambdaStripCalculator(MuonLoaderImpl.ASM_VERSION, data.getStripMethodLambdas());
				classReader.accept(calc, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
				Collection<String> additionalStripMethods = calc.computeAdditionalMethodsToStrip();

				if (!additionalStripMethods.isEmpty()) {
					stripMethods = new HashSet<>(stripMethods);
					stripMethods.addAll(additionalStripMethods);

					stripAnyLambdas = true;
				}
			}

			if (!data.isEmpty()) {

				if (stripAnyLambdas) {
					// ClassWriter has a (useful) optimisation that copies over the
					// entire constant pool and bootstrap methods from the original one,
					// as well as any untransformed methods.
					// However we can't use the second one, since we may need to remove bootstrap methods
					// that reference methods which are no longer present in the stripped version.
					classWriter = new ClassWriter(0);
				} else {
					classWriter = new ClassWriter(classReader, 0);
				}

				visitor = new ClassStripper(MuonLoaderImpl.ASM_VERSION, classWriter, data.getStripInterfaces(), data.getStripFields(), stripMethods);
				visitorCount++;
			}
		}

		if (classWriter == null) {
			classWriter = new ClassWriter(classReader, 0);
			visitor = classWriter;
		}

		if (applyAccessWidener) {
			visitor = AccessWidenerClassVisitor.createClassVisitor(MuonLoaderImpl.ASM_VERSION, visitor, accessWidener);
			visitorCount++;
		}

		if (transformAccess) {
			visitor = new PackageAccessFixer(MuonLoaderImpl.ASM_VERSION, visitor);
			visitorCount++;
		}

		if (visitorCount <= 0) {
			return null;
		}

		classReader.accept(visitor, 0);
		return classWriter.toByteArray();
	}
}
