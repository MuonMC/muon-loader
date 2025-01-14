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

package org.muonmc.loader.impl.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.muonmc.loader.api.ModDependencyIdentifier;
import org.muonmc.loader.api.ModMetadata.ProvidedMod;
import org.muonmc.loader.api.VersionRange;
import org.muonmc.loader.api.gui.MuonDisplayedError;
import org.muonmc.loader.api.gui.MuonLoaderGui;
import org.muonmc.loader.api.gui.MuonLoaderText;
import org.muonmc.loader.api.plugin.solver.AliasedLoadOption;
import org.muonmc.loader.api.plugin.solver.LoadOption;
import org.muonmc.loader.api.plugin.solver.ModLoadOption;
import org.muonmc.loader.api.plugin.solver.Rule;
import org.muonmc.loader.impl.plugin.muon.DisabledModIdDefinition;
import org.muonmc.loader.impl.plugin.muon.MandatoryModIdDefinition;
import org.muonmc.loader.impl.plugin.muon.OptionalModIdDefintion;
import org.muonmc.loader.impl.plugin.muon.MuonRuleBreakOnly;
import org.muonmc.loader.impl.plugin.muon.MuonRuleDepOnly;
import org.muonmc.loader.impl.util.FileUtil;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
class SolverErrorHelper {

	private final MuonPluginManagerImpl manager;
	private final List<SolverError> errors = new ArrayList<>();

	SolverErrorHelper(MuonPluginManagerImpl manager) {
		this.manager = manager;
	}

	void reportErrors() {
		for (SolverError error : errors) {
			error.report(manager);
		}
	}

	private void addError(SolverError error) {
		for (SolverError e2 : errors) {
			if (error.mergeInto(e2)) {
				return;
			}
		}
		errors.add(error);
	}

	void reportSolverError(Collection<Rule> rules) {

		List<RuleLink> links = new ArrayList<>();
		Map<LoadOption, OptionLink> option2Link = new HashMap<>();

		for (Rule rule : rules) {
			RuleLink ruleLink = new RuleLink(rule);
			links.add(ruleLink);

			for (LoadOption from : rule.getNodesFrom()) {
				from = getTarget(from);
				OptionLink optionLink = option2Link.computeIfAbsent(from, OptionLink::new);
				ruleLink.from.add(optionLink);
				optionLink.to.add(ruleLink);
			}

			for (LoadOption from : rule.getNodesTo()) {
				from = getTarget(from);
				OptionLink optionLink = option2Link.computeIfAbsent(from, OptionLink::new);
				ruleLink.to.add(optionLink);
				optionLink.from.add(ruleLink);
			}
		}

		List<RuleLink> rootRules = new ArrayList<>();
		for (RuleLink link : links) {
			if (link.from.isEmpty()) {
				rootRules.add(link);
			}
		}

		if (reportKnownSolverError(rootRules)) {
			return;
		}

		addError(new UnhandledError(rules));
	}

	private static LoadOption getTarget(LoadOption from) {
		if (from instanceof AliasedLoadOption) {
			LoadOption target = ((AliasedLoadOption) from).getTarget();
			if (target != null) {
				return target;
			}
		}
		return from;
	}

	static class RuleLink {
		final Rule rule;

		final List<OptionLink> from = new ArrayList<>();
		final List<OptionLink> to = new ArrayList<>();

		RuleLink(Rule rule) {
			this.rule = rule;
		}

		void addTo(OptionLink to) {
			this.to.add(to);
			to.from.add(this);
		}

		void addFrom(OptionLink from) {
			this.from.add(from);
			from.to.add(this);
		}
	}

	static class OptionLink {
		final LoadOption option;

		final List<RuleLink> from = new ArrayList<>();
		final List<RuleLink> to = new ArrayList<>();

		OptionLink(LoadOption option) {
			this.option = option;
			if (LoadOption.isNegated(option)) {
				throw new IllegalArgumentException("Call 'OptionLinkBase.get' instead of this!!");
			}
		}
	}

	private boolean reportKnownSolverError(List<RuleLink> rootRules) {
		if (rootRules.isEmpty()) {
			return false;
		}

		if (rootRules.size() == 1) {
			RuleLink rootRule = rootRules.get(0);

			if (rootRule.rule instanceof MandatoryModIdDefinition) {
				return reportSingleMandatoryError(rootRule);
			}

			return false;
		}

		if (reportDuplicateMandatoryMods(rootRules)) {
			return true;
		}

		if (reportBreakingMods(rootRules)) {
			return true;
		}

		return false;
	}

