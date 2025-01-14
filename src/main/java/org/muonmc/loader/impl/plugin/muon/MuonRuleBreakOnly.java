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
import org.muonmc.loader.api.plugin.solver.ModLoadOption;
import org.muonmc.loader.api.plugin.solver.RuleContext;
import org.muonmc.loader.api.plugin.solver.RuleDefiner;
import org.muonmc.loader.impl.plugin.VersionRangeDescriber;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;
import org.muonmc.loader.impl.util.log.Log;
import org.muonmc.loader.impl.util.log.LogCategory;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public class MuonRuleBreakOnly extends MuonRuleBreak {
	public final ModDependency.Only publicDep;
	final List<ModLoadOption> conflictingOptions;
	final List<ModLoadOption> okayOptions;
	final List<ModLoadOption> allOptions;

	final MuonRuleDep unless;

	public MuonRuleBreakOnly(MuonPluginManager manager, RuleContext ctx, LoadOption source, ModDependency.Only publicDep) {
		super(source);
		this.publicDep = publicDep;
		conflictingOptions = new ArrayList<>();
		okayOptions = new ArrayList<>();
		allOptions = new ArrayList<>();

		if (StandardMuonPlugin.DEBUG_PRINT_STATE) {
			Log.info(LogCategory.SOLVING, "Adding a mod break from " + source + " to " + publicDep.id().id());
		}

		ModDependency except = publicDep.unless();
		if (except != null && !except.shouldIgnore()) {
			MuonModDepOption option = new MuonModDepOption(except);
			ctx.addOption(option);
			this.unless = StandardMuonPlugin.createModDepLink(manager, ctx, option, except);
			ctx.addRule(unless);
		} else {
			this.unless = null;
		}
	}

	@Override
	public boolean onLoadOptionAdded(LoadOption option) {
		if (option instanceof ModLoadOption) {
			ModLoadOption mod = (ModLoadOption) option;

			if (!mod.id().equals(publicDep.id().id())) {
				return false;
			}

			allOptions.add(mod);

			String maven = publicDep.id().mavenGroup();
			boolean groupMatches = maven.isEmpty() || maven.equals(mod.group());

			if (groupMatches && publicDep.matches(mod.version())) {
				conflictingOptions.add(mod);

				if (StandardMuonPlugin.DEBUG_PRINT_STATE) {
					Log.info(LogCategory.SOLVING, "  x  conflicting option: " + mod.fullString());
				}
				return true;
			} else {
				okayOptions.add(mod);

				if (StandardMuonPlugin.DEBUG_PRINT_STATE) {
					String reason = !groupMatches ? "different group" : "different version";
					Log.info(LogCategory.SOLVING, "  +  okay option: " + mod.fullString() + " because " + reason);
				}
			}

		}
		return false;
	}

	@Override
	public boolean onLoadOptionRemoved(LoadOption option) {
		boolean changed = conflictingOptions.remove(option);
		changed |= okayOptions.remove(option);
		allOptions.remove(option);
		return changed;
	}

	@Override
	public void define(RuleDefiner definer) {

		// "optional" is meaningless for breaks
		List<ModLoadOption> conflicts = conflictingOptions;

		if (conflicts.isEmpty()) {
			return;
		}

		LoadOption[] options = new LoadOption[unless == null ? 2 : 3];
		options[1] = definer.negate(source);

		if (unless != null) {
			options[2] = definer.negate(unless.source);
		}

		for (ModLoadOption conflict : conflicts) {
			options[0] = definer.negate(conflict);
			definer.atLeastOneOf(options);
		}
	}

	@Override
	boolean hasAnyConflictingOptions() {
		return !conflictingOptions.isEmpty();
	}

	public List<ModLoadOption> getConflictingOptions() {
		return Collections.unmodifiableList(conflictingOptions);
	}

	public List<ModLoadOption> getOkayOptions() {
		return Collections.unmodifiableList(okayOptions);
	}

	public List<ModLoadOption> getAllOptions() {
		return Collections.unmodifiableList(allOptions);
	}

	@Override
	public String toString() {
		return publicDep.toString();
	}

	@Override
	public Collection<? extends LoadOption> getNodesFrom() {
		return Collections.singleton(source);
	}

	@Override
	public Collection<? extends LoadOption> getNodesTo() {
		return allOptions;
	}

	@Override
	public void fallbackErrorDescription(StringBuilder errors) {

		errors.append("Breakage for ");

		errors.append(source);
		errors.append(" on ");
		errors.append(publicDep.id());
		errors.append(" versions ");
		errors.append(publicDep.versions());
		errors.append(" (");
		errors.append(conflictingOptions.size());
		errors.append(" breaking options, ");
		errors.append(okayOptions.size());
		errors.append(" okay options)");

		for (ModLoadOption option : conflictingOptions) {
			errors.append("\n\tx " + option.fullString());
		}

		for (ModLoadOption option : okayOptions) {
			errors.append("\n\t+ " + option.fullString());
		}
	}

	@Override
	public void appendRuleDescription(Consumer<MuonLoaderText> to) {
		StringBuilder id = new StringBuilder(publicDep.id().mavenGroup());
		if (id.length() > 0) {
			id.append(":");
		}
		id.append(publicDep.id().id());
		Object on = VersionRangeDescriber.describe(source.describe(), publicDep.versionRange(), id.toString(), false);
		to.accept(MuonLoaderText.translate("solver.rule.break.only", on));
		to.accept(MuonLoaderText.translate("solver.rule.break.only.conflicting", conflictingOptions.size()));
		for (ModLoadOption option : conflictingOptions) {
			to.accept(MuonLoaderText.translate("solver.rule.mod_def.optional.source", option.describe()));
		}
		to.accept(MuonLoaderText.translate("solver.rule.break.only.okay", okayOptions.size()));
		for (ModLoadOption option : okayOptions) {
			to.accept(MuonLoaderText.translate("solver.rule.mod_def.optional.source", option.describe()));
		}
	}
}
