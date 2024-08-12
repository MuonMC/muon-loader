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

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.muonmc.loader.api.plugin.gui.PluginGuiTreeNode;
import org.muonmc.loader.api.plugin.solver.LoadOption;
import org.muonmc.loader.api.plugin.solver.ModLoadOption;
import org.muonmc.loader.api.plugin.solver.ModSolveResult;
import org.muonmc.loader.api.plugin.solver.Rule;
import org.muonmc.loader.api.plugin.solver.TentativeLoadOption;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;
import org.muonmc.loader.api.LoaderValue;
import org.muonmc.loader.api.MuonLoader;

/** The base type for all plugins.
 * <p>
 * Plugins are applied the following steps:
 * <ol>
 * <li>{@link #load(MuonPluginContext, Map)} is called to set the {@link MuonPluginContext}.</li></li>
 * <li>Quilt Loader will scan all files in those folders, and follow these steps:
 * <ol>
 * <li>If it ends with ".disabled", or is a system or hidden file, then it is skipped.</li>
 * <li>If it is a zip or jar file (or can be opened by {@link FileSystems#newFileSystem(Path, ClassLoader)}) then it
 * will be opened, and checked for a "muon.mod.json" file. If one is found, then it is loaded as a quilt mod (and
 * possibly as a new plugin - which will be loaded instantly, rather than waiting until the next cycle).</li>
 * <li>If "muon.mod.json" couldn't be found then the zip root will be passed to
 * {@link #scanZip(Path, ModLocation, PluginGuiTreeNode)}</li>
 * <li>Otherwise it will be passed to {@link #scanUnknownFile(Path, ModLocation, PluginGuiTreeNode)}</li>
 * </ol>
 * </li>
 * <li>{@link #beforeSolve()} is called.</li>
 * <li>Loader will begin solving the rules added</li>
 * </ol>
 * In particular, plugins must never call {@link MuonLoader} directly - that's designed solely for mods to use after
 * mod loading is complete. */
@MuonLoaderInternal(MuonLoaderInternalType.PLUGIN_API)
public interface MuonLoaderPlugin {

	/** Called at the very start to pass the {@link MuonPluginContext} that this plugin should use for every call into
	 * quilt.
	 * <p>
	 * Plugins aren't expected to do anything at that modifies quilt at this stage, but they could load configuration
	 * data.
	 * 
	 * @param context The context, to use later. You should store this in a field if you need it.
	 * @param previousData The data written to {@link #unload(Map)} by a different version of this plugin, or an empty
	 *            map if the plugin hasn't been reloaded. */
	void load(MuonPluginContext context, Map<String, LoaderValue> previousData);

	/** Prepares to unload this plugin, in preparation for loading a different version of the same plugin. If you wish
	 * to keep some data from previous runs into the next run, you should put them into the given map. */
	void unload(Map<String, LoaderValue> data);

	/** Called once per mod folder that is added - either directly by quilt, or by any plugin calling
	 * {@link MuonPluginContext#addFolderToScan(Path)} */
	default void onModFolderAdded(Path folder) {}

	/** Called once per archival file found in any of the folders added by {@link #addModFolders(Set)} or
	 * {@link #onModFolderAdded(Path)}. This is only called for zips that aren't identified as quilt mods, and aren't
	 * system files.
	 * <p>
	 * You can retrieve the file name of the original zip by using {@link MuonPluginManager#getParent(Path)}.
	 * <p>
	 * Note that this will be called for <em>all</em> plugins, even if previous plugins loaded the zip as a mod!
	 * 
	 * @param root The root of the zip file.
	 * @param fromClasspath TODO
	 * @param guiNode TODO
	 * @return One or many {@link ModLoadOption}s if this plugin could load the given zip as a mod, or either null or an
	 *         empty array if it couldn't.
	 * @throws IOException if something went wrong while reading the zip and so an error message should be displayed. */
	default ModLoadOption[] scanZip(Path root, ModLocation location, PluginGuiTreeNode guiNode) throws IOException {
		return null;
	}

	/** Called once per file encountered which loader can't open (I.E. those which are not passed to
	 * {@link #scanZip(Path, boolean, PluginGuiTreeNode)}). However system files are not passed here.
	 * 
	 * @param file
	 * @param fromClasspath TODO
	 * @param guiNode TODO
	 * @return One or many {@link ModLoadOption}s if this plugin could load the given zip as a mod, or either null or an
	 *         empty array if it couldn't.
	 * @throws IOException if something went wrong while reading the zip and so an error message should be displayed. */
	default ModLoadOption[] scanUnknownFile(Path file, ModLocation location, PluginGuiTreeNode guiNode)
		throws IOException {
		return null;
	}