	/** Reports an error where there is only one root rule, of a {@link MandatoryModIdDefinition}. */
	private boolean reportSingleMandatoryError(RuleLink rootRule) {
		MandatoryModIdDefinition def = (MandatoryModIdDefinition) rootRule.rule;
		ModLoadOption mandatoryMod = def.option;

		if (rootRule.to.size() != 1) {//
			// This should always be the case, since a mandatory definition
			// always defines to a single source
			return false;
		}

		// TODO: Put this whole thing in a loop
		// and then check for transitive (and fully valid) dependency paths!

		OptionLink modLink = rootRule.to.get(0);
		List<OptionLink> modLinks = Collections.singletonList(modLink);
		Set<OptionLink> nextModLinks = new LinkedHashSet<>();
		List<List<OptionLink>> fullChain = new ArrayList<>();

		String groupOn = null;
		String modOn = null;
		ModDependencyIdentifier modIdOn = null;

		while (true) {
			groupOn = null;
			modOn = null;
			Boolean areAllInvalid = null;
			nextModLinks = new LinkedHashSet<>();

			for (OptionLink link : modLinks) {
				if (link.to.isEmpty()) {
					// Apparently nothing is stopping this mod from loading
					// (so there's a bug here somewhere)
					return false;
				}

				if (link.to.size() > 1) {
					// FOR NOW
					// just handle a single dependency / problem at a time
					return false;
				}

				RuleLink rule = link.to.get(0);
				if (rule.rule instanceof MuonRuleDepOnly) {
					MuonRuleDepOnly dep = (MuonRuleDepOnly) rule.rule;

					ModDependencyIdentifier id = dep.publicDep.id();
					if (groupOn == null) {
						if (!id.mavenGroup().isEmpty()) {
							groupOn = id.mavenGroup();
						}
					} else if (!id.mavenGroup().isEmpty() && !groupOn.equals(id.mavenGroup())) {
						// A previous dep targets a different group of the same mod, so this is a branching condition
						return false;
					}

					if (modOn == null) {
						modOn = id.id();
					} else if (!modOn.equals(id.id())) {
						// A previous dep targets a different mod, so this is a branching condition
						return false;
					}

					modIdOn = id;

					if (dep.publicDep.unless() != null) {
						// TODO: handle 'unless' clauses!
						return false;
					}

					if (dep.getValidOptions().isEmpty()) {
						// Loop exit condition!
						if (areAllInvalid != null && areAllInvalid) {
							continue;
						} else if (nextModLinks.isEmpty()) {
							areAllInvalid = true;
							continue;
						} else {
							// Some deps are mismatched, others aren't
							// so this isn't necessarily a flat dep chain
							// (However it could be if the chain ends with a mandatory mod
							// like minecraft, which doesn't have anything else at the end of the chain
							return false;
						}
					}

					if (dep.getAllOptions().size() != rule.to.size()) {
						return false;
					}

					// Now check that they all match up
					for (LoadOption option : dep.getAllOptions()) {
						option = getTarget(option);
						boolean found = false;
						for (OptionLink link2 : rule.to) {
							if (option == link2.option) {
								found = true;
								nextModLinks.add(link2);
							}
						}
						if (!found) {
							return false;
						}
					}

				} else {
					// TODO: Handle other conditions!
					return false;
				}
			}

			fullChain.add(modLinks);

			if (areAllInvalid != null && areAllInvalid) {
				// Now we have validated that every mod in the previous list all depend on the same mod

				// Technically we should think about how to handle multiple *conflicting* version deps.
				// For example:
				// buildcraft requires abstract_base *
				// abstract_base 18 requires minecraft 1.18.x
				// abstract_base 19 requires minecraft 1.19.x

				// Technically we are just showing deps as "transitive" so
				// we just say that buildcraft requires minecraft 1.18.x or 1.19.x
				// so we need to make the required version list "bigger" rather than smaller

				// This breaks down if "abstract_base" requires an additional library though.
				// (Although we aren't handling that here)

				VersionRange fullRange = VersionRange.NONE;
				Set<ModLoadOption> allInvalidOptions = new HashSet<>();
				for (OptionLink link : fullChain.get(fullChain.size() - 1)) {
					// We validate all this earlier
					MuonRuleDepOnly dep = (MuonRuleDepOnly) link.to.get(0).rule;
					fullRange = VersionRange.ofRanges(Arrays.asList(fullRange, dep.publicDep.versionRange()));
					allInvalidOptions.addAll(dep.getWrongOptions());
				}

				for (OptionLink from : modLinks) {
					addError(new DependencyError(modIdOn, fullRange, (ModLoadOption) from.option, allInvalidOptions));
				}

				return true;
			}

			modLinks = new ArrayList<>(nextModLinks);
		}
	}

