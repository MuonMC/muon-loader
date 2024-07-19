/*
 * Copyright 2022, 2023 QuiltMC
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

package org.muonmc.loader.impl.plugin;

import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.Callable;

import org.muonmc.loader.api.plugin.MuonPluginContext;
import org.muonmc.loader.api.gui.MuonDisplayedError;
import org.muonmc.loader.api.gui.MuonLoaderText;
import org.muonmc.loader.api.gui.MuonTreeNode;
import org.muonmc.loader.api.gui.MuonTreeNode.SortOrder;
import org.muonmc.loader.api.plugin.MuonPluginManager;
import org.muonmc.loader.api.plugin.MuonPluginTask;
import org.muonmc.loader.api.plugin.gui.PluginGuiTreeNode;
import org.muonmc.loader.api.plugin.solver.LoadOption;
import org.muonmc.loader.api.plugin.solver.ModLoadOption;
import org.muonmc.loader.api.plugin.solver.Rule;
import org.muonmc.loader.api.plugin.solver.RuleContext;
import org.muonmc.loader.api.plugin.solver.TentativeLoadOption;
import org.muonmc.loader.impl.gui.MuonStatusNode;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
abstract class BasePluginContext implements MuonPluginContext {

	final MuonPluginManagerImpl manager;
	final String pluginId;
	final RuleContext ruleContext = new ModRuleContext();

	MuonStatusNode extraModsRoot;
	Collection<Rule> blameableRules = null;
	Rule blamedRule = null;

	public BasePluginContext(MuonPluginManagerImpl manager, String pluginId) {
		this.manager = manager;
		this.pluginId = pluginId;
	}

	@Override
	public MuonPluginManager manager() {
		return manager;
	}

	@Override
	public String pluginId() {
		return pluginId;
	}

	@Override
	public String toString() {
		return "CTX:" + pluginId;
	}

	@Override
	@Deprecated
	public void addFileToScan(Path file, PluginGuiTreeNode guiNode, boolean direct) {
		// TODO: Log / store / do something to store the plugin
		manager.scanModFile(file, new ModLocationImpl(false, direct), (MuonStatusNode) guiNode);
	}

	@Override
	public void addFileToScan(Path file, MuonTreeNode guiNode, boolean direct) {
		// TODO: Log / store / do something to store the plugin
		manager.scanModFile(file, new ModLocationImpl(false, direct), (MuonStatusNode) guiNode);
	}

	@Override
	public boolean addFolderToScan(Path folder) {
		return manager.addModFolder(folder, this);
	}

	@Override
	public void lockZip(Path path) {
		// TODO Auto-generated method stub
		throw new AbstractMethodError("// TODO: Implement this!");
	}

	@Override
	public MuonDisplayedError reportError(MuonLoaderText title) {
		return manager.reportError(this, title);
	}

	@Override
	public void haltLoading() {
		manager.haltLoading(this);
	}

	@Override
	public <V> MuonPluginTask<V> submit(Callable<V> task) {
		return manager.submit(this, task);
	}

	@Override
	public <V> MuonPluginTask<V> submitAfter(Callable<V> task, MuonPluginTask<?>... deps) {
		return manager.submitAfter(this, task, deps);
	}

	@Override
	public RuleContext ruleContext() {
		return ruleContext;
	}

	@Override
	public void addModLoadOption(ModLoadOption mod, PluginGuiTreeNode guiNode) {
		manager.addSingleModOption(mod, BasePluginContext.this, true, (MuonStatusNode) guiNode);
	}

	@Override
	public void addModLoadOption(ModLoadOption mod, MuonTreeNode guiNode) {
		manager.addSingleModOption(mod, BasePluginContext.this, true, (MuonStatusNode) guiNode);
	}

	@Override
	public <T extends LoadOption & TentativeLoadOption> void addTentativeOption(T option) {
		addTentativeOption0(option);
	}

	private void addTentativeOption0(LoadOption option) {
		manager.addLoadOption(option, this);
	}

	@Override
	public void blameRule(Rule rule) {
		if (blameableRules == null) {
			throw new IllegalStateException(
				"Cannot call 'blameRule' unless we are in the middle of recovering from a solver failure!"
			);
		}

		if (!blameableRules.contains(rule)) {
			throw new IllegalArgumentException("Cannot blame a rule that isn't part of the current problem!");
		}

		if (blamedRule != null) {
			throw new IllegalStateException("Cannot blame more than 1 rule!");
		}

		blamedRule = rule;
	}

	class ModRuleContext implements RuleContext {

		@Override
		public void addOption(LoadOption option) {
			if (option instanceof TentativeLoadOption) {
				addTentativeOption0(option);
			} else if (option instanceof ModLoadOption) {
				ModLoadOption mod = (ModLoadOption) option;
				if (extraModsRoot == null) {
					extraModsRoot = manager.getModsFromPluginsGuiNode().addChild(SortOrder.ALPHABETICAL_ORDER);
					extraModsRoot.text(MuonLoaderText.translate("gui.text.plugin", pluginId));
				}

				MuonStatusNode guiNode = extraModsRoot.addChild(MuonLoaderText.of(mod.id()));
				manager.addSingleModOption(mod, BasePluginContext.this, true, guiNode);
			} else {
				manager.addLoadOption(option, BasePluginContext.this);
			}
		}

		@Override
		public void setWeight(LoadOption option, Rule key, int weight) {
			manager.solver.setWeight(option, key, weight);
		}

		@Override
		public void removeOption(LoadOption option) {
			manager.removeLoadOption(option);
		}

		@Override
		public void addRule(Rule rule) {
			manager.addRule(rule);
		}

		@Override
		public void redefine(Rule rule) {
			manager.solver.redefine(rule);
		}
	}
}
