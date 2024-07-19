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

package org.muonmc.loader.impl.gui;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.muonmc.loader.api.LoaderValue;
import org.muonmc.loader.api.LoaderValue.LObject;
import org.muonmc.loader.api.gui.MuonGuiTab;
import org.muonmc.loader.api.gui.MuonGuiTreeTab;
import org.muonmc.loader.api.gui.MuonLoaderText;
import org.muonmc.loader.api.gui.MuonTreeNode;
import org.muonmc.loader.api.gui.MuonWarningLevel;

class TreeTab extends AbstractTab implements MuonGuiTreeTab {

	final MuonStatusNode rootNode;
	boolean inheritLevel = true;
	MuonWarningLevel visibilityLevel = MuonWarningLevel.NONE;

	TreeTab(BasicWindow<?> parent, MuonLoaderText text) {
		super(parent, text);
		rootNode = new MuonStatusNode(this);
	}

	TreeTab(BasicWindow<?> parent, MuonLoaderText text, MuonStatusNode rootNode) {
		super(parent, text);
		if (rootNode.parent != null) {
			throw new IllegalArgumentException("Cannot use a different root node if the other root node already has a parent!");
		}
		this.rootNode = rootNode;
	}

	TreeTab(QuiltGuiSyncBase parent, LObject obj) throws IOException {
		super(parent, obj);
		rootNode = readChild(HELPER.expectValue(obj, "root_node"), MuonStatusNode.class);
		inheritLevel = HELPER.expectBoolean(obj, "inherit_level");
		visibilityLevel = HELPER.expectEnum(MuonWarningLevel.class, obj, "visibilityLevel");
	}

	@Override
	protected void write0(Map<String, LoaderValue> map) {
		super.write0(map);
		map.put("root_node", writeChild(rootNode));
		map.put("inherit_level", lvf().bool(inheritLevel));
		map.put("visibilityLevel", lvf().string(visibilityLevel.name()));
	}

	@Override
	void handleUpdate(String name, LObject data) throws IOException {
		switch (name) {
			case "set_inherit_level": {
				this.inheritLevel = HELPER.expectBoolean(data, "inherit_level");
				invokeListeners(TabChangeListener.class, TabChangeListener::onLevelChanged);
				break;
			}
			default: {
				super.handleUpdate(name, data);
			}
		}
	}

	@Override
	MuonWarningLevel getInheritedLevel() {
		return rootNode.maximumLevel();
	}

	@Override
	String syncType() {
		return "tree_tab";
	}

	@Override
	public MuonTreeNode rootNode() {
		return rootNode;
	}

	@Override
	public MuonWarningLevel level() {
		if (inheritLevel) {
			return rootNode.maximumLevel();
		} else {
			return super.level();
		}
	}

	@Override
	public MuonGuiTab level(MuonWarningLevel level) {
		inheritLevel(false);
		return super.level(level);
	}

	@Override
	public MuonGuiTreeTab inheritLevel(boolean should) {
		this.inheritLevel = should;
		if (shouldSendUpdates()) {
			Map<String, LoaderValue> map = new HashMap<>();
			map.put("inherit_level", lvf().bool(should));
			sendUpdate("set_inherit_level", lvf().object(map));
		}
		return this;
	}


	@Override
	public MuonGuiTreeTab visibilityLevel(MuonWarningLevel level) {
		visibilityLevel = level;
		return this;
	}
}
