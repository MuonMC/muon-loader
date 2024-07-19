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

package org.muonmc.loader.api.gui;

import java.nio.file.Path;

import org.jetbrains.annotations.ApiStatus;

/** A reported error during plugin loading, which is shown in the error screen. This doesn't necessarily indicate an
 * error - however reporting any errors will cause the plugin loading to halt at the end of the current cycle. */
@ApiStatus.NonExtendable
public interface MuonDisplayedError extends MuonGuiButtonContainer {

	/** Adds more lines which are shown in the log and the crash report file, NOT in the gui.
	 * 
	 * @return this. */
	MuonDisplayedError appendReportText(String... lines);

	/** Adds more lines of description. */
	MuonDisplayedError appendDescription(MuonLoaderText... descriptions);

	/** Removes all description text that was added by {@link #appendDescription(MuonLoaderText...)}. Useful for
	 * changing the description after the window has already been shown. */
	MuonDisplayedError clearDescription();

	MuonDisplayedError setOrdering(int priority);

	/** Adds more lines of additional information, which is hidden from the user by default. */
	MuonDisplayedError appendAdditionalInformation(MuonLoaderText... information);

	/** Removes all additional text that was added by {@link #appendAdditionalInformation(MuonLoaderText...)}. Useful
	 * for changing the description after the window has already been shown. */
	MuonDisplayedError clearAdditionalInformation();

	/** Adds a {@link Throwable} to this error - which will be included in the crash-report file, but will not be shown
	 * in the gui. */
	MuonDisplayedError appendThrowable(Throwable t);

	MuonDisplayedError title(MuonLoaderText text);

	MuonLoaderIcon icon();

	/** Defaults to {@link MuonLoaderGui#iconLevelError()}. */
	MuonDisplayedError setIcon(MuonLoaderIcon icon);

	// For backwards compatibility we need to keep these button methods around.

	/** Adds a button to this error, which will open a file browser, selecting the given file. */
	@Override
	default QuiltErrorButton addFileViewButton(Path openedPath) {
		return MuonGuiButtonContainer.super.addFileViewButton(openedPath);
	}

	/** Adds a button to this error, which will open a file browser, selecting the given file. */
	@Override
	QuiltErrorButton addFileViewButton(MuonLoaderText name, Path openedPath);

	/** Adds a button to this error, which will open a file editor, editing the given file. */
	@Override
	default QuiltErrorButton addFileEditButton(Path openedPath) {
		return MuonGuiButtonContainer.super.addFileEditButton(openedPath);
	}

	/** Adds a button to this error, which will open a file editor, editing the given file. */
	@Override
	QuiltErrorButton addFileEditButton(MuonLoaderText name, Path openedPath);

	/** Adds a button to this error, which will open a file browser showing the selected folder. */
	@Override
	QuiltErrorButton addFolderViewButton(MuonLoaderText name, Path openedFolder);

	/** Adds a button to this error, which will open the specified URL in a browser window. */
	@Override
	QuiltErrorButton addOpenLinkButton(MuonLoaderText name, String url);

	/** Adds a button to this error, which opens the quilt user support forum. */
	@Override
	QuiltErrorButton addOpenQuiltSupportButton();

	@Override
	QuiltErrorButton addCopyTextToClipboardButton(MuonLoaderText name, String fullText);

	@Override
	QuiltErrorButton addCopyFileToClipboardButton(MuonLoaderText name, Path openedFile);

	@Override
	QuiltErrorButton addOnceActionButton(MuonLoaderText name, MuonLoaderText disabledText, Runnable action);

	@Override
	QuiltErrorButton addActionButton(MuonLoaderText name, Runnable action);

	/** Changes this error message to be "fixed". If {@link #setIcon(MuonLoaderIcon)} hasn't been called then the icon
	 * is set to {@link MuonLoaderGui#iconTick()} */
	void setFixed();

	/** @return True if {@link #setFixed()} has been called. */
	boolean isFixed();

	/** Adds an action that will be ran when {@link #setFixed()} is called. */
	void addOnFixedListener(Runnable action);

	@ApiStatus.NonExtendable
	public interface QuiltErrorButton {
		QuiltErrorButton text(MuonLoaderText text);

		MuonLoaderIcon icon();

		QuiltErrorButton icon(MuonLoaderIcon icon);

		/** Enables this button. This is the default state. */
		default void enable() {
			setEnabled(true, null);
		}

		/** Changes the "enabled" state of this button, which controls whether the action associated with this button
		 * can run.
		 * 
		 * @param enabled
		 * @param disabledMessage Shown when the user hovers over the button and it's disabled. This is ignored when
		 *            enabled is true. */
		void setEnabled(boolean enabled, MuonLoaderText disabledMessage);
	}
}
