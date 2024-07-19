/*
 * Copyright 2016 FabricMC
 * Copyright 2022-2023 QuiltMC
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

package net.fabricmc.loader.impl.gui;

import java.awt.GraphicsEnvironment;
import java.util.function.Consumer;

import org.muonmc.loader.api.gui.LoaderGuiException;
import org.muonmc.loader.api.gui.MuonLoaderGui;
import org.muonmc.loader.api.gui.MuonLoaderText;
import org.muonmc.loader.impl.MuonLoaderImpl;
import org.muonmc.loader.impl.game.GameProvider;
import org.muonmc.loader.impl.gui.MuonGuiEntry;
import org.muonmc.loader.impl.util.log.Log;
import org.muonmc.loader.impl.util.log.LogCategory;

import net.fabricmc.loader.impl.gui.FabricStatusTree.FabricBasicButtonType;
import net.fabricmc.loader.impl.gui.FabricStatusTree.FabricStatusTab;

/** @deprecated Replaced by the public APIs in {@link MuonLoaderGui} */
@Deprecated
public class FabricGuiEntry {

	/** Opens the given {@link FabricStatusTree} in a new swing window.
	 *
	 * @throws Exception if something went wrong while opening the window. */
	public static void open(FabricStatusTree tree) throws Exception {
		MuonLoaderGui.open(tree.toQuiltWindow());
	}

	/** @param exitAfter If true then this will call {@link System#exit(int)} after showing the gui, otherwise this will
	 *            return normally. */
	public static void displayCriticalError(Throwable exception, boolean exitAfter) {
		displayError(MuonLoaderText.translate("").toString(), exception, exitAfter);
	}

	public static void displayError(String mainText, Throwable exception, boolean exitAfter) {
		MuonGuiEntry.displayError(mainText, exception, true, exitAfter);
	}

	public static void displayError(String mainText, Throwable exception, Consumer<FabricStatusTree> treeCustomiser, boolean exitAfter) {
		GameProvider provider = MuonLoaderImpl.INSTANCE.tryGetGameProvider();

		if ((provider == null || provider.canOpenGui()) && !GraphicsEnvironment.isHeadless()) {
			FabricStatusTree tree = new FabricStatusTree("Quilt Loader " + MuonLoaderImpl.VERSION, mainText);
			FabricStatusTab crashTab = tree.addTab(MuonLoaderText.translate("tab.messages").toString());

			if (exception == null) {
				exception = new Error("Missing exception!");
			}
			crashTab.node.addCleanedException(exception);

			tree.addButton(MuonLoaderText.translate("button.exit").toString(), FabricBasicButtonType.CLICK_ONCE).makeClose();
			treeCustomiser.accept(tree);
			try {
				MuonLoaderGui.open(tree.toQuiltWindow());
			} catch (LoaderGuiException e) {
				if (exitAfter) {
					Log.warn(LogCategory.GENERAL, "Failed to open the error gui!", e);
				} else {
					throw new RuntimeException("Failed to open the error gui!", e);
				}
			}
		}

		if (exitAfter) {
			System.exit(1);
		}
	}
}