	private static void setIconFromMod(
			MuonPluginManagerImpl manager, ModLoadOption mandatoryMod,
		MuonDisplayedError error) {
		// TODO: Only upload a ModLoadOption's icon once!
		Map<String, byte[]> images = new HashMap<>();
		for (int size : new int[] { 16, 32 }) {
			String iconPath = mandatoryMod.metadata().icon(size);
			if (iconPath != null && ! images.containsKey(iconPath)) {
				Path path = mandatoryMod.resourceRoot().resolve(iconPath);
				try (InputStream stream = Files.newInputStream(path)) {
					images.put(iconPath, FileUtil.readAllBytes(stream));
				} catch (IOException io) {
					// TODO: Warn about this somewhere!
					io.printStackTrace();
				}
			}
		}

		if (!images.isEmpty()) {
			error.setIcon(MuonLoaderGui.createIcon(images.values().toArray(new byte[0][])));
		}
	}

	private static String getDepName(MuonRuleDepOnly dep) {
		String id = dep.publicDep.id().id();
		switch (id) {
			case "fabric":
				return "Fabric API";
			default:
				return "'" + id + "'";
		}
	}

	private boolean reportDuplicateMandatoryMods(List<RuleLink> rootRules) {
		// N+1 root rules for N duplicated mods:
		// 0 is the OptionalModIdDefintion
		// 1..N is either MandatoryModIdDefinition or DisabledModIdDefinition (and the mod is mandatory)

		if (rootRules.size() < 3) {
			return false;
		}

		// Step 1: Find the single OptionalModIdDefinition
		OptionalModIdDefintion optionalDef = null;
		for (RuleLink link : rootRules) {
			if (link.rule instanceof OptionalModIdDefintion) {
				if (optionalDef != null) {
					return false;
				}
				optionalDef = (OptionalModIdDefintion) link.rule;
			}
		}

		if (optionalDef == null) {
			return false;
		}

		// Step 2: Check to see if the rest of the root rules are MandatoryModIdDefinition
		// and they share some provided id (or real id)
		List<ModLoadOption> mandatories = new ArrayList<>();
		Set<String> commonIds = null;
		for (RuleLink link : rootRules) {
			if (link.rule == optionalDef) {
				continue;
			}

			if (link.rule instanceof MandatoryModIdDefinition) {
				MandatoryModIdDefinition mandatory = (MandatoryModIdDefinition) link.rule;
				if (commonIds == null) {
					commonIds = new HashSet<>();
					commonIds.add(mandatory.getModId());
					for (ProvidedMod provided : mandatory.option.metadata().provides()) {
						commonIds.add(provided.id());
					}
				} else {
					Set<String> fromThis = new HashSet<>();
					fromThis.add(mandatory.getModId());
					for (ProvidedMod provided : mandatory.option.metadata().provides()) {
						fromThis.add(provided.id());
					}
					commonIds.retainAll(fromThis);
					if (commonIds.isEmpty()) {
						return false;
					}
				}
				mandatories.add(mandatory.option);
			} else if (link.rule instanceof DisabledModIdDefinition) {
				DisabledModIdDefinition disabled = (DisabledModIdDefinition) link.rule;
				if (!disabled.option.isMandatory()) {
					return false;
				}
				if (!commonIds.contains(disabled.getModId())) {
					return false;
				}
				commonIds.clear();
				commonIds.add(disabled.getModId());
			} else {
				return false;
			}
		}

		if (mandatories.isEmpty()) {
			// So this means there's an OptionalModIdDefintion with only
			// DisabledModIdDefinitions as roots
			// that means this isn't a duplicate mandatory mods error!
			return false;
		}
		ModLoadOption firstMandatory = mandatories.get(0);
		String bestName = null;
		for (ModLoadOption option : mandatories) {
			if (commonIds.contains(option.id())) {
				bestName = option.metadata().name();
				break;
			}
		}

		if (bestName == null) {
			bestName = "id'" + commonIds.iterator().next() + "'";
		}

		// Title:
		// Duplicate mod: "BuildCraft"

		// Description:
		// - "buildcraft-all-9.0.0.jar"
		// - "buildcraft-all-9.0.1.jar"
		// Remove all but one.

		// With buttons to view each mod individually

		MuonLoaderText title = MuonLoaderText.translate("error.duplicate_mandatory", bestName);
		MuonDisplayedError error = manager.theMuonPluginContext.reportError(title);
		error.appendReportText("Duplicate mandatory mod ids " + commonIds);
		setIconFromMod(manager, firstMandatory, error);

		for (ModLoadOption option : mandatories) {
			String path = manager.describePath(option.from());
			// Just in case
			Optional<Path> container = manager.getRealContainingFile(option.from());
			error.appendDescription(MuonLoaderText.translate("error.duplicate_mandatory.mod", path));
			container.ifPresent(value -> error.addFileViewButton(MuonLoaderText.translate("button.view_file", value.getFileName()), value)
					.icon(option.modCompleteIcon()));

			error.appendReportText("- " + path);
		}

		error.appendDescription(MuonLoaderText.translate("error.duplicate_mandatory.desc"));

		return true;
	}

