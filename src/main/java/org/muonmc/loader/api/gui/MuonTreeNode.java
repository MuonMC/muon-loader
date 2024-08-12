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

/** A node in a tree.
 * <p>
 * Nodes have the following properties:
 * <ol>
 * <li>An {@link MuonLoaderIcon icon}, displayed at the start of the node.</li>
 * <li>Some {@link MuonLoaderText text}, displayed as the rest of the node.</li>
 * <li>A direct {@link MuonWarningLevel level}, automatically added to the icon. (Although it is added to the left side
 * of the icon, unlike {@link MuonLoaderIcon#withDecoration(MuonLoaderIcon)})</li>
 * </ol>
 */
public interface MuonTreeNode {

	/** @return The current icon for this node. */
	MuonLoaderIcon icon();

	/** Sets the icon for this node.
	 * 
	 * @param icon The new icon.
	 * @return this. */
	MuonTreeNode icon(MuonLoaderIcon icon);

	/** @return The current text for this node. */
	MuonLoaderText text();

	/** Sets the text for this node.
	 * 
	 * @param text The new text.
	 * @return this. */
	MuonTreeNode text(MuonLoaderText text);

	/** @return The current {@link MuonWarningLevel} for this node. The default is {@link MuonWarningLevel#NONE} */
	MuonWarningLevel level();

	/** Sets the level for this node.
	 * 
	 * @param level The new {@link MuonWarningLevel}.
	 * @return this. */
	MuonTreeNode level(MuonWarningLevel level);

	/** @return The maximum warning level of this node and it's children. If this node has no children then this returns
	 *         {@link #level()} */
	MuonWarningLevel maximumLevel();

	/** @return The number of child nodes, and sub-child nodes (called recursively) with a {@link #level()} equal to the
	 *         given level. Also checks this node. */
	int countAtLevel(MuonWarningLevel level);

	/** Controls whether the node is automatically expanded in the GUI. Defaults to
	 * {@link MuonWarningLevel#WARN}. */
	MuonTreeNode autoExpandLevel(MuonWarningLevel level);

	/** Controls the ordering of child nodes. {@link #ADDITION_ORDER} nodes always come first, followed by
	 * {@link #ALPHABETICAL_ORDER} */
	public enum SortOrder {

		/** Sorts nodes by the order in which they were added via {@link MuonTreeNode#addChild(SortOrder)}. */
		ADDITION_ORDER,

		/** Sorts nodes by their {@link MuonTreeNode#sortPrefix()} first, and then by their
		 * {@link MuonTreeNode#text()}. */
		ALPHABETICAL_ORDER,
	}

	/** Adds a new child node to this tree node.
	 * 
	 * @param sortOrder The order to sort the children in.
	 * @return The new child node. */
	MuonTreeNode addChild(SortOrder sortOrder);

	/** Adds a new child node to this tree node. This uses a {@link SortOrder} of {@link SortOrder#ADDITION_ORDER
	 * ADDITION_ORDER}.
	 * 
	 * @return The new child node. */
	default MuonTreeNode addChild() {
		return addChild(SortOrder.ADDITION_ORDER);
	}

	/** Adds a new child node to this tree node. This uses a {@link SortOrder} of {@link SortOrder#ADDITION_ORDER
	 * ADDITION_ORDER}.
	 * 
	 * @param text Convenience parameter, which is passed to {@link #text(MuonLoaderText)} of the returned child node.
	 * @return The new child node. */
	default MuonTreeNode addChild(MuonLoaderText text) {
		return addChild(text, SortOrder.ADDITION_ORDER);
	}

	/** Adds a new child node to this tree node.
	 * 
	 * @param text Convenience parameter, which is passed to {@link #text(MuonLoaderText)} of the returned child node.
	 * @param sortOrder The order to sort the children in.
	 * @return The new child node. */
	default MuonTreeNode addChild(MuonLoaderText text, SortOrder sortOrder) {
		return addChild(sortOrder).text(text);
	}

	/** @return The current sort prefix, which is used when comparing this node with siblings in
	 *         {@link SortOrder#ALPHABETICAL_ORDER}. */
	String sortPrefix();

	/** @param sortPrefix The new sort prefix, which is used when comparing this node with siblings in
	 *            {@link SortOrder#ALPHABETICAL_ORDER}.
	 * @return this */
	MuonTreeNode sortPrefix(String sortPrefix);
}
