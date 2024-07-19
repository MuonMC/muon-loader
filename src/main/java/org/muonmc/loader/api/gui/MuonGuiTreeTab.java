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

public interface MuonGuiTreeTab extends MuonGuiTab {

	/** @return The root node. This node isn't displayed in the gui, but all of its children are. */
	MuonTreeNode rootNode();

	/** @return The current {@link MuonWarningLevel} for this tab. The default is to inherit the level directly from
	 *         the {@link #rootNode()}s {@link MuonTreeNode#maximumLevel()} */
	@Override
	MuonWarningLevel level();

	/** Sets the level for this tab. This also sets {@link #inheritLevel(boolean)} to false.
	 * 
	 * @param level The new {@link MuonWarningLevel}.
	 * @return this. */
	@Override
	MuonGuiTab level(MuonWarningLevel level);

	/** Defaults to true.
	 * 
	 * @param should If true then this {@link #level()} will be based off the {@link #rootNode()}
	 *            {@link MuonTreeNode#maximumLevel()}.
	 * @return this */
	MuonGuiTreeTab inheritLevel(boolean should);

	/** Controls whether nodes are visible in the GUI. Defaults to {@link MuonWarningLevel#NONE}, which means that
	 * {@link MuonWarningLevel#DEBUG_ONLY} nodes aren't shown. */
	MuonGuiTreeTab visibilityLevel(MuonWarningLevel level);
}
