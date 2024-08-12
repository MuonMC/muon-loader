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
public class MuonRuleDepAny extends MuonRuleDep {

	final MuonRuleDepOnly[] options;
	final ModDependency.Any publicDep;

	public MuonRuleDepAny(MuonPluginManager manager, RuleContext ctx, LoadOption option, ModDependency.Any any) {

		super(option);
		this.publicDep = any;
		List<MuonRuleDepOnly> optionList = new ArrayList<>();

		for (ModDependency.Only only : any) {
			if (!only.shouldIgnore()) {
				MuonModDepOption sub = new MuonModDepOption(only);
				ctx.addOption(sub);
				MuonRuleDepOnly dep = new MuonRuleDepOnly(manager, ctx, sub, only);
				ctx.addRule(dep);
				optionList.add(dep);
			}
		}

		this.options = optionList.toArray(new MuonRuleDepOnly[0]);
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
			array[i] = options[i].source;
		}
		array[i] = definer.negate(source);
		definer.atLeastOneOf(array);
	}

	@Override
	boolean hasAnyValidOptions() {
		for (MuonRuleDepOnly on : options) {
			if (on.hasAnyValidOptions()) {
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
		for (MuonRuleDepOnly on : options) {
			list.add(on.source);
		}
		return list;
	}

	@Override
	public void fallbackErrorDescription(StringBuilder errors) {
		errors.append("Dependancy for ");
		errors.append(source);
		errors.append(" on any of: ");

		for (MuonRuleDepOnly on : options) {
			errors.append("\n\t-");
			errors.append(on.source);
			errors.append(" ");
		}
	}

	@Override
	public void appendRuleDescription(Consumer<MuonLoaderText> to) {
		to.accept(MuonLoaderText.translate("solver.rule.dep.any", source.describe()));
		for (MuonRuleDepOnly on : options) {
			to.accept(on.source.describe());
		}
	}
}
