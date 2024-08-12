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

package org.muonmc.loader.api.gui;

public interface MuonGuiTab {

	/** @return The current icon for this tab. */
	MuonLoaderIcon icon();

	/** Sets the icon for this tab.
	 * 
	 * @param icon The new icon.
	 * @return this. */
	MuonGuiTab icon(MuonLoaderIcon icon);

	/** @return The current text for this tab. */
	MuonLoaderText text();

	/** Sets the text for this tab.
	 * 
	 * @param text The new text.
	 * @return this. */
	MuonGuiTab text(MuonLoaderText text);

	/** @return The current {@link MuonWarningLevel} for this tab. The default is {@link MuonWarningLevel#NONE} */
	MuonWarningLevel level();

	/** Sets the level for this tab.
	 * 
	 * @param level The new {@link MuonWarningLevel}.
	 * @return this. */
	MuonGuiTab level(MuonWarningLevel level);
}
