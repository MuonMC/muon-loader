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

package org.muonmc.loader.impl.gui;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.muonmc.loader.impl.MuonConstants;
import org.muonmc.loader.impl.game.GameProvider;
import org.muonmc.loader.impl.util.LoaderValueHelper;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;
import org.muonmc.loader.impl.util.SystemProperties;
import org.muonmc.loader.impl.util.log.Log;
import org.muonmc.loader.impl.util.log.LogCategory;
import org.muonmc.loader.api.LoaderValue;
import org.muonmc.loader.api.LoaderValue.LObject;
import org.muonmc.loader.api.LoaderValue.LType;
import org.muonmc.loader.api.gui.LoaderGuiClosed;
import org.muonmc.loader.api.gui.LoaderGuiException;
import org.muonmc.loader.api.gui.MuonBasicWindow;
import org.muonmc.loader.api.gui.MuonDisplayedError;
import org.muonmc.loader.api.gui.MuonDisplayedError.QuiltErrorButton;
import org.muonmc.loader.api.gui.MuonGuiMessagesTab;
import org.muonmc.loader.api.gui.MuonLoaderGui;
import org.muonmc.loader.api.gui.MuonLoaderText;
import org.muonmc.loader.api.gui.MuonLoaderWindow;
import org.muonmc.loader.api.plugin.LoaderValueFactory;
import org.muonmc.loader.impl.MuonLoaderImpl;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public class MuonFork {

	private static final MuonForkComms COMMS;
	private static final LoaderValueHelper<IOException> HELPER = LoaderValueHelper.IO_EXCEPTION;
	private static final IOException FORK_EXCEPTION;
	private static Error previousServerError;

	static {
		GameProvider provider = MuonLoaderImpl.INSTANCE.getGameProvider();
		if (Boolean.getBoolean(SystemProperties.DISABLE_FORKED_GUIS) || !provider.canOpenGui()) {
			COMMS = null;
			FORK_EXCEPTION = null;
		} else {
			MuonForkComms comms = null;
			IOException error = null;
			try {
				File base = MuonLoaderImpl.INSTANCE.getLoaderCacheDir().resolve("comms").toFile();
				comms = MuonForkComms.connect(base, MuonFork::handleMessageFromServer);
			} catch (IOException e) {
				Log.error(LogCategory.GUI, "Failed to spawn the child process!", e);
				error = e;
			}
			COMMS = comms;
			FORK_EXCEPTION = error;
		}
	}

	public static void close() {
		if (COMMS != null) {
			COMMS.send(lvf().nul());
		}
	}

	private static LoaderValueFactory lvf() {
		return LoaderValueFactory.getFactory();
	}

	public static void openErrorGui(List<MuonDisplayedError> errors) throws LoaderGuiException, LoaderGuiClosed {
		MuonBasicWindow<Boolean> window = MuonLoaderGui.createBasicWindow(false);
		window.title(MuonLoaderText.of(MuonConstants.NAME + " " + MuonLoaderImpl.VERSION));
		window.addOpenQuiltSupportButton();
		QuiltErrorButton continueButton = window.addContinueButton();
		continueButton.text(MuonLoaderText.translate("button.ignore"));
		continueButton.icon(MuonLoaderGui.iconContinueIgnoring());
		MuonGuiMessagesTab messages = window.addMessagesTab(MuonLoaderText.EMPTY);
		window.restrictToSingleTab();
		for (MuonDisplayedError error : errors) {
			error.addOnFixedListener(() -> {
				for (MuonDisplayedError e : errors) {
					if (!e.isFixed()) {
						return;
					}
				}
				continueButton.text(MuonLoaderText.of("button.continue"));
				continueButton.icon(MuonLoaderGui.iconContinue());
				window.returnValue(true);
			});
			messages.addMessage(error);
		}

		if (!MuonLoaderGui.open(window)) {
			throw LoaderGuiClosed.INSTANCE;
		}
	}

	public static <R> R open(MuonLoaderWindow<R> window) throws LoaderGuiException {
		open(window, true);
		return window.returnValue();
	}

	public static void open(MuonLoaderWindow<?> window, boolean shouldWait) throws LoaderGuiException {
		if (COMMS == null) {
			if (FORK_EXCEPTION != null) {
				// Gui NOT disabled, but it failed to open
				throw new LoaderGuiException("Failed to spawn the child process previously", FORK_EXCEPTION);
			}
			// Gui disabled, act as if the user pressed "ignore" or just closed the window immediately
			return;
		}

		if (previousServerError != null) {
			// Gui NOT disabled, but it previously crashed.
			throw new LoaderGuiException("The gui server encountered an error!", previousServerError);
		}

		AbstractWindow<?> realWindow = (AbstractWindow<?>) window;

		realWindow.send();
		realWindow.open();

		if (shouldWait) {
			realWindow.waitUntilClosed(() -> previousServerError);
		}
	}

	static void uploadIcon(int index, Map<Integer, BufferedImage> images) {
		if (COMMS == null) {
			return;
		}
		Map<String, LoaderValue> map = new HashMap<>();
		map.put("__TYPE", lvf().string(ForkCommNames.ID_UPLOAD_ICON));
		map.put("index", lvf().number(index));
		Map<String, LoaderValue> imageMap = new HashMap<>();
		for (Map.Entry<Integer, BufferedImage> entry : images.entrySet()) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				ImageIO.write(entry.getValue(), "png", baos);
			} catch (IOException e) {
				throw new Error("Failed to write image!", e);
			}
			String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
			imageMap.put(entry.getKey().toString(), lvf().string(base64));
		}
		map.put("images", lvf().object(imageMap));
		COMMS.send(lvf().object(map));
	}

	private static void handleMessageFromServer(LoaderValue msg) {
		try {
			handleMessageFromServer0(msg);
		} catch (IOException io) {
			String json = "<failed to turn sent json into a string>";
			try {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				LoaderValueFactory.getFactory().write(msg, baos);
				json = new String(baos.toByteArray(), StandardCharsets.UTF_8);
			} catch (IOException e) {
				io.addSuppressed(e);
			}
			throw new Error("Failed to handle json message\n" + json, io);
		}
	}

	private static void handleMessageFromServer0(LoaderValue msg) throws IOException {
		if (msg.type() == LType.NULL) {
			return;
		} else if (msg.type() == LType.OBJECT) {
			LObject packet = msg.asObject();
			String type = HELPER.expectString(packet, "__TYPE");
			switch (type) {
				case ForkCommNames.ID_EXCEPTION: {
					// The server encountered an exception
					LoaderValue detail = packet.get("detail");
					if (detail == null || detail.type() != LType.STRING) {
						Log.error(LogCategory.COMMS, "The gui-server encountered an unknown error!");
						previousServerError = new Error("[Unknown error: the gui server didn't send a detail trace]");
					} else {
						Log.error(LogCategory.COMMS, "The gui-server encountered an error:\n" + detail.asString());
						previousServerError = new Error("Previous error stacktrace:\n" + detail.asString());
					}
					COMMS.close();
					return;
				}
				case ForkCommNames.ID_GUI_OBJECT_UPDATE: {
					QuiltGuiSyncBase.updateObject(packet);
					return;
				}
			}
		}
		throw new Error("Unhandled message " + msg);
	}
}
