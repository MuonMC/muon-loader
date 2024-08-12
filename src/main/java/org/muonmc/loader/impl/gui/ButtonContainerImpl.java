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

import java.nio.file.Path;

import org.muonmc.loader.api.FasterFiles;
import org.muonmc.loader.api.gui.MuonDisplayedError.QuiltErrorButton;
import org.muonmc.loader.api.gui.MuonGuiButtonContainer;
import org.muonmc.loader.api.gui.MuonLoaderText;

interface ButtonContainerImpl extends MuonGuiButtonContainer {

	interface ButtonContainerListener {
		default void onButtonAdded(QuiltJsonButton button) {}
	}

	default QuiltJsonButton button(MuonLoaderText name, QuiltJsonButton.QuiltBasicButtonAction action) {
		return button(name, action, null);
	}

	default QuiltJsonButton button(MuonLoaderText name, QuiltJsonButton.QuiltBasicButtonAction action, Runnable run) {
		return addButton(new QuiltJsonButton(getThis(), name.toString(), null, action, run));
	}

	QuiltGuiSyncBase getThis();

	QuiltJsonButton addButton(QuiltJsonButton button);

	@Override
	default QuiltErrorButton addFileViewButton(MuonLoaderText name, Path openedPath) {
		return button(name, QuiltJsonButton.QuiltBasicButtonAction.VIEW_FILE).arg("file", openedPath.toString());
	}

	@Override
	default QuiltErrorButton addFileEditButton(MuonLoaderText name, Path openedPath) {
		return button(name, QuiltJsonButton.QuiltBasicButtonAction.EDIT_FILE).arg("file", openedPath.toString());
	}

	@Override
	default QuiltErrorButton addFileOpenButton(MuonLoaderText name, Path openedPath) {
		return button(name, QuiltJsonButton.QuiltBasicButtonAction.OPEN_FILE).arg("file", openedPath.toString());
	}

	@Override
	default QuiltErrorButton addFolderViewButton(MuonLoaderText name, Path openedFolder) {
		if (FasterFiles.isRegularFile(openedFolder)) {
			return addFileViewButton(name, openedFolder);
		} else {
			return button(name, QuiltJsonButton.QuiltBasicButtonAction.VIEW_FOLDER)//
				.arg("folder", openedFolder.toString());
		}
	}

	@Override
	default QuiltErrorButton addOpenLinkButton(MuonLoaderText name, String url) {
		return button(name, QuiltJsonButton.QuiltBasicButtonAction.OPEN_WEB_URL).arg("url", url);
	}

	@Override
	default QuiltErrorButton addOpenQuiltSupportButton() {
		return addButton(QuiltJsonButton.createUserSupportButton(getThis()));
	}

	@Override
	default QuiltErrorButton addCopyTextToClipboardButton(MuonLoaderText name, String fullText) {
		return button(name, QuiltJsonButton.QuiltBasicButtonAction.PASTE_CLIPBOARD_TEXT).arg("text", fullText);
	}

	@Override
	default QuiltErrorButton addCopyFileToClipboardButton(MuonLoaderText name, Path openedFile) {
		return button(name, QuiltJsonButton.QuiltBasicButtonAction.PASTE_CLIPBOARD_FILE)//
			.arg("file", openedFile.toString());
	}

	@Override
	default QuiltErrorButton addOnceActionButton(MuonLoaderText name, MuonLoaderText disabledText, Runnable action) {
		QuiltJsonButton button = button(name, QuiltJsonButton.QuiltBasicButtonAction.RETURN_SIGNAL_ONCE, action);
		button.disabledText = disabledText.toString();
		return button;
	}

	@Override
	default QuiltErrorButton addActionButton(MuonLoaderText name, Runnable action) {
		return button(name, QuiltJsonButton.QuiltBasicButtonAction.RETURN_SIGNAL_MANY, action);
	}
}
