/*
 * Copyright 2022, 2023, 2024 QuiltMC
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

package org.muonmc.loader.api.plugin;

import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.jetbrains.annotations.ApiStatus;
import org.muonmc.loader.api.plugin.gui.PluginGuiTreeNode;
import org.muonmc.loader.api.plugin.solver.*;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;
import org.muonmc.loader.api.gui.MuonDisplayedError;
import org.muonmc.loader.api.gui.MuonLoaderText;
import org.muonmc.loader.api.gui.MuonTreeNode;
import org.muonmc.loader.api.plugin.solver.LoadOption;
import org.muonmc.loader.api.plugin.solver.ModLoadOption;
import org.muonmc.loader.api.plugin.solver.Rule;
import org.muonmc.loader.api.plugin.solver.RuleContext;
import org.muonmc.loader.api.plugin.solver.TentativeLoadOption;

/** Passed to loader plugins as the singular way to access the rest of quilt. */
@ApiStatus.NonExtendable
@MuonLoaderInternal(MuonLoaderInternalType.PLUGIN_API)
public interface MuonPluginContext {

	// ###########
	// # Context #
	// ###########

	/** @return The global plugin manager, which is independent of specific contexts. */
	MuonPluginManager manager();

	/** @return The plugin that this context is for. */
	MuonLoaderPlugin plugin();

	/** @return The modID of this plugin. */
	String pluginId();

	/** @return The {@link Path} that the plugin is loaded from. Use this to lookup resources rather than
	 *         {@link Class#getResource(String)}. */
	Path pluginPath();

	// ##############
	// # Operations #
	// ##############

	/** Adds an additional file to scan for mods, which will go through the same steps as files found in mod folders.
	 * (This is more flexible than loading files manually, since it allows fabric mods to be jar-in-jar'd in quilt mods,
	 * or vice versa. Or any mod type of which a loader plugin can load).
	 * 
	 * @param guiNode The GUI node to display the loaded mod details under
	 * @param direct True if the file is directly loaded rather than being included in another mod (see
	 *            {@link ModLocation#isDirect()})
	 * @deprecated {@link PluginGuiTreeNode} has moved to public API: {@link MuonTreeNode}. As such please call
	 *             {@link #addFileToScan(Path, MuonTreeNode, boolean)} instead. */
	@Deprecated
	void addFileToScan(Path file, PluginGuiTreeNode guiNode, boolean direct);

	/** Adds an additional file to scan for mods, which will go through the same steps as files found in mod folders.
	 * (This is more flexible than loading files manually, since it allows fabric mods to be jar-in-jar'd in quilt mods,
	 * or vice versa. Or any mod type of which a loader plugin can load).
	 * 
	 * @param guiNode The GUI node to display the loaded mod details under
	 * @param direct True if the file is directly loaded rather than being included in another mod (see {@link ModLocation#isDirect()}) */
	void addFileToScan(Path file, MuonTreeNode guiNode, boolean direct);

	/** Adds an additional folder to scan for mods, which will be treated in the same way as the regular mods folder.
	 *
	 * @return true if the given folder is a new folder, or false if it has already been added and scanned before. */
	boolean addFolderToScan(Path folder);

	/** "Locks" a zip file that has been opened by {@link MuonPluginManager#loadZip(Path)} so that it won't be unloaded
	 * if no loaded mod is using it.
	 * 
	 * @param path A path that has been returned by {@link MuonPluginManager#loadZip(Path)}, <em>not</em> one of it's
	 *            subfolders, or the zip file passed to that method. */
	void lockZip(Path path);

	/** Reports an error, which will be shown in the error gui screen and saved in the crash report file. */
	MuonDisplayedError reportError(MuonLoaderText title);

	/** Stops loading as soon as possible. This normally means it will throw an internal exception. This should be used
	 * when you've reported an error via {@link #reportError(MuonLoaderText)} and don't want to add an extra throwable
	 * stacktrace to the crash report. */
	void haltLoading();

	// ##############
	// # Scheduling #
	// ##############

	/** Submits a task to be completed after plugin resolution, but before the current cycle ends. The task may be
	 * executed on a different thread, depending on loaders config options.
	 * <p>
	 * This should only be called by {@link MuonLoaderPlugin#resolve(TentativeLoadOption)},
	 * {@link MuonLoaderPlugin#finish(ModSolveResult)}, or by any tasks that are
	 * passed to this function during their execution.
	 * 
	 * @return A {@link MuonPluginTask} which will contain the result of the task, or the failure state if something
	 *         went wrong. */
	<V> MuonPluginTask<V> submit(Callable<V> task);

	/** Submits a task to be completed after plugin resolution, and additionally after the given tasks have completed,
	 * but before the current cycle ends. The task may be executed on a different thread, depending on loaders config
	 * options. Note that the task will still be executed, <em>even if the dependencies failed.</em> This is to allow
	 * the task to handle errors directly.
	 * 
	 * @param deps The tasks that must complete before the given task can be executed.
	 * @return A {@link MuonPluginTask} which will contain the result of the task, or the failure state if something
	 *         went wrong. */
	<V> MuonPluginTask<V> submitAfter(Callable<V> task, MuonPluginTask<?>... deps);

	// #######
	// # Gui #
	// #######

	/** Used to ask the real user of something. Normally this will append something to the existing gui rather than
	 * opening a new gui each time this is called.
	 * <p>
	 * TODO: Create all gui stuff! for now this just throws an {@link AbstractMethodError} */
	default <V> MuonPluginTask<V> addGuiRequest() {
		throw new AbstractMethodError("// TODO: Add gui support!");
	}

	// ###########
	// # Solving #
	// ###########

	/** Retrieves a context for directly adding {@link LoadOption}s and {@link Rule}s. Note that you shouldn't use this
	 * to add mods. */
	RuleContext ruleContext();

	/** Adds a {@link ModLoadOption} to the {@link RuleContext}, using the specified gui node for all it's location
	 * information.
	 * <p>
	 * This is preferable to calling {@link RuleContext#addOption(LoadOption)} since that adds a "floating" parent node
	 * associated with the plugin itself, not where it might have been loaded from. 
	 * @param fileNode The {@link PluginGuiTreeNode} which is shown in the 'Files' tab of the error window.
	 * @deprecated {@link PluginGuiTreeNode} has moved to public API: {@link MuonTreeNode}. As such please call
	 *             {@link #addModLoadOption(ModLoadOption, MuonTreeNode)} instead. */
	@Deprecated
	void addModLoadOption(ModLoadOption mod, PluginGuiTreeNode fileNode);

	/** Adds a {@link ModLoadOption} to the {@link RuleContext}, using the specified gui node for all it's location
	 * information.
	 * <p>
	 * This is preferable to calling {@link RuleContext#addOption(LoadOption)} since that adds a "floating" parent node
	 * associated with the plugin itself, not where it might have been loaded from. 
	 * @param fileNode The {@link PluginGuiTreeNode} which is shown in the 'Files' tab of the error window.*/
	void addModLoadOption(ModLoadOption mod, MuonTreeNode fileNode);

	/** Adds a tentative option which can be resolved later by
	 * {@link MuonLoaderPlugin#resolve(TentativeLoadOption)}, if it is selected.
	 * 
	 * @param option */
	<T extends LoadOption & TentativeLoadOption> void addTentativeOption(T option);

	/** Only callable during {@link MuonLoaderPlugin#handleError(java.util.Collection)} to identify the given rule as one
	 * which can be removed for the purposes of error message generation. */
	void blameRule(Rule rule);
}
