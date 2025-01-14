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
public class MuonRuleDepOnly extends MuonRuleDep {

	private final MuonPluginManager manager;
	public final ModDependency.Only publicDep;
	private final List<ModLoadOption> validOptions;
	private final List<ModLoadOption> invalidOptions;
	private final List<ModLoadOption> allOptions;
	private boolean valid = true;

	public final MuonRuleDep unless;

	public MuonRuleDepOnly(MuonPluginManager manager, RuleContext ctx, LoadOption source, ModDependency.Only publicDep) {
		super(source);
		this.manager = manager;
		this.publicDep = publicDep;
		validOptions = new ArrayList<>();
		invalidOptions = new ArrayList<>();
		allOptions = new ArrayList<>();

		if (StandardMuonPlugin.DEBUG_PRINT_STATE) {
			Log.info(LogCategory.SOLVING, "Adding a mod dependency from " + source + " to " + publicDep.id().id());
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
		if (option == source && !valid) {
			valid = true;
			return true;
		}
		if (option instanceof ModLoadOption) {
			ModLoadOption mod = (ModLoadOption) option;

			if (!mod.id().equals(publicDep.id().id())) {
				return false;
			}

			allOptions.add(mod);

			String maven = publicDep.id().mavenGroup();
			boolean groupMatches = maven.isEmpty() || maven.equals(mod.group());

			if (groupMatches && publicDep.matches(mod.version())) {
				validOptions.add(mod);

				if (StandardMuonPlugin.DEBUG_PRINT_STATE) {
					Log.info(LogCategory.SOLVING, "  +  valid option: " + mod.fullString());
				}
			} else {
				invalidOptions.add(mod);

				if (StandardMuonPlugin.DEBUG_PRINT_STATE) {
					String reason = groupMatches ? "mismatched group" : "wrong version";
					Log.info(LogCategory.SOLVING, "  x  mismatching option: " + mod.fullString() + " because " + reason);
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean onLoadOptionRemoved(LoadOption option) {
		boolean changed = validOptions.remove(option);
		changed |= invalidOptions.remove(option);
		allOptions.remove(option);
		if (option == source && valid) {
			valid = false;
			changed = true;
		}
		return changed;
	}

	@Override
	public void define(RuleDefiner definer) {

		if (!valid) {
			return;
		}

		boolean optional = publicDep.optional();
		List<ModLoadOption> options = validOptions;
		boolean negateOptions = false;

		if (optional && options.isEmpty()) {
			options = invalidOptions;
			negateOptions = true;

			if (options.isEmpty()) {
				return;
			}
		}

		boolean allWrongEnvironment = !options.isEmpty();

		for (ModLoadOption option : options) {
			if (option.metadata().environment().matches(manager.getEnvironment())) {
				allWrongEnvironment = false;
				break;
			}
		}

		if (allWrongEnvironment) {
			return;
		}

		LoadOption[] array = new LoadOption[options.size() + (unless == null ? 1 : 2)];
		int i = 0;

		for (; i < options.size(); i++) {
			array[i] = options.get(i);
			if (negateOptions) {
				array[i] = definer.negate(array[i]);
			}
		}

		// i is incremented when we exit the for loop, so this is fine.
		array[i++] = definer.negate(source);
		if (unless != null) {
			array[i] = unless.source;
		}

		definer.atLeastOneOf(array);
	}

	@Override
	boolean hasAnyValidOptions() {
		return !validOptions.isEmpty();
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

	public List<ModLoadOption> getValidOptions() {
		return Collections.unmodifiableList(validOptions);
	}

	public List<ModLoadOption> getWrongOptions() {
		return Collections.unmodifiableList(invalidOptions);
	}

	public List<ModLoadOption> getAllOptions() {
		return Collections.unmodifiableList(allOptions);
	}

	@Override
	public void fallbackErrorDescription(StringBuilder errors) {

		if (publicDep.optional()) {
			errors.append("Optional dependency for ");
		} else {
			errors.append("Dependency for ");
		}

		errors.append(source);
		errors.append(" on ");
		errors.append(publicDep.id());
		errors.append(" versions ");
		errors.append(publicDep.versions());
		errors.append(" (");
		errors.append(validOptions.size());
		errors.append(" valid options, ");
		errors.append(invalidOptions.size());
		errors.append(" invalid options)");

		for (ModLoadOption option : validOptions) {
			errors.append("\n\t+ " + option.fullString());
		}

		for (ModLoadOption option : invalidOptions) {
			errors.append("\n\tx " + option.fullString());
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
		to.accept(MuonLoaderText.translate("solver.rule.dep.only", on));
		to.accept(MuonLoaderText.translate("solver.rule.dep.only.matching", validOptions.size()));
		for (ModLoadOption option : validOptions) {
			to.accept(MuonLoaderText.translate("solver.rule.mod_def.optional.source", option.describe()));
		}
		to.accept(MuonLoaderText.translate("solver.rule.dep.only.invalid", invalidOptions.size()));
		for (ModLoadOption option : invalidOptions) {
			to.accept(MuonLoaderText.translate("solver.rule.mod_def.optional.source", option.describe()));
		}
	}
}
