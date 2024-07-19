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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;
import org.muonmc.loader.api.LoaderValue;
import org.muonmc.loader.api.LoaderValue.LObject;
import org.muonmc.loader.api.gui.MuonLoaderGui;
import org.muonmc.loader.api.gui.MuonLoaderIcon;
import org.muonmc.loader.api.gui.MuonLoaderIcon.SubIconPosition;
import org.muonmc.loader.api.gui.MuonLoaderText;
import org.muonmc.loader.api.gui.MuonTreeNode;
import org.muonmc.loader.api.gui.MuonWarningLevel;
import org.muonmc.loader.api.plugin.gui.PluginGuiManager;
import org.muonmc.loader.api.plugin.gui.PluginGuiTreeNode;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public final class MuonStatusNode extends QuiltGuiSyncBase implements MuonTreeNode, PluginGuiTreeNode {

	interface TreeNodeListener extends Listener {
		default void onTextChanged() {}

		default void onIconChanged() {}

		default void onLevelChanged() {}

		default void onMaxLevelChanged() {}

		default void onChildAdded(MuonStatusNode child) {}
	}

	private MuonLoaderText apiText = MuonLoaderText.EMPTY;
	String text = "";

	PluginIconImpl icon = new PluginIconImpl();

	MuonWarningLevel level = MuonWarningLevel.NONE;
	MuonWarningLevel maxLevel = MuonWarningLevel.NONE;
	MuonWarningLevel autoExpandLevel = MuonWarningLevel.WARN;

	private String sortPrefix = "";

	final List<MuonStatusNode> childNodesByAddition = new ArrayList<>();
	final List<MuonStatusNode> childNodesByAlphabetical = new ArrayList<>();

	/** Extra text for more information. Lines should be separated by "\n". */
	public String details;

	MuonStatusNode(QuiltGuiSyncBase parent) {
		super(parent);
	}

	MuonStatusNode(QuiltGuiSyncBase parent, LoaderValue.LObject obj) throws IOException {
		super(parent, obj);
		text = HELPER.expectString(obj, "name");
		icon = readChild(HELPER.expectValue(obj, "icon"), PluginIconImpl.class);
		level = HELPER.expectEnum(MuonWarningLevel.class, obj, "level");
		maxLevel = HELPER.expectEnum(MuonWarningLevel.class, obj, "maxLevel");
		autoExpandLevel = HELPER.expectEnum(MuonWarningLevel.class, obj, "autoExpandLevel");
		details = obj.containsKey("details") ? HELPER.expectString(obj, "details") : null;
		for (LoaderValue sub : HELPER.expectArray(obj, "children_by_addition")) {
			childNodesByAddition.add(readChild(sub, MuonStatusNode.class));
		}
		for (LoaderValue sub : HELPER.expectArray(obj, "children_by_alphabetical")) {
			childNodesByAlphabetical.add(readChild(sub, MuonStatusNode.class));
		}
	}

	@Override
	protected void write0(Map<String, LoaderValue> map) {
		map.put("name", lvf().string(text));
		map.put("icon", writeChild(icon));
		map.put("level", lvf().string(level.name()));
		map.put("maxLevel", lvf().string(maxLevel.name()));
		map.put("autoExpandLevel", lvf().string(autoExpandLevel.name()));
		if (details != null) {
			map.put("details", lvf().string(details));
		}
		map.put("children_by_addition", lvf().array(write(childNodesByAddition)));
		map.put("children_by_alphabetical", lvf().array(write(childNodesByAlphabetical)));
	}

	@Override
	String syncType() {
		return "tree_node";
	}

	@Override
	void handleUpdate(String name, LObject data) throws IOException {
		switch (name) {
			case "set_icon": {
				this.icon = readChild(HELPER.expectValue(data, "icon"), PluginIconImpl.class);
				invokeListeners(TreeNodeListener.class, TreeNodeListener::onIconChanged);
				break;
			}
			case "set_text": {
				this.text = HELPER.expectString(data, "text");
				invokeListeners(TreeNodeListener.class, TreeNodeListener::onTextChanged);
				break;
			}
			case "set_level": {
				this.level = HELPER.expectEnum(MuonWarningLevel.class, data, "level");
				invokeListeners(TreeNodeListener.class, TreeNodeListener::onLevelChanged);
				break;
			}
			case "set_max_level": {
				this.maxLevel = HELPER.expectEnum(MuonWarningLevel.class, data, "max_level");
				invokeListeners(TreeNodeListener.class, TreeNodeListener::onMaxLevelChanged);
				break;
			}
			default: {
				super.handleUpdate(name, data);
			}
		}
	}

	@Override
	public MuonStatusNode parent() {
		return parent instanceof MuonStatusNode ? (MuonStatusNode) parent : null;
	}

	@Override
	public MuonLoaderIcon icon() {
		return icon;
	}

	@Override
	public MuonTreeNode icon(MuonLoaderIcon icon) {
		this.icon = PluginIconImpl.fromApi(icon);
		if (this.icon == null) {
			this.icon = new PluginIconImpl();
		}
		invokeListeners(TreeNodeListener.class, TreeNodeListener::onIconChanged);
		if (shouldSendUpdates()) {
			Map<String, LoaderValue> map = new HashMap<>();
			map.put("icon", writeChild(this.icon));
			sendUpdate("set_icon", lvf().object(map));
		}
		return this;
	}

	@Override
	public MuonLoaderText text() {
		return apiText;
	}

	@Override
	public MuonStatusNode text(MuonLoaderText text) {
		apiText = Objects.requireNonNull(text);
		this.text = text.toString();
		MuonStatusNode p = parent();
		if (p != null) {
			p.sortChildren();
		}
		invokeListeners(TreeNodeListener.class, TreeNodeListener::onTextChanged);
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
	public MuonTreeNode level(MuonWarningLevel level) {
		this.level = Objects.requireNonNull(level);
		invokeListeners(TreeNodeListener.class, TreeNodeListener::onLevelChanged);
		if (shouldSendUpdates()) {
			Map<String, LoaderValue> map = new HashMap<>();
			map.put("level", lvf().string(this.level.name()));
			sendUpdate("set_level", lvf().object(map));
		}
		recomputeMaxLevel();
		return this;
	}

	private void recomputeMaxLevel() {
		MuonWarningLevel oldMaxLevel = maxLevel;
		maxLevel = level;
		for (MuonStatusNode child : childIterable()) {
			maxLevel = MuonWarningLevel.getHighest(maxLevel, child.maxLevel);
		}
		if (maxLevel != oldMaxLevel) {
			if (shouldSendUpdates()) {
				Map<String, LoaderValue> map = new HashMap<>();
				map.put("max_level", lvf().string(this.maxLevel.name()));
				sendUpdate("set_max_level", lvf().object(map));
			}
			if (parent instanceof MuonStatusNode) {
				((MuonStatusNode) parent).recomputeMaxLevel();
			}
		}
	}

	@Override
	public MuonWarningLevel maximumLevel() {
		return maxLevel;
	}

	@Override
	public int countAtLevel(MuonWarningLevel level) {
		int count = this.level == level ? 1 : 0;
		for (MuonStatusNode node : childIterable()) {
			count += node.countAtLevel(level);
		}
		return count;
	}

	@Override
	public MuonTreeNode autoExpandLevel(MuonWarningLevel level) {
		autoExpandLevel = level;
		return this;
	}

	public boolean getExpandByDefault() {
		return autoExpandLevel.ordinal() >= maxLevel.ordinal();
	}

	public void setExpandByDefault(boolean expandByDefault) {
		if (expandByDefault) {
			autoExpandLevel(MuonWarningLevel.values()[MuonWarningLevel.values().length - 1]);
		}
	}

	@Deprecated
	public void setError() {
		level(MuonWarningLevel.ERROR);
	}

	@Deprecated
	public void setWarning() {
		level(MuonWarningLevel.WARN);
	}

	@Deprecated
	public void setInfo() {
		level(MuonWarningLevel.INFO);
	}

	@Override
	public MuonStatusNode addChild(MuonTreeNode.SortOrder sortOrder) {
		MuonStatusNode child = new MuonStatusNode(this);
		if (sortOrder == MuonTreeNode.SortOrder.ADDITION_ORDER) {
			childNodesByAddition.add(child);
		} else {
			childNodesByAlphabetical.add(child);
		}
		invokeListeners(TreeNodeListener.class, l -> l.onChildAdded(child));
		if (shouldSendUpdates()) {
			Map<String, LoaderValue> map = new HashMap<>();
			map.put("sort_order", lvf().string(sortOrder.name()));
			map.put("child", writeChild(child));
			sendUpdate("add_child", lvf().object(map));
		}
		return child;
	}

	@Override
	public MuonStatusNode addChild() {
		return addChild(MuonTreeNode.SortOrder.ADDITION_ORDER);
	}

	@Override
	public MuonStatusNode addChild(MuonLoaderText text) {
		MuonStatusNode child = addChild();
		child.text(text);
		return child;
	}

	@Override
	public MuonStatusNode addChild(MuonLoaderText text, MuonTreeNode.SortOrder sortOrder) {
		return addChild(sortOrder).text(text);
	}

	void forEachChild(Consumer<? super MuonStatusNode> consumer) {
		childNodesByAddition.forEach(consumer);
		childNodesByAlphabetical.forEach(consumer);
	}

	Iterable<MuonStatusNode> childIterable() {
		return () -> new Iterator<MuonStatusNode>() {
			final Iterator<MuonStatusNode> first = childNodesByAddition.iterator();
			final Iterator<MuonStatusNode> second = childNodesByAlphabetical.iterator();

			@Override
			public boolean hasNext() {
				return first.hasNext() || second.hasNext();
			}

			@Override
			public MuonStatusNode next() {
				if (first.hasNext()) {
					return first.next();
				} else {
					return second.next();
				}
			}
		};
	}

	@Override
	public String sortPrefix() {
		return sortPrefix;
	}

	@Override
	public MuonStatusNode sortPrefix(String sortPrefix) {
		if (sortPrefix == null) {
			sortPrefix = "";
		}
		if (this.sortPrefix.equals(sortPrefix)) {
			return this;
		}
		this.sortPrefix = sortPrefix;
		MuonStatusNode p = parent();
		if (p != null) {
			p.sortChildren();
		}
		if (shouldSendUpdates()) {
			Map<String, LoaderValue> map = new HashMap<>();
			map.put("sort_prefix", lvf().string(sortPrefix));
			sendUpdate("set_sort_prefix", lvf().object(map));
		}
		return this;
	}

	private void sortChildren() {
		childNodesByAlphabetical.sort((a, b) -> {
			int cmp = a.sortPrefix.compareTo(b.sortPrefix);
			if (cmp != 0) {
				return cmp;
			}
			return a.text.compareTo(b.text);
		});
	}

	// Deprecated PluginGuiTreeNode methods

	@Override
	@Deprecated
	public MuonTreeNode getNew() {
		return this;
	}

	@Override
	@Deprecated
	public PluginGuiTreeNode addChild(PluginGuiTreeNode.SortOrder sortOrder) {
		return addChild(MuonLoaderText.EMPTY, sortOrder);
	}

	@Override
	@Deprecated
	public PluginGuiTreeNode addChild(MuonLoaderText text, PluginGuiTreeNode.SortOrder sortOrder) {
		MuonTreeNode.SortOrder newOrder = sortOrder == PluginGuiTreeNode.SortOrder.ADDITION_ORDER
			? MuonTreeNode.SortOrder.ADDITION_ORDER
			: MuonTreeNode.SortOrder.ALPHABETICAL_ORDER;
		return addChild(newOrder);
	}

	@Override
	@Deprecated
	public PluginGuiTreeNode setDirectLevel(WarningLevel level) {
		level(fromOldLevel(level));
		return this;
	}

	@Override
	@Deprecated
	public PluginGuiTreeNode setException(Throwable exception) {
		return this;
	}

	@Deprecated
	private static MuonWarningLevel fromOldLevel(WarningLevel level) {
		switch (level) {
			case CONCERN:
				return MuonWarningLevel.CONCERN;
			case DEBUG_ONLY:
				return MuonWarningLevel.DEBUG_ONLY;
			case ERROR:
				return MuonWarningLevel.ERROR;
			case FATAL:
				return MuonWarningLevel.FATAL;
			case INFO:
				return MuonWarningLevel.INFO;
			case NONE:
				return MuonWarningLevel.NONE;
			case WARN:
				return MuonWarningLevel.WARN;
			default:
				throw new IllegalStateException("Unknown WarningLevel " + level);
		}
	}

	@Deprecated
	private WarningLevel toOldLevel(MuonWarningLevel level) {
		switch (level) {
			case CONCERN:
				return WarningLevel.CONCERN;
			case DEBUG_ONLY:
				return WarningLevel.DEBUG_ONLY;
			case ERROR:
				return WarningLevel.ERROR;
			case FATAL:
				return WarningLevel.FATAL;
			case INFO:
				return WarningLevel.INFO;
			case NONE:
				return WarningLevel.NONE;
			case WARN:
				return WarningLevel.WARN;
			default:
				throw new IllegalStateException("Unknown QuiltWarningLevel " + level);
		}
	}

	@Override
	@Deprecated
	public WarningLevel getDirectLevel() {
		return toOldLevel(level());
	}

	@Override
	@Deprecated
	public WarningLevel getMaximumLevel() {
		return toOldLevel(maximumLevel());
	}

	@Override
	@Deprecated
	public int countOf(WarningLevel level) {
		return countAtLevel(fromOldLevel(level));
	}

	@Override
	@Deprecated
	public MuonLoaderIcon mainIcon() {
		MuonLoaderIcon i = icon();
		if (i == null) {
			return MuonLoaderGui.iconTreeDot();
		}
		for (SubIconPosition pos : SubIconPosition.values()) {
			if (i.getDecoration(pos) != null) {
				i = i.withDecoration(pos, null);
			}
		}
		return i;
	}

	@Override
	@Deprecated
	public PluginGuiTreeNode mainIcon(MuonLoaderIcon icon) {
		for (SubIconPosition pos : SubIconPosition.values()) {
			icon = icon.withDecoration(pos, icon().getDecoration(pos));
		}
		icon(icon);
		return this;
	}

	@Override
	@Deprecated
	public @Nullable MuonLoaderIcon subIcon() {
		return icon.getDecoration(SubIconPosition.BOTTOM_RIGHT);
	}

	@Override
	@Deprecated
	public PluginGuiTreeNode subIcon(MuonLoaderIcon subIcon) {
		icon(icon().withDecoration(SubIconPosition.BOTTOM_RIGHT, subIcon));
		return this;
	}

	@Override
	@Deprecated
	public void expandByDefault(boolean autoCollapse) {
		autoExpandLevel(autoCollapse ? MuonWarningLevel.NONE : MuonWarningLevel.FATAL);
	}

	@Override
	@Deprecated
	public PluginGuiManager manager() {
		return GuiManagerImpl.MANAGER;
	}
}