	private boolean reportBreakingMods(List<RuleLink> rootRules) {
		if (rootRules.size() != 2) {
			return false;
		}

		// ROOT[0] = A=RuleLink<MandatoryModIdDefinition>
		// -> B=OptionLink<ModLoadOption> -> nothing
		// ROOT[1] = C=RuleLink<MandatoryModIdDefinition>
		// -> D=OptionLink<ModLoadOption>
		// -> E=RuleLink<QuiltRuleBreakOnly> -> B

		RuleLink ruleA = rootRules.get(0);
		RuleLink ruleC = rootRules.get(1);
		MandatoryModIdDefinition modA, modC;
		OptionLink linkB, linkD;

		if (ruleA.rule instanceof MandatoryModIdDefinition) {
			modA = (MandatoryModIdDefinition) ruleA.rule;
			linkB = ruleA.to.get(0);
		} else {
			return false;
		}

		if (ruleC.rule instanceof MandatoryModIdDefinition) {
			modC = (MandatoryModIdDefinition) ruleC.rule;
			linkD = ruleC.to.get(0);
		} else {
			return false;
		}

		if (linkB.to.size() > linkD.to.size()) {
			RuleLink tempRule = ruleA;
			MandatoryModIdDefinition tempMod = modA;
			OptionLink tempLink = linkB;

			ruleA = ruleC;
			modA = modC;
			linkB = linkD;

			ruleC = tempRule;
			modC = tempMod;
			linkD = tempLink;
		}

		if (!linkB.to.isEmpty() || linkD.to.size() != 1) {
			return false;
		}

		RuleLink linkE = linkD.to.get(0);

		if (!(linkE.rule instanceof MuonRuleBreakOnly)) {
			return false;
		}

		MuonRuleBreakOnly ruleE = (MuonRuleBreakOnly) linkE.rule;
		if (linkE.to.size() != 1 || !linkE.to.contains(linkB)) {
			return false;
		}

		ModDependencyIdentifier modOn = ruleE.publicDep.id();
		VersionRange versionsOn = ruleE.publicDep.versionRange();
		ModLoadOption from = modC.option;
		Set<ModLoadOption> allBreakingOptions = new LinkedHashSet<>();
		allBreakingOptions.addAll(ruleE.getConflictingOptions());
		String reason = ruleE.publicDep.reason();
		this.errors.add(new BreakageError(modOn, versionsOn, from, allBreakingOptions, reason));

		return true;
	}

	/**
	 * A solver error which might have multiple real errors merged into one for display.
	 */
	static abstract class SolverError {

		/** Attempts to merge this error into the given error. This object itself shouldn't be modified.
		 * 
		 * @return True if the destination object was modified (and this was merged into it), false otherwise. */
		abstract boolean mergeInto(SolverError into);

		abstract void report(MuonPluginManagerImpl manager);
	}

	static class UnhandledError extends SolverError {

		final Collection<Rule> rules;

		public UnhandledError(Collection<Rule> rules) {
			this.rules = rules;
		}

		@Override
		boolean mergeInto(SolverError into) {
			return false;
		}

