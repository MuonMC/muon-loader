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

import java.nio.file.Path;

public interface MuonGuiButtonContainer {

	/** Adds a button to this error, which will open a file browser, selecting the given file. */
	default MuonDisplayedError.QuiltErrorButton addFileViewButton(Path openedPath) {
		return addFileViewButton(MuonLoaderText.translate("button.view_file", openedPath.getFileName()), openedPath);
	}

	/** Adds a button to this error, which will open a file browser, selecting the given file. */
	MuonDisplayedError.QuiltErrorButton addFileViewButton(MuonLoaderText name, Path openedPath);

	/** Adds a button to this error, which will open a file editor, editing the given file. */
	default MuonDisplayedError.QuiltErrorButton addFileEditButton(Path openedPath) {
		return addFileEditButton(MuonLoaderText.translate("button.edit_file", openedPath.getFileName()), openedPath);
	}

	/** Adds a button to this error, which will open a file editor, editing the given file. */
	MuonDisplayedError.QuiltErrorButton addFileEditButton(MuonLoaderText name, Path openedPath);

	/** Adds a button to this error which will open a file viewer for the given file. */
	default MuonDisplayedError.QuiltErrorButton addFileOpenButton(Path openedPath) {
		return addFileOpenButton(MuonLoaderText.translate("button.open_file", openedPath.getFileName()), openedPath);
	}

	/** Adds a button to this error, which will open a file viewer, viewing the given file. */
	MuonDisplayedError.QuiltErrorButton addFileOpenButton(MuonLoaderText name, Path openedPath);

	/** Adds a button to this error, which will open a file browser showing the selected folder. */
	MuonDisplayedError.QuiltErrorButton addFolderViewButton(MuonLoaderText name, Path openedFolder);

	/** Adds a button to this error, which will open the specified URL in a browser window. */
	MuonDisplayedError.QuiltErrorButton addOpenLinkButton(MuonLoaderText name, String url);

	/** Adds a button to this error, which opens the Muon user support forum. */
	MuonDisplayedError.QuiltErrorButton addOpenQuiltSupportButton();

	MuonDisplayedError.QuiltErrorButton addCopyTextToClipboardButton(MuonLoaderText name, String fullText);

	MuonDisplayedError.QuiltErrorButton addCopyFileToClipboardButton(MuonLoaderText name, Path openedFile);

	MuonDisplayedError.QuiltErrorButton addOnceActionButton(MuonLoaderText name, MuonLoaderText disabledText, Runnable action);

	MuonDisplayedError.QuiltErrorButton addActionButton(MuonLoaderText name, Runnable action);
}
