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

public interface QuiltGuiButtonContainer {

	/** Adds a button to this error, which will open a file browser, selecting the given file. */
	default QuiltDisplayedError.QuiltErrorButton addFileViewButton(Path openedPath) {
		return addFileViewButton(QuiltLoaderText.translate("button.view_file", openedPath.getFileName()), openedPath);
	}

	/** Adds a button to this error, which will open a file browser, selecting the given file. */
	QuiltDisplayedError.QuiltErrorButton addFileViewButton(QuiltLoaderText name, Path openedPath);

	/** Adds a button to this error, which will open a file editor, editing the given file. */
	default QuiltDisplayedError.QuiltErrorButton addFileEditButton(Path openedPath) {
		return addFileEditButton(QuiltLoaderText.translate("button.edit_file", openedPath.getFileName()), openedPath);
	}

	/** Adds a button to this error, which will open a file editor, editing the given file. */
	QuiltDisplayedError.QuiltErrorButton addFileEditButton(QuiltLoaderText name, Path openedPath);

	/** Adds a button to this error which will open a file viewer for the given file. */
	default QuiltDisplayedError.QuiltErrorButton addFileOpenButton(Path openedPath) {
		return addFileOpenButton(QuiltLoaderText.translate("button.open_file", openedPath.getFileName()), openedPath);
	}

	/** Adds a button to this error, which will open a file viewer, viewing the given file. */
	QuiltDisplayedError.QuiltErrorButton addFileOpenButton(QuiltLoaderText name, Path openedPath);

	/** Adds a button to this error, which will open a file browser showing the selected folder. */
	QuiltDisplayedError.QuiltErrorButton addFolderViewButton(QuiltLoaderText name, Path openedFolder);

	/** Adds a button to this error, which will open the specified URL in a browser window. */
	QuiltDisplayedError.QuiltErrorButton addOpenLinkButton(QuiltLoaderText name, String url);

	/** Adds a button to this error, which opens the Muon user support forum. */
	QuiltDisplayedError.QuiltErrorButton addOpenQuiltSupportButton();

	QuiltDisplayedError.QuiltErrorButton addCopyTextToClipboardButton(QuiltLoaderText name, String fullText);

	QuiltDisplayedError.QuiltErrorButton addCopyFileToClipboardButton(QuiltLoaderText name, Path openedFile);

	QuiltDisplayedError.QuiltErrorButton addOnceActionButton(QuiltLoaderText name, QuiltLoaderText disabledText, Runnable action);

	QuiltDisplayedError.QuiltErrorButton addActionButton(QuiltLoaderText name, Runnable action);
}