	/** Called once per folder group added as a mod. This is called for both classpath groups, and folders added with
	 * -Dloader.addMods=folder
	 * 
	 * @return One or many {@link ModLoadOption}s if this plugin could load the given zip as a mod, or either null or an
	 *         empty array if it couldn't.
	 * @throws IOException if something went wrong while reading a file in the folder and so an error message should be
	 *             displayed. */
	default ModLoadOption[] scanFolder(Path folder, ModLocation location, PluginGuiTreeNode guiNode)
		throws IOException {
		return null;
	}

	/** Called if multiple plugins return mod options for the same file. This is never invoked for quilt mods. Only one
	 * plugin needs to return a non-null value - however if both plugins return null, or both plugins return the same
	 * value, then the game will crash since quilt loader doesn't know which to choose. <em>(In the future quilt loader
	 * might add system properties to handle these cases, however for now plugin authors are intended to discuss this to
	 * agree on a common ordering).</em>
	 * 
	 * @param path The path that the mods were loaded from. This is either the
	 *            {@link MuonPluginManager#getParent(Path)} of the path passed to
	 *            {@link #scanZip(Path, ModLocation, PluginGuiTreeNode)}, or the exact path passed to either
	 *            {@link #scanUnknownFile(Path, ModLocation, PluginGuiTreeNode)} or
	 *            {@link #scanFolder(Path, ModLocation, PluginGuiTreeNode)}
	 * @param thisOptions An unmodifiable list of the options this plugin loaded from the path.
	 * @param otherPluginId The ID of the other plugin. This is never quilt loader itself
	 *            ({@link MuonPluginManager#MUON_LOADER}), since files recognised as quilt mods are never passed to
	 *            plugins. All plugins are expected to handle {@link MuonPluginManager#QUILTED_FABRIC_LOADER} (if they
	 *            load from zips), since it doesn't handle any other plugin.
	 * @param otherOptions An unmodifiable list of the options that the other plugin loaded from the path. You are
	 *            intended to check {@link ModLoadOption#subType}, and nothing else.
	 * @return True if this plugin has a higher priority than the other plugin, or false if this plugin has a lower
	 *         priority. If this plugin doesn't know then it should return null. */
	default @Nullable Boolean isHigherPriorityThan(Path path, List<ModLoadOption> thisOptions, String otherPluginId, List<ModLoadOption> otherOptions) {
		return null;
	}

	/** Called once per cycle just before the set of {@link Rule}s and {@link LoadOption}s are solved. */
	default void beforeSolve() {}

	/** Called after solving has finished and successfully found the final set of {@link LoadOption}s and mods. None of
	 * the "present" {@link LoadOption}s will be {@link TentativeLoadOption}. This will only be called once, and marks
	 * the end of the final cycle.
	 * <p>
	 * Like resolving, you can submit tasks and queue gui requests during this, which will be completed before the game
	 * can actually be launched. */
	default void finish(ModSolveResult result) {}

	// #######
	// Solving
	// #######

	/** Resolves a single {@link TentativeLoadOption} that was added via
	 * {@link MuonPluginContext#addTentativeOption(LoadOption)}. This is only called if the option was selected by the
	 * solver - unselected options are not resolved.
	 * <p>
	 * Long-running operations should use {@link MuonPluginContext#submit(java.util.concurrent.Callable)} to perform
	 * those tasks in the future, and possibly on different threads. Operations that require use of the gui should use
	 * {@link MuonPluginContext#addGuiRequest()} instead, and call submit after that has been accepted or denied.
	 * 
	 * @return A {@link MuonPluginTask} containing (or will contain) the {@link LoadOption} that will replace the
	 *         {@link TentativeLoadOption} next cycle. */
	default MuonPluginTask<? extends LoadOption> resolve(TentativeLoadOption from) {
		throw new IllegalStateException(
			getClass() + " has added a TentativeLoadOption (" + from.getClass() + ") but can't resolve it!"
		);
	}

	/** @return True if this plugin did something which will solve / change the error in future, and so loader won't ask
	 *         any other plugins to solve this. You are expected to call {@link MuonPluginContext#blameRule(Rule)} if
	 *         you can't actually fix the issue, but can identify a rule to be removed.
	 *         <p>
	 *         If no plugin can identify a rule to be removed then loader will remove a random rule in order to move on
	 *         to the next error. If this returns true then no rules will be removed, and instead loader will assume
	 *         that the error has been handled in some other way. (and it will promptly crash if you haven't) */
	default boolean handleError(Collection<Rule> ruleChain) {
		return false;
	}

	/** Called whenever a new LoadOption is added, for plugins to add Rules based on this. (For example the default
	 * plugin creates rules based on the dependencies and breaks sections of the muon.mod.json if this option is a
	 * {@link MainModLoadOption}).
	 * <p>
	 * Most plugins are not expected to implement this. */
	default void onLoadOptionAdded(LoadOption option) {}

	/**
	 * <p>
	 * Most plugins are not expected to implement this. */
	default void onLoadOptionRemoved(LoadOption option) {}
}
