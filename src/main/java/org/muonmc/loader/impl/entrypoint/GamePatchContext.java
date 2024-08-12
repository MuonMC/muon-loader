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

package org.muonmc.loader.impl.entrypoint;

import org.jetbrains.annotations.ApiStatus;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

@ApiStatus.NonExtendable
@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public interface GamePatchContext {

	/** @return A {@link ClassReader} which reads the original class file. */
	ClassReader getClassSourceReader(String className);

	/** @return A {@link ClassNode}, which may have already been modified by another {@link GamePatch}. */
	ClassNode getClassNode(String className);

	void addPatchedClass(ClassNode patchedClass);
}