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
import org.muonmc.loader.impl.report.QuiltReport;
import org.muonmc.loader.impl.util.QuiltLoaderInternal;
import org.muonmc.loader.impl.util.QuiltLoaderInternalType;
import org.muonmc.loader.impl.util.log.Log;
import org.muonmc.loader.impl.util.log.LogCategory;
import org.muonmc.loader.api.MuonLoader;
import org.muonmc.loader.api.gui.LoaderGuiException;
import org.muonmc.loader.api.gui.QuiltBasicWindow;
import org.muonmc.loader.api.gui.QuiltDisplayedError.QuiltErrorButton;
import org.muonmc.loader.api.gui.QuiltGuiMessagesTab;
import org.muonmc.loader.api.gui.QuiltLoaderGui;
import org.muonmc.loader.api.gui.QuiltLoaderText;
import org.muonmc.loader.impl.MuonLoaderImpl;

/** The main entry point for all quilt-based stuff. */
@QuiltLoaderInternal(QuiltLoaderInternalType.LEGACY_EXPOSED)
@Deprecated
public final class QuiltGuiEntry {
	/** @param exitAfter If true then this will call {@link System#exit(int)} after showing the gui, otherwise this will
	 *            return normally. */
	public static void displayError(String mainText, Throwable exception, boolean warnEarly, boolean exitAfter) {
		if (warnEarly) {
			Log.error(LogCategory.GUI, "An error occurred: " + mainText, exception);
		}

		GameProvider provider = MuonLoaderImpl.INSTANCE.tryGetGameProvider();

		if ((provider == null || provider.canOpenGui()) && !GraphicsEnvironment.isHeadless()) {

			QuiltReport report = new QuiltReport("Crashed!");
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
			} catch (QuiltReport.CrashReportSaveFailed e) {
				crashReportText = e.fullReportText;
			}

			String title = "Quilt Loader " + MuonLoaderImpl.VERSION;
			QuiltBasicWindow<Void> window = QuiltLoaderGui.createBasicWindow();
			window.title(QuiltLoaderText.of(title));
			window.mainText(QuiltLoaderText.of(mainText));

			QuiltGuiMessagesTab messages = window.addMessagesTab(QuiltLoaderText.EMPTY);
			window.restrictToSingleTab();

			QuiltJsonGuiMessage error = new QuiltJsonGuiMessage(null, "quilt_loader", QuiltLoaderText.translate("error.unhandled"));
			error.appendDescription(QuiltLoaderText.translate("error.unhandled_launch.desc"));
			error.setOrdering(-100);
			error.addOpenQuiltSupportButton();
			messages.addMessage(error);

			if (crashReportText != null) {
				error = new QuiltJsonGuiMessage(null, "quilt_loader", QuiltLoaderText.translate("error.failed_to_save_crash_report"));
				error.setIcon(GuiManagerImpl.ICON_LEVEL_ERROR);
				error.appendDescription(QuiltLoaderText.translate("error.failed_to_save_crash_report.desc"));
				error.appendAdditionalInformation(QuiltLoaderText.translate("error.failed_to_save_crash_report.info"));
				error.addCopyTextToClipboardButton(QuiltLoaderText.translate("button.copy_crash_report"), crashReportText);
				messages.addMessage(error);
			}

			if (crashReportFile != null) {
				window.addFileOpenButton(QuiltLoaderText.translate("button.open_crash_report"), crashReportFile);
				window.addCopyFileToClipboardButton(QuiltLoaderText.translate("button.copy_crash_report"), crashReportFile);
			}

			window.addFolderViewButton(QuiltLoaderText.translate("button.open_mods_folder"), MuonLoaderImpl.INSTANCE.getModsDir());

			QuiltErrorButton continueBtn = window.addContinueButton();
			continueBtn.text(QuiltLoaderText.translate("button.exit"));
			continueBtn.icon(QuiltLoaderGui.iconLevelError());

			try {
				QuiltLoaderGui.open(window);
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
