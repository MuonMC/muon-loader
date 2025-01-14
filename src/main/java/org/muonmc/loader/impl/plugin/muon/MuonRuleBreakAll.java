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

package org.muonmc.loader.impl.plugin.muon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.muonmc.loader.api.ModDependency;
import org.muonmc.loader.api.gui.MuonLoaderText;
import org.muonmc.loader.api.plugin.MuonPluginManager;
import org.muonmc.loader.api.plugin.solver.LoadOption;
import org.muonmc.loader.api.plugin.solver.RuleContext;
import org.muonmc.loader.api.plugin.solver.RuleDefiner;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public class MuonRuleBreakAll extends MuonRuleBreak {

	final MuonRuleBreakOnly[] options;
	final ModDependency.All publicDep;

	public MuonRuleBreakAll(MuonPluginManager manager, RuleContext ctx, LoadOption option, ModDependency.All all) {

		super(option);
		this.publicDep = all;
		List<MuonRuleBreakOnly> optionList = new ArrayList<>();

		for (ModDependency.Only only : all) {
			if (!only.shouldIgnore()) {
				MuonModDepOption sub = new MuonModDepOption(only);
				ctx.addOption(sub);
				MuonRuleBreakOnly dep = new MuonRuleBreakOnly(manager, ctx, sub, only);
				ctx.addRule(dep);
				optionList.add(dep);
			}
		}

		this.options = optionList.toArray(new MuonRuleBreakOnly[0]);
	}

	@Override
	public boolean onLoadOptionAdded(LoadOption option) {
		return false;
	}

	@Override
	public boolean onLoadOptionRemoved(LoadOption option) {
		return false;
	}

	@Override
	public void define(RuleDefiner definer) {
		LoadOption[] array = new LoadOption[options.length + 1];
		int i = 0;

		for (; i < options.length; i++) {
			array[i] = definer.negate(options[i].source);
		}

		array[i] = source;
		definer.atMost(array.length - 1, array);
	}

	@Override
	boolean hasAnyConflictingOptions() {
		for (MuonRuleBreakOnly on : options) {
			if (on.hasAnyConflictingOptions()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		// FIXME: This needs a proper toString()
		return "" + publicDep;
	}

	@Override
	public Collection<? extends LoadOption> getNodesFrom() {
		return Collections.singleton(source);
	}

	@Override
	public Collection<? extends LoadOption> getNodesTo() {
		List<LoadOption> list = new ArrayList<>();
		for (MuonRuleBreakOnly on : options) {
			list.add(on.source);
		}
		return list;
	}

	@Override
	public void fallbackErrorDescription(StringBuilder errors) {
		errors.append("Breakage for ");
		errors.append(source);
		errors.append(" on all of: ");

		for (MuonRuleBreakOnly on : options) {
			errors.append("\n\t-");
			errors.append(on.source);
			errors.append(" ");
		}
	}

	@Override
	public void appendRuleDescription(Consumer<MuonLoaderText> to) {
		to.accept(MuonLoaderText.translate("solver.rule.break.all", source.describe()));
		for (MuonRuleBreakOnly on : options) {
			to.accept(on.source.describe());
		}
	}
}
