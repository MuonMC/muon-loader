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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.muonmc.loader.api.LoaderValue;
import org.muonmc.loader.api.LoaderValue.LObject;
import org.muonmc.loader.api.LoaderValue.LType;
import org.muonmc.loader.api.gui.MuonGuiTab;
import org.muonmc.loader.api.gui.MuonLoaderIcon;
import org.muonmc.loader.api.gui.MuonLoaderText;
import org.muonmc.loader.api.gui.MuonWarningLevel;

abstract class AbstractTab extends QuiltGuiSyncBase implements MuonGuiTab {

	interface TabChangeListener extends Listener {
		default void onIconChanged() {}

		default void onTextChanged() {}

		default void onLevelChanged() {}
	}

	PluginIconImpl icon;
	private MuonLoaderText apiText;

	String text;
	private MuonWarningLevel level = MuonWarningLevel.NONE;

	AbstractTab(BasicWindow<?> parent, MuonLoaderText text) {
		super(parent);
		icon(null);
		text(text);
	}

	AbstractTab(QuiltGuiSyncBase parent, LObject obj) throws IOException {
		super(parent, obj);
		icon = obj.containsKey("icon") ? readChild(HELPER.expectValue(obj, "icon"), PluginIconImpl.class) : null;
		text = HELPER.expectString(obj, "text");
		level = HELPER.expectEnum(MuonWarningLevel.class, obj, "level");
	}

	@Override
	protected void write0(Map<String, LoaderValue> map) {
		if (icon != null) {
			map.put("icon", writeChild(icon));
		}
		map.put("text", lvf().string(text));
		map.put("level", lvf().string(level.name()));
	}

	@Override
	void handleUpdate(String name, LObject data) throws IOException {
		switch (name) {
			case "set_icon": {
				LoaderValue value = HELPER.expectValue(data, "icon");
				if (value.type() == LType.NULL) {
					this.icon = null;
				} else {
					this.icon = readChild(HELPER.expectValue(data, "icon"), PluginIconImpl.class);
				}
				invokeListeners(TabChangeListener.class, TabChangeListener::onIconChanged);
				break;
			}
			case "set_text": {
				this.text = HELPER.expectString(data, "text");
				invokeListeners(TabChangeListener.class, TabChangeListener::onTextChanged);
				break;
			}
			case "set_level": {
				this.level = HELPER.expectEnum(MuonWarningLevel.class, data, "level");
				invokeListeners(TabChangeListener.class, TabChangeListener::onLevelChanged);
				break;
			}
			default: {
				super.handleUpdate(name, data);
			}
		}
	}

	@Override
	public MuonLoaderIcon icon() {
		return icon;
	}

	@Override
	public MuonGuiTab icon(MuonLoaderIcon icon) {
		this.icon = PluginIconImpl.fromApi(icon);
		invokeListeners(TabChangeListener.class, TabChangeListener::onIconChanged);
		if (shouldSendUpdates()) {
			Map<String, LoaderValue> map = new HashMap<>();
			map.put("icon", this.icon != null ? writeChild(this.icon) : lvf().nul());
			sendUpdate("set_icon", lvf().object(map));
		}
		return this;
	}

	@Override
	public MuonLoaderText text() {
		return apiText;
	}

	@Override
	public MuonGuiTab text(MuonLoaderText text) {
		this.apiText = text;
		this.text = text.toString();
		invokeListeners(TabChangeListener.class, TabChangeListener::onTextChanged);
		if (shouldSendUpdates()) {
			Map<String, LoaderValue> map = new HashMap<>();
			map.put("text", lvf().string(this.text));
			sendUpdate("set_text", lvf().object(map));
		}
		return this;
	}

	@Override
	public MuonWarningLevel level() {
		return level;
	}

	@Override
	public MuonGuiTab level(MuonWarningLevel level) {
		this.level = level;
		invokeListeners(TabChangeListener.class, TabChangeListener::onLevelChanged);
		if (shouldSendUpdates()) {
			Map<String, LoaderValue> map = new HashMap<>();
			map.put("level", lvf().string(level.name()));
			sendUpdate("set_level", lvf().object(map));
		}
		return this;
	}

	abstract MuonWarningLevel getInheritedLevel();
}
