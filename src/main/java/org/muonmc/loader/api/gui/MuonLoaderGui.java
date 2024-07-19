/*
 * Copyright 2023 QuiltMC
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

package org.muonmc.loader.api.gui;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.muonmc.loader.impl.gui.GuiManagerImpl;
import org.muonmc.loader.impl.gui.QuiltFork;
import org.muonmc.loader.impl.gui.MuonJsonGuiMessage;
import org.muonmc.loader.impl.gui.QuiltLoaderGuiImpl;
import org.muonmc.loader.api.ModContainer;
import org.muonmc.loader.api.MuonLoader;

/** Central API for dealing with opening guis on a separate process. Used since some games don't work properly on macOS
 * if we open a swing window in the main process. */
public class MuonLoaderGui {
	private MuonLoaderGui() {}

	// Gui opening

	/** Creates a new error to be displayed in {@link #openErrorGui(MuonDisplayedError)}. This doesn't do anything
	 * else.
	 * 
	 * @return A new {@link MuonDisplayedError}. */
	public static MuonDisplayedError createError() {
		return createError(MuonLoaderText.EMPTY);
	}

	/** Creates a new error to be displayed in {@link #openErrorGui(MuonDisplayedError)}. This doesn't do anything
	 * else.
	 * 
	 * @return A new {@link MuonDisplayedError}. */
	public static MuonDisplayedError createError(MuonLoaderText title) {
		return new MuonJsonGuiMessage(null, null, title);
	}

	/** @throws LoaderGuiException if something went wrong while opening the gui
	 * @throws LoaderGuiClosed if the gui was closed without fixing the errors. */
	public static void openErrorGui(MuonDisplayedError error) throws LoaderGuiException, LoaderGuiClosed {
		openErrorGui(Collections.singletonList(error));
	}

	/** @throws LoaderGuiException if something went wrong while opening the gui
	 * @throws LoaderGuiClosed if the gui was closed without fixing the errors. */
	public static void openErrorGui(MuonDisplayedError... errors) throws LoaderGuiException, LoaderGuiClosed {
		openErrorGui(Arrays.asList(errors));
	}

	/** @throws LoaderGuiException if something went wrong while opening the gui
	 * @throws LoaderGuiClosed if the gui was closed without fixing the errors. */
	public static void openErrorGui(List<MuonDisplayedError> errors) throws LoaderGuiException, LoaderGuiClosed {
		QuiltFork.openErrorGui(errors);
	}

	/** @return A new {@link MuonBasicWindow}. This hasn't been displayed yet.
	 * @see #open(MuonLoaderWindow) */
	public static <R> MuonBasicWindow<R> createBasicWindow(R defaultReturnValue) {
		return QuiltLoaderGuiImpl.createBasicWindow(defaultReturnValue);
	}

	/** @return A new {@link MuonBasicWindow}. This hasn't been displayed yet.
	 * @see #open(MuonLoaderWindow) */
	public static MuonBasicWindow<Void> createBasicWindow() {
		return createBasicWindow(null);
	}

	/** @return A new {@link MuonTreeNode} that can be passed to many different windows rather than being limited to
	 *         just one. */
	public static MuonTreeNode createTreeNode() {
		return QuiltLoaderGuiImpl.createTreeNode();
	}

	/** Opens a window, waiting for the user to close it before returning the {@link MuonLoaderWindow#returnValue()}.
	 * 
	 * @throws LoaderGuiException if something went wrong while opening the gui */
	public static <R> R open(MuonLoaderWindow<R> window) throws LoaderGuiException {
		return QuiltFork.open(window);
	}

	/** @throws LoaderGuiException if something went wrong while opening the gui */
	public static void open(MuonLoaderWindow<?> window, boolean shouldWait) throws LoaderGuiException {
		QuiltFork.open(window, shouldWait);
	}

	// Icons

	public static MuonLoaderIcon createIcon(byte[] imageBytes) {
		return createIcon(new byte[][] { imageBytes });
	}