		@Override
		void report(MuonPluginManagerImpl manager) {
			MuonDisplayedError error = manager.theMuonPluginContext.reportError(
				MuonLoaderText.translate("error.unhandled_solver")
			);
			error.appendDescription(MuonLoaderText.translate("error.unhandled_solver.desc"));
			error.addOpenQuiltSupportButton();
			error.appendReportText("Unhandled solver error involving the following rules:");

			StringBuilder sb = new StringBuilder();
			int number = 1;
			for (Rule rule : rules) {
				error.appendDescription(MuonLoaderText.translate("error.unhandled_solver.desc.rule_n", number, rule.getClass()));
				rule.appendRuleDescription(error::appendDescription);
				error.appendReportText("Rule " + number++ + ":");
				sb.setLength(0);
				// TODO: Rename 'fallbackErrorDescription'
				// to something like 'fallbackReportDescription'
				// and then clean up all of the implementations to be more readable.
				rule.fallbackErrorDescription(sb);
				error.appendReportText(sb.toString());
			}
			error.appendReportText("");
		}
	}

	static class DependencyError extends SolverError {
		final ModDependencyIdentifier modOn;
		final VersionRange versionsOn;
		final Set<ModLoadOption> from = new LinkedHashSet<>();
		final Set<ModLoadOption> allInvalidOptions;

		DependencyError(ModDependencyIdentifier modOn, VersionRange versionsOn, ModLoadOption from, Set<ModLoadOption> allInvalidOptions) {
			this.modOn = modOn;
			this.versionsOn = versionsOn;
			this.from.add(from);
			this.allInvalidOptions = allInvalidOptions;
		}

		@Override
		boolean mergeInto(SolverError into) {
			if (into instanceof DependencyError) {
				DependencyError depDst = (DependencyError) into;
				if (!modOn.equals(depDst.modOn) || !versionsOn.equals(depDst.versionsOn)) {
					return false;
				}
				depDst.from.addAll(from);
				return true;
			}
			return false;
		}

		@Override
		void report(MuonPluginManagerImpl manager) {

			boolean transitive = false;
			boolean missing = allInvalidOptions.isEmpty();

			// Title:
			// "BuildCraft" requires [version 1.5.1] of "Quilt Standard Libraries", which is
			// missing!

			// Description:
			// BuildCraft is loaded from '<mods>/buildcraft-9.0.0.jar'
			ModLoadOption mandatoryMod = from.iterator().next();
			String rootModName = from.size() > 1 ? from.size() + " mods" : mandatoryMod.metadata().name();

			MuonLoaderText first = VersionRangeDescriber.describe(rootModName, versionsOn, modOn.id(), transitive);

			Object[] secondData = new Object[allInvalidOptions.size() == 1 ? 1 : 0];
			String secondKey = "error.dep.";
			if (missing) {
				secondKey += "missing";
			} else if (allInvalidOptions.size() > 1) {
				secondKey += "multi_mismatch";
			} else {
				secondKey += "single_mismatch";
				secondData[0] = allInvalidOptions.iterator().next().version().toString();
			}
			MuonLoaderText second = MuonLoaderText.translate(secondKey + ".title", secondData);
			MuonLoaderText title = MuonLoaderText.translate("error.dep.join.title", first, second);
			MuonDisplayedError error = manager.theMuonPluginContext.reportError(title);

			setIconFromMod(manager, mandatoryMod, error);

			Map<Path, ModLoadOption> realPaths = new LinkedHashMap<>();

			for (ModLoadOption mod : from) {
				Object[] modDescArgs = { mod.metadata().name(), manager.describePath(mod.from()) };
				error.appendDescription(MuonLoaderText.translate("info.root_mod_loaded_from", modDescArgs));
				manager.getRealContainingFile(mod.from()).ifPresent(p -> realPaths.putIfAbsent(p, mod));
			}

			for (Map.Entry<Path, ModLoadOption> entry : realPaths.entrySet()) {
				error.addFileViewButton(entry.getKey()).icon(entry.getValue().modCompleteIcon());
			}

			String issuesUrl = mandatoryMod.metadata().contactInfo().get("issues");
			if (issuesUrl != null) {
				error.addOpenLinkButton(MuonLoaderText.translate("button.mod_issue_tracker", mandatoryMod.metadata().name()), issuesUrl);
			}

			StringBuilder report = new StringBuilder(rootModName);
			report.append(" requires");
			if (VersionRange.ANY.equals(versionsOn)) {
				report.append(" any version of ");
			} else {
				report.append(" version ").append(versionsOn).append(" of ");
			}
			report.append(modOn);// TODO
			report.append(", which is missing!");
			error.appendReportText(report.toString(), "");

			for (ModLoadOption mod : from) {
				error.appendReportText("- " + manager.describePath(mod.from()));
			}
		}
	}

