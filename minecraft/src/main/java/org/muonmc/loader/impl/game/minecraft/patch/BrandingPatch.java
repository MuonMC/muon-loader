package org.muonmc.loader.impl.game.minecraft.patch;
/*
 * Copyright 2016 FabricMC
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

import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.muonmc.loader.impl.entrypoint.GamePatch;
import org.muonmc.loader.impl.entrypoint.GamePatchContext;
import org.muonmc.loader.impl.launch.common.MuonLauncher;
import org.muonmc.loader.impl.util.log.Log;
import org.muonmc.loader.impl.util.log.LogCategory;

import java.util.ListIterator;

/**
 * Changes the vanilla server/client brand which acts similarly to a user agent.
 */
@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public final class BrandingPatch extends GamePatch {
	@Override
	public void process(MuonLauncher launcher, String namespace, GamePatchContext context) {
		for (String brandClassName : new String[] {
				"net.minecraft.client.ClientBrandRetriever",
				"net.minecraft.server.MinecraftServer"
		}) {
			ClassNode brandClass = context.getClassNode(brandClassName);

			if (brandClass != null) {
				if (applyBrandingPatch(brandClass)) {
					context.addPatchedClass(brandClass);
				}
			}
		}
	}

	private boolean applyBrandingPatch(ClassNode classNode) {
		boolean applied = false;

		for (MethodNode node : classNode.methods) {
			if (node.name.equals("getClientModName") || node.name.equals("getServerModName") && node.desc.endsWith(")Ljava/lang/String;")) {
				Log.debug(LogCategory.GAME_PATCH, "Applying brand name hook to %s::%s", classNode.name, node.name);

				ListIterator<AbstractInsnNode> it = node.instructions.iterator();

				while (it.hasNext()) {
					if (it.next().getOpcode() == Opcodes.ARETURN) {
						it.previous();
						it.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "org/muonmc/loader/impl/game/minecraft/Hooks", "insertBranding", "(Ljava/lang/String;)Ljava/lang/String;", false));
						it.next();
					}
				}

				applied = true;
			}
		}

		return applied;
	}
}
