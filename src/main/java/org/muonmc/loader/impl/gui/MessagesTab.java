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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.muonmc.loader.api.LoaderValue;
import org.muonmc.loader.api.LoaderValue.LObject;
import org.muonmc.loader.api.gui.MuonDisplayedError;
import org.muonmc.loader.api.gui.MuonGuiMessagesTab;
import org.muonmc.loader.api.gui.MuonLoaderText;
import org.muonmc.loader.api.gui.MuonWarningLevel;

class MessagesTab extends AbstractTab implements MuonGuiMessagesTab {

	interface MessageTabListener extends TabChangeListener {
		default void onMessageAdded(MuonJsonGuiMessage message) {}
		default void onMessageRemoved(int index, MuonJsonGuiMessage message) {}
	}

	final List<MuonJsonGuiMessage> messages = new ArrayList<>();

	public MessagesTab(BasicWindow<?> parent, MuonLoaderText text) {
		super(parent, text);
	}

	public MessagesTab(QuiltGuiSyncBase parent, LObject obj) throws IOException {
		super(parent, obj);
		for (LoaderValue value : HELPER.expectArray(obj, "messages")) {
			messages.add(readChild(value, MuonJsonGuiMessage.class));
		}
	}

	@Override
	protected void write0(Map<String, LoaderValue> map) {
		super.write0(map);
		map.put("messages", lvf().array(write(messages)));
	}

	@Override
	void handleUpdate(String name, LObject data) throws IOException {
		switch (name) {
			case "add_message": {
				MuonJsonGuiMessage message = readChild(HELPER.expectValue(data, "message"), MuonJsonGuiMessage.class);
				messages.add(message);
				invokeListeners(MessageTabListener.class, l -> l.onMessageAdded(message));
				break;
			}
			case "remove_message": {
				int index = HELPER.expectNumber(data, "index").intValue();
				MuonJsonGuiMessage removed = messages.remove(index);
				if (removed != null) {
					invokeListeners(MessageTabListener.class, l -> l.onMessageRemoved(index, removed));
				}
				break;
			}
			default: {
				super.handleUpdate(name, data);
			}
		}
	}

	@Override
	String syncType() {
		return "tab_messages";
	}

	@Override
	public void addMessage(MuonDisplayedError message) {
		MuonJsonGuiMessage impl = (MuonJsonGuiMessage) message;
		messages.add(impl);
		if (shouldSendUpdates()) {
			Map<String, LoaderValue> map = new HashMap<>();
			map.put("message", writeChild(impl));
			sendUpdate("add_message", lvf().object(map));
		}
	}

	@Override
	public void removeMessage(MuonDisplayedError message) {
		int index = messages.indexOf(message);
		if (index >= 0) {
			Map<String, LoaderValue> map = new HashMap<>();
			map.put("index", lvf().number(index));
			sendUpdate("remove_message", lvf().object(map));
		}
	}

	@Override
	MuonWarningLevel getInheritedLevel() {
		for (MuonJsonGuiMessage msg : messages) {
			// TODO: Add levels to messages!
		}
		return MuonWarningLevel.NONE;
	}
}
