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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;
import org.muonmc.loader.api.LoaderValue;
import org.muonmc.loader.api.LoaderValue.LArray;
import org.muonmc.loader.api.LoaderValue.LObject;
import org.muonmc.loader.api.gui.MuonDisplayedError;
import org.muonmc.loader.api.gui.MuonLoaderGui;
import org.muonmc.loader.api.gui.MuonLoaderIcon;
import org.muonmc.loader.api.gui.MuonLoaderText;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public final class MuonJsonGuiMessage extends QuiltGuiSyncBase implements MuonDisplayedError, ButtonContainerImpl {

	interface QuiltMessageListener extends Listener, ButtonContainerListener {
		default void onFixed() {}
		default void onIconChanged() {}
		default void onTitleChanged() {}
		default void onDescriptionChanged() {}
		default void onAdditionalInfoChanged() {}
		default void onTreeNodeChanged() {}
	}

	/** Intentionally not {@link MuonLoaderGui#iconLevelError()} so we can use an identity check. */
	private static final PluginIconImpl DEFAULT_ICON = new PluginIconImpl("level_error");

	boolean fixed = false;

	// Gui fields
	public String title;
	public PluginIconImpl icon = DEFAULT_ICON;
	public final List<String> description = new ArrayList<>();
	public final List<String> additionalInfo = new ArrayList<>();

	public final List<QuiltJsonButton> buttons = new ArrayList<>();

	public String subMessageHeader = "";
	public final List<MuonJsonGuiMessage> subMessages = new ArrayList<>();
	MuonStatusNode treeNode = null;

	// Report fields
	final String reportingPlugin;
	final Throwable reportTrace;
	public int ordering = 0;
	final List<String> reportLines = new ArrayList<>();
	final List<Throwable> exceptions = new ArrayList<>();

	public MuonJsonGuiMessage(QuiltGuiSyncBase parent, String reporter, MuonLoaderText title) {
		super(parent);
		reportTrace = new Throwable();
		this.reportingPlugin = reporter;
		this.title = title.toString();
	}

	public MuonJsonGuiMessage(QuiltGuiSyncBase parent, LoaderValue.LObject obj) throws IOException {
		super(parent, obj);
		this.reportingPlugin = null;
		reportTrace = null;
		title = HELPER.expectString(obj, "title");
		if (obj.containsKey("icon")) {
			icon = readChild(HELPER.expectValue(obj, "icon"), PluginIconImpl.class);
		}
		if (obj.containsKey("tree_node")) {
			treeNode = readChild(HELPER.expectValue(obj, "tree_node"), MuonStatusNode.class);
		}
		subMessageHeader = HELPER.expectString(obj, "sub_message_header");

		for (LoaderValue sub : HELPER.expectArray(obj, "description")) {
			description.add(sub.asString());
		}

		for (LoaderValue sub : HELPER.expectArray(obj, "info")) {
			additionalInfo.add(sub.asString());
		}

		for (LoaderValue sub : HELPER.expectArray(obj, "buttons")) {
			buttons.add(readChild(sub, QuiltJsonButton.class));
		}

		for (LoaderValue sub : HELPER.expectArray(obj, "sub_messages")) {
			subMessages.add(readChild(sub, MuonJsonGuiMessage.class));
		}
	}

	@Override
	String syncType() {
		return "message";
	}

	@Override
	protected void write0(Map<String, LoaderValue> map) {
		map.put("title", lvf().string(title));
		if (icon != null) {
			map.put("icon", writeChild(icon));
		}
		if (treeNode != null) {
			map.put("tree_node", writeChild(treeNode));
		}
		map.put("description", stringArray(description));
		map.put("info", stringArray(additionalInfo));
		map.put("buttons", lvf().array(write(buttons)));
		map.put("sub_message_header", lvf().string(subMessageHeader));
		map.put("sub_messages", lvf().array(write(subMessages)));
	}

	private static LoaderValue stringArray(List<String> list) {
		int i = 0;
		LoaderValue[] values = new LoaderValue[list.size()];
		for (String str : list) {
			values[i++] = lvf().string(str);
		}
		return lvf().array(values);
	}

	@Override
	public MuonDisplayedError appendReportText(String... lines) {
		Collections.addAll(reportLines, lines);
		return this;
	}

	@Override
	public MuonDisplayedError appendDescription(MuonLoaderText... descriptions) {
		int fromIndex = description.size();
		for (MuonLoaderText text : descriptions) {
			Collections.addAll(description, text.toString().split("\\n"));
		}
		if (shouldSendUpdates()) {
			int toIndex = description.size();
			Map<String, LoaderValue> map = new HashMap<>();
			LoaderValue[] array = new LoaderValue[toIndex - fromIndex];
			for (int i = 0; i < array.length; i++) {
				array[i] = lvf().string(description.get(i + fromIndex));
			}
			map.put("add", lvf().array(array));
			sendUpdate("description", lvf().object(map));
		}
		return this;
	}

	@Override
	public MuonDisplayedError clearDescription() {
		description.clear();
		if (shouldSendUpdates()) {
			sendSignal("clear_description");
		}
		return this;
	}

	@Override
	public MuonDisplayedError setOrdering(int priority) {
		this.ordering = priority;
		return this;
	}

	@Override
	public MuonDisplayedError appendAdditionalInformation(MuonLoaderText... information) {
		int fromIndex = additionalInfo.size();
		for (MuonLoaderText text : information) {
			Collections.addAll(additionalInfo, text.toString().split("\\n"));
		}
		if (shouldSendUpdates()) {
			int toIndex = additionalInfo.size();
			Map<String, LoaderValue> map = new HashMap<>();
			LoaderValue[] array = new LoaderValue[toIndex - fromIndex];
			for (int i = 0; i < array.length; i++) {
				array[i] = lvf().string(additionalInfo.get(i + fromIndex));
			}
			map.put("add", lvf().array(array));
			sendUpdate("additional_info", lvf().object(map));
		}
		return this;
	}

	@Override
	public MuonDisplayedError clearAdditionalInformation() {
		additionalInfo.clear();
		if (shouldSendUpdates()) {
			sendSignal("clear_additional_info");
		}
		return this;
	}

	@Override
	public MuonDisplayedError appendThrowable(Throwable t) {
		exceptions.add(t);
		return this;
	}

	@Override
	public MuonDisplayedError title(MuonLoaderText text) {
		this.title = text.toString();
		return this;
	}

	@Override
	public MuonLoaderIcon icon() {
		return icon;
	}

	@Override
	public MuonDisplayedError setIcon(MuonLoaderIcon icon) {
		this.icon = PluginIconImpl.fromApi(icon);
		if (shouldSendUpdates()) {
			if (this.icon == null) {
				sendSignal("clear_icon");
			} else {
				Map<String, LoaderValue> map = new HashMap<>();
				map.put("icon", writeChild(this.icon));
				sendUpdate("set_icon", lvf().object(map));
			}
		}
		return this;
	}

	// Disabled because I'm not sure how to best go about adding this to a gui

//	@Override
//	public QuiltTreeNode treeNode() {
//		if (treeNode == null) {
//			treeNode(new QuiltStatusNode(this));
//		}
//		return treeNode;
//	}
//
//	@Override
//	public QuiltDisplayedError treeNode(QuiltTreeNode node) {
//		this.treeNode = (QuiltStatusNode) node;
//		if (shouldSendUpdates()) {
//			if (node == null) {
//				sendSignal("remove_tree_node");
//			} else {
//				Map<String, LoaderValue> map = new HashMap<>();
//				map.put("tree_node", writeChild(this.treeNode));
//				sendUpdate("set_tree_node", lvf().object(map));
//			}
//		}
//		return this;
//	}

	@Override
	public QuiltJsonButton addButton(QuiltJsonButton button) {
		buttons.add(button);
		if (shouldSendUpdates()) {
			Map<String, LoaderValue> map = new HashMap<>();
			map.put("button", writeChild(button));
			sendUpdate("add_button", lvf().object(map));
		}
		return button;
	}

	@Override
	public MuonJsonGuiMessage getThis() {
		return this;
	}

	@Override
	public QuiltErrorButton addFileViewButton(MuonLoaderText name, Path openedPath) {
		return ButtonContainerImpl.super.addFileViewButton(name, openedPath);
	}

	@Override
	public QuiltErrorButton addFileEditButton(MuonLoaderText name, Path openedPath) {
		return ButtonContainerImpl.super.addFileEditButton(name, openedPath);
	}

	@Override
	public QuiltErrorButton addFolderViewButton(MuonLoaderText name, Path openedFolder) {
		return ButtonContainerImpl.super.addFolderViewButton(name, openedFolder);
	}

	@Override
	public QuiltErrorButton addOpenLinkButton(MuonLoaderText name, String url) {
		return ButtonContainerImpl.super.addOpenLinkButton(name, url);
	}

	@Override
	public QuiltErrorButton addOpenQuiltSupportButton() {
		return ButtonContainerImpl.super.addOpenQuiltSupportButton();
	}

	@Override
	public QuiltErrorButton addCopyTextToClipboardButton(MuonLoaderText name, String fullText) {
		return ButtonContainerImpl.super.addCopyTextToClipboardButton(name, fullText);
	}

	@Override
	public QuiltErrorButton addCopyFileToClipboardButton(MuonLoaderText name, Path openedFile) {
		return ButtonContainerImpl.super.addCopyFileToClipboardButton(name, openedFile);
	}

	@Override
	public QuiltErrorButton addOnceActionButton(MuonLoaderText name, MuonLoaderText disabledText, Runnable action) {
		return ButtonContainerImpl.super.addOnceActionButton(name, disabledText, action);
	}

	@Override
	public QuiltErrorButton addActionButton(MuonLoaderText name, Runnable action) {
		return ButtonContainerImpl.super.addActionButton(name, action);
	}

	@Override
	public void setFixed() {
		fixed = true;
		if (/* Intentional identity check */ icon == DEFAULT_ICON) {
			setIcon(MuonLoaderGui.iconTick());
		}
		if (shouldSendUpdates()) {
			sendSignal("fixed");
		}
		invokeListeners(QuiltMessageListener.class, QuiltMessageListener::onFixed);
	}

	@Override
	public boolean isFixed() {
		return fixed;
	}

	@Override
	public void addOnFixedListener(Runnable action) {
		listeners.add(new QuiltMessageListener() {
			@Override
			public void onFixed() {
				action.run();
			}
		});
	}

	@Override
	void handleUpdate(String name, LObject data) throws IOException {
		switch (name) {
			case "fixed": {
				this.fixed = true;
				invokeListeners(QuiltMessageListener.class, QuiltMessageListener::onFixed);
				return;
			}
			case "clear_description": {
				description.clear();
				invokeListeners(QuiltMessageListener.class, QuiltMessageListener::onDescriptionChanged);
				return;
			}
			case "clear_additional_info": {
				additionalInfo.clear();
				invokeListeners(QuiltMessageListener.class, QuiltMessageListener::onAdditionalInfoChanged);
				return;
			}
			case "description":
			case "additional_info": {
				boolean isDescription = name.startsWith("d");

				LArray lines = HELPER.expectArray(data, "add");
				for (int i = 0; i < lines.size(); i++) {
					String line = HELPER.expectString(lines.get(i));
					(isDescription ? description : additionalInfo).add(line);
				}

				if (isDescription) {
					invokeListeners(QuiltMessageListener.class, QuiltMessageListener::onDescriptionChanged);
				} else {
					invokeListeners(QuiltMessageListener.class, QuiltMessageListener::onAdditionalInfoChanged);
				}
				return;
			}
			case "clear_icon": {
				icon = null;
				invokeListeners(QuiltMessageListener.class, QuiltMessageListener::onIconChanged);
				return;
			}
			case "set_icon": {
				icon = readChild(HELPER.expectValue(data, "icon"), PluginIconImpl.class);
				invokeListeners(QuiltMessageListener.class, QuiltMessageListener::onIconChanged);
				return;
			}
			case "set_tree_node": {
				treeNode = readChild(HELPER.expectValue(data, "tree_node"), MuonStatusNode.class);
				invokeListeners(QuiltMessageListener.class, QuiltMessageListener::onTreeNodeChanged);
				return;
			}
			case "remove_tree_node": {
				treeNode = null;
				invokeListeners(QuiltMessageListener.class, QuiltMessageListener::onTreeNodeChanged);
				return;
			}
			case "add_button": {
				LoaderValue value = HELPER.expectValue(data, "button");
				QuiltJsonButton button = readChild(value, QuiltJsonButton.class);
				buttons.add(button);
				invokeListeners(ButtonContainerListener.class, l -> l.onButtonAdded(button));
				break;
			}
			default: {
				throw new IOException("Unhandled update '" + name + "'");
			}
		}
	}

	public List<String> toReportText() {
		List<String> lines = new ArrayList<>();
		lines.addAll(reportLines);

		if (lines.isEmpty()) {
			lines.add("The plugin that created this error (" + reportingPlugin + ") forgot to call 'appendReportText'!");
			lines.add("The next stacktrace is where the plugin created the error, not the actual error.'");
			exceptions.add(0, reportTrace);
		}

		for (Throwable ex : exceptions) {
			lines.add("");
			StringWriter writer = new StringWriter();
			ex.printStackTrace(new PrintWriter(writer));
			Collections.addAll(lines, writer.toString().split("\n"));
		}
		return lines;
	}
}