	static class BreakageError extends SolverError {
		final ModDependencyIdentifier modOn;
		final VersionRange versionsOn;
		final Set<ModLoadOption> from = new LinkedHashSet<>();
		final Set<ModLoadOption> allBreakingOptions;
		final String reason;

		BreakageError(ModDependencyIdentifier modOn, VersionRange versionsOn, ModLoadOption from, Set<
			ModLoadOption> allBreakingOptions, String reason) {
			this.modOn = modOn;
			this.versionsOn = versionsOn;
			this.from.add(from);
			this.allBreakingOptions = allBreakingOptions;
			this.reason = reason;
		}

		@Override
		boolean mergeInto(SolverError into) {
			if (into instanceof BreakageError) {
				BreakageError depDst = (BreakageError) into;
				if (!modOn.equals(depDst.modOn) || !versionsOn.equals(depDst.versionsOn)) {
					return false;
				}
				depDst.from.addAll(from);
				return true;
			}
			return false;
		}

		@Override
		void report(MuonPluginManagerImpl manager) {

			boolean transitive = false;

			// Title:
			// "BuildCraft" breaks with [version 1.5.1] of "Quilt Standard Libraries", but it's present!

			// Description:
			// BuildCraft is loaded from '<mods>/buildcraft-9.0.0.jar'
			ModLoadOption mandatoryMod = from.iterator().next();
			String rootModName = from.size() > 1 ? from.size() + " mods" : mandatoryMod.metadata().name();

			MuonLoaderText first = VersionRangeDescriber.describe(
				rootModName, versionsOn, modOn.id(), false, transitive
			);

			Object[] secondData = new Object[allBreakingOptions.size() == 1 ? 1 : 0];
			String secondKey = "error.break.";
			if (allBreakingOptions.size() > 1) {
				secondKey += "multi_conflict";
			} else {
				secondKey += "single_conflict";
				secondData[0] = allBreakingOptions.iterator().next().version().toString();
			}
			MuonLoaderText second = MuonLoaderText.translate(secondKey + ".title", secondData);
			MuonLoaderText title = MuonLoaderText.translate("error.break.join.title", first, second);
			MuonDisplayedError error = manager.theMuonPluginContext.reportError(title);

			setIconFromMod(manager, mandatoryMod, error);

			if (!reason.isEmpty()) {
				error.appendDescription(MuonLoaderText.translate("error.reason", reason));
				// A newline after the reason was desired here, but do you think Swing loves nice things?
			}

			Map<Path, ModLoadOption> realPaths = new LinkedHashMap<>();

			for (ModLoadOption mod : from) {
				Object[] modDescArgs = { mod.metadata().name(), manager.describePath(mod.from()) };
				error.appendDescription(MuonLoaderText.translate("info.root_mod_loaded_from", modDescArgs));
				manager.getRealContainingFile(mod.from()).ifPresent(p -> realPaths.putIfAbsent(p, mod));
			}

			for (ModLoadOption mod : allBreakingOptions) {
				Object[] modDescArgs = { mod.metadata().name(), manager.describePath(mod.from()) };
				error.appendDescription(MuonLoaderText.translate("info.root_mod_loaded_from", modDescArgs));
				manager.getRealContainingFile(mod.from()).ifPresent(p -> realPaths.putIfAbsent(p, mod));
			}

			for (Map.Entry<Path, ModLoadOption> entry : realPaths.entrySet()) {
				error.addFileViewButton(entry.getKey()).icon(entry.getValue().modCompleteIcon());
			}

			String issuesUrl = mandatoryMod.metadata().contactInfo().get("issues");
			if (issuesUrl != null) {
				error.addOpenLinkButton(
					MuonLoaderText.translate("button.mod_issue_tracker", mandatoryMod.metadata().name()), issuesUrl
				);
			}

			StringBuilder report = new StringBuilder(rootModName);
			report.append(" breaks");
			if (VersionRange.ANY.equals(versionsOn)) {
				report.append(" all versions of ");
			} else {
				report.append(" version ").append(versionsOn).append(" of ");
			}
			report.append(modOn);// TODO
			report.append(", which is present!");
			error.appendReportText(report.toString(), "");

			if (!reason.isEmpty()) {
				error.appendReportText("Breaking mod's reason: " + reason, "");
			}

			for (ModLoadOption mod : from) {
				error.appendReportText("- " + manager.describePath(mod.from()));
			}
		}
	}
}
