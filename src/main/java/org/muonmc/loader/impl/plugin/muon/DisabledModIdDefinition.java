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

package org.muonmc.loader.impl.plugin.muon;

import java.util.function.Consumer;

import org.muonmc.loader.api.gui.MuonLoaderText;
import org.muonmc.loader.api.plugin.MuonPluginContext;
import org.muonmc.loader.api.plugin.solver.LoadOption;
import org.muonmc.loader.api.plugin.solver.ModLoadOption;
import org.muonmc.loader.api.plugin.solver.RuleDefiner;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

/** A concrete definition that mandates that the modid must <strong>not</strong> be loaded by the given {@link ModLoadOption}. */
@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public final class DisabledModIdDefinition extends ModIdDefinition {
	public final ModLoadOption option;
	private boolean valid = true;
	private final MuonPluginContext ctx;

	public DisabledModIdDefinition(MuonPluginContext ctx, ModLoadOption candidate) {
		this.option = candidate;
		this.ctx = ctx;
	}

	@Override
	public String getModId() {
		return option.id();
	}

	@Override
	ModLoadOption[] sources() {
		return new ModLoadOption[] { option };
	}

	@Override
	public void define(RuleDefiner definer) {
		if (valid) {
			definer.atMost(0, option);
		}
	}

	@Override
	public boolean onLoadOptionAdded(LoadOption option) {
		if (option == this.option && !valid) {
			valid = true;
			return true;
		}
		return false;
	}

	@Override
	public boolean onLoadOptionRemoved(LoadOption option) {
		if (option == this.option && valid) {
			valid = false;
			return true;
		}
		return false;
	}

	@Override
	String getFriendlyName() {
		return option.metadata().name() + " (" + option.id() + ")";
	}

	@Override
	public String toString() {
		return "disabled " + option.fullString();
	}

	@Override
	public void fallbackErrorDescription(StringBuilder errors) {
		errors.append("Disabled mod ");
		errors.append(getFriendlyName());
		errors.append(" v");
		errors.append(option.metadata().version());
	}

	@Override
	public void appendRuleDescription(Consumer<MuonLoaderText> to) {
		String from = ctx.manager().describePath(option.from());
		to.accept(MuonLoaderText.translate("solver.rule.mod_def.disabled", getModId(), option.metadata().version(), from));
	}
}
