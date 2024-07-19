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

package org.muonmc.loader.impl.gui;

import java.awt.GraphicsEnvironment;
import java.nio.file.Path;

import org.muonmc.loader.impl.game.GameProvider;
import org.muonmc.loader.impl.report.MuonReport;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;
import org.muonmc.loader.impl.util.log.Log;
import org.muonmc.loader.impl.util.log.LogCategory;
import org.muonmc.loader.api.MuonLoader;
import org.muonmc.loader.api.gui.LoaderGuiException;
import org.muonmc.loader.api.gui.MuonBasicWindow;
import org.muonmc.loader.api.gui.MuonDisplayedError.QuiltErrorButton;
import org.muonmc.loader.api.gui.MuonGuiMessagesTab;
import org.muonmc.loader.api.gui.MuonLoaderGui;
import org.muonmc.loader.api.gui.MuonLoaderText;
import org.muonmc.loader.impl.MuonLoaderImpl;

/** The main entry point for all Muon-based stuff. */
@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public final class MuonGuiEntry {
	/** @param exitAfter If true then this will call {@link System#exit(int)} after showing the gui, otherwise this will
	 *            return normally. */
	public static void displayError(String mainText, Throwable exception, boolean warnEarly, boolean exitAfter) {
		if (warnEarly) {
			Log.error(LogCategory.GUI, "An error occurred: " + mainText, exception);
		}

		GameProvider provider = MuonLoaderImpl.INSTANCE.tryGetGameProvider();

		if ((provider == null || provider.canOpenGui()) && !GraphicsEnvironment.isHeadless()) {

			MuonReport report = new MuonReport("Crashed!");
			// It's arguably the most important version - if anything goes wrong while writing this report
			// at least we know what code was used to generate it.
			report.overview("Quilt Loader Version: " + MuonLoaderImpl.VERSION);
			report.addStacktraceSection("Crash", 0, exception);
			try {
				MuonLoaderImpl.INSTANCE.appendModTable(report.addStringSection("Mods", 0)::lines);
			} catch (Throwable t) {
				report.addStacktraceSection("Exception while building the mods table", 0, t);
			}

			Path crashReportFile = null;
			String crashReportText = null;
			try {
				crashReportFile = report.writeInDirectory(MuonLoader.getGameDir());
			} catch (MuonReport.CrashReportSaveFailed e) {
				crashReportText = e.fullReportText;
			}

			String title = "Quilt Loader " + MuonLoaderImpl.VERSION;
			MuonBasicWindow<Void> window = MuonLoaderGui.createBasicWindow();
			window.title(MuonLoaderText.of(title));
			window.mainText(MuonLoaderText.of(mainText));

			MuonGuiMessagesTab messages = window.addMessagesTab(MuonLoaderText.EMPTY);
			window.restrictToSingleTab();

			MuonJsonGuiMessage error = new MuonJsonGuiMessage(null, "quilt_loader", MuonLoaderText.translate("error.unhandled"));
			error.appendDescription(MuonLoaderText.translate("error.unhandled_launch.desc"));
			error.setOrdering(-100);
			error.addOpenQuiltSupportButton();
			messages.addMessage(error);

			if (crashReportText != null) {
				error = new MuonJsonGuiMessage(null, "quilt_loader", MuonLoaderText.translate("error.failed_to_save_crash_report"));
				error.setIcon(GuiManagerImpl.ICON_LEVEL_ERROR);
				error.appendDescription(MuonLoaderText.translate("error.failed_to_save_crash_report.desc"));
				error.appendAdditionalInformation(MuonLoaderText.translate("error.failed_to_save_crash_report.info"));
				error.addCopyTextToClipboardButton(MuonLoaderText.translate("button.copy_crash_report"), crashReportText);
				messages.addMessage(error);
			}

			if (crashReportFile != null) {
				window.addFileOpenButton(MuonLoaderText.translate("button.open_crash_report"), crashReportFile);
				window.addCopyFileToClipboardButton(MuonLoaderText.translate("button.copy_crash_report"), crashReportFile);
			}

			window.addFolderViewButton(MuonLoaderText.translate("button.open_mods_folder"), MuonLoaderImpl.INSTANCE.getModsDir());

			QuiltErrorButton continueBtn = window.addContinueButton();
			continueBtn.text(MuonLoaderText.translate("button.exit"));
			continueBtn.icon(MuonLoaderGui.iconLevelError());

			try {
				MuonLoaderGui.open(window);
			} catch (LoaderGuiException e) {
				if (exitAfter) {
					Log.warn(LogCategory.GUI, "Failed to open the error gui!", e);
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