	/** @param images Array of differently sized images, to be chosen by the UI. */
	public static MuonLoaderIcon createIcon(byte[][] images) {
		return GuiManagerImpl.allocateIcons(images);
	}

	public static MuonLoaderIcon createIcon(Map<Integer, BufferedImage> images) {
		return GuiManagerImpl.allocateIcons(images);
	}

	public static MuonLoaderIcon getModIcon(ModContainer mod) {
		return GuiManagerImpl.getModIcon(mod);
	}

	public static MuonLoaderIcon getModIcon(String modid) {
		return getModIcon(MuonLoader.getModContainer(modid).orElse(null));
	}

	// Builtin Icons

	public static MuonLoaderIcon iconContinue() {
		return GuiManagerImpl.ICON_CONTINUE;
	}

	public static MuonLoaderIcon iconContinueIgnoring() {
		return GuiManagerImpl.ICON_CONTINUE_BUT_IGNORE;
	}

	public static MuonLoaderIcon iconReload() {
		return GuiManagerImpl.ICON_RELOAD;
	}

	public static MuonLoaderIcon iconFolder() {
		return GuiManagerImpl.ICON_FOLDER;
	}

	public static MuonLoaderIcon iconUnknownFile() {
		return GuiManagerImpl.ICON_GENERIC_FILE;
	}

	public static MuonLoaderIcon iconTextFile() {
		return GuiManagerImpl.ICON_TEXT_FILE;
	}

	public static MuonLoaderIcon iconZipFile() {
		return GuiManagerImpl.ICON_ZIP;
	}

	public static MuonLoaderIcon iconJarFile() {
		return GuiManagerImpl.ICON_JAR;
	}

	public static MuonLoaderIcon iconJsonFile() {
		return GuiManagerImpl.ICON_JSON;
	}

	public static MuonLoaderIcon iconJavaClassFile() {
		return GuiManagerImpl.ICON_JAVA_CLASS;
	}

	public static MuonLoaderIcon iconPackage() {
		return GuiManagerImpl.ICON_PACKAGE;
	}

	public static MuonLoaderIcon iconJavaPackage() {
		return GuiManagerImpl.ICON_JAVA_PACKAGE;
	}

	public static MuonLoaderIcon iconDisabled() {
		return GuiManagerImpl.ICON_DISABLED;
	}

	public static MuonLoaderIcon iconQuilt() {
		return GuiManagerImpl.ICON_MUON;
	}

	public static MuonLoaderIcon iconFabric() {
		return GuiManagerImpl.ICON_FABRIC;
	}

	public static MuonLoaderIcon iconWeb() {
		return GuiManagerImpl.ICON_WEB_LINK;
	}

	public static MuonLoaderIcon iconClipboard() {
		return GuiManagerImpl.ICON_CLIPBOARD;
	}

	public static MuonLoaderIcon iconTick() {
		return GuiManagerImpl.ICON_TICK;
	}

	public static MuonLoaderIcon iconCross() {
		return GuiManagerImpl.ICON_CROSS;
	}

	public static MuonLoaderIcon iconTreeDot() {
		return GuiManagerImpl.ICON_TREE_DOT;
	}

	public static MuonLoaderIcon iconLevelFatal() {
		return GuiManagerImpl.ICON_LEVEL_FATAL;
	}

	public static MuonLoaderIcon iconLevelError() {
		return GuiManagerImpl.ICON_LEVEL_ERROR;
	}

	public static MuonLoaderIcon iconLevelWarn() {
		return GuiManagerImpl.ICON_LEVEL_WARN;
	}

	public static MuonLoaderIcon iconLevelConcern() {
		return GuiManagerImpl.ICON_LEVEL_CONCERN;
	}

	public static MuonLoaderIcon iconLevelInfo() {
		return GuiManagerImpl.ICON_LEVEL_INFO;
	}
}
