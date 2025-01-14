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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Pattern;

import org.muonmc.loader.impl.filesystem.MuonJoinedFileSystem;
import org.muonmc.loader.impl.metadata.qmj.InternalModMetadata;
import org.muonmc.loader.impl.metadata.qmj.ModMetadataReader;
import org.muonmc.loader.impl.metadata.qmj.QuiltOverrides;
import org.muonmc.loader.impl.metadata.qmj.V1ModMetadataBuilder;
import org.quiltmc.json5.exception.ParseException;
import org.muonmc.loader.api.FasterFiles;
import org.muonmc.loader.api.LoaderValue;
import org.muonmc.loader.api.ModDependency;
import org.muonmc.loader.api.ModMetadata.ProvidedMod;
import org.muonmc.loader.api.gui.LoaderGuiClosed;
import org.muonmc.loader.api.gui.LoaderGuiException;
import org.muonmc.loader.api.gui.MuonDisplayedError;
import org.muonmc.loader.api.gui.MuonLoaderGui;
import org.muonmc.loader.api.gui.MuonLoaderIcon;
import org.muonmc.loader.api.gui.MuonLoaderText;
import org.muonmc.loader.api.MuonLoader;
import org.muonmc.loader.api.Version;
import org.muonmc.loader.api.VersionRange;
import org.muonmc.loader.api.plugin.ModLocation;
import org.muonmc.loader.api.plugin.ModMetadataExt;
import org.muonmc.loader.api.plugin.MuonPluginContext;
import org.muonmc.loader.api.plugin.MuonPluginManager;
import org.muonmc.loader.api.plugin.gui.PluginGuiTreeNode;
import org.muonmc.loader.api.plugin.gui.PluginGuiTreeNode.SortOrder;
import org.muonmc.loader.api.plugin.solver.AliasedLoadOption;
import org.muonmc.loader.api.plugin.solver.LoadOption;
import org.muonmc.loader.api.plugin.solver.ModLoadOption;
import org.muonmc.loader.api.plugin.solver.RuleContext;
import org.muonmc.loader.impl.MuonLoaderImpl;
import org.muonmc.loader.impl.game.GameProvider;
import org.muonmc.loader.impl.game.GameProvider.BuiltinMod;
import org.muonmc.loader.impl.plugin.BuiltinMuonPlugin;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;
import org.muonmc.loader.impl.util.SystemProperties;
import org.muonmc.loader.impl.util.log.Log;
import org.muonmc.loader.impl.util.log.LogCategory;

/** Quilt-loader's plugin. For simplicities sake this is a builtin plugin - and cannot be disabled, or reloaded (since
 * quilt-loader can't reload itself to a different version). */
@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public class StandardMuonPlugin extends BuiltinMuonPlugin {

	public static final boolean DEBUG_PRINT_STATE = Boolean.getBoolean(SystemProperties.DEBUG_MOD_SOLVING);
	public static final boolean DISBALE_BUILTIN_MIXIN_EXTRAS = Boolean.getBoolean(SystemProperties.DISABLE_BUILTIN_MIXIN_EXTRAS);
	public static final boolean DEBUG_OVERRIDE_FILE = Boolean.getBoolean(SystemProperties.DEBUG_OVERRIDE_FILE);

	private QuiltOverrides overrides;
	private final Map<String, OptionalModIdDefintion> modDefinitions = new HashMap<>();

	@Override
	public void load(MuonPluginContext context, Map<String, LoaderValue> previousData) {
		super.load(context, previousData);
		loadOverrides();
	}

	private void loadOverrides() {
		Path overrideFile = context().manager().getConfigDirectory().resolve("quilt-loader-overrides.json").toAbsolutePath();
		if (DEBUG_OVERRIDE_FILE) {
			Log.info(LogCategory.GENERAL, "Attempting to load the override file " + overrideFile);
		}
		boolean isNew = false;
		try {
			if (!Files.exists(overrideFile)) {
				if (Boolean.getBoolean(SystemProperties.GENERATE_OVERRIDES_FILE)) {
					Log.info(LogCategory.GENERAL, "Creating new (empty) override file at " + overrideFile);
					Files.createFile(overrideFile);
					isNew = true;
				} else if (DEBUG_OVERRIDE_FILE) {
					Log.info(LogCategory.GENERAL, "File not found.");
				}
			} else if (DEBUG_OVERRIDE_FILE) {
				Log.info(LogCategory.GENERAL, "Found file, loading...");
			}
			overrides = new QuiltOverrides(overrideFile);
			if (DEBUG_OVERRIDE_FILE) {
				Log.info(LogCategory.GENERAL, "Valid overrides file loaded.");
				Log.info(LogCategory.GENERAL, "It contains " + overrides.pathOverrides.size() + " path overrides and " + overrides.patternOverrides.size() +" pattern overrides:");
				for (Map.Entry<String, QuiltOverrides.ModOverrides> entry : overrides.pathOverrides.entrySet()) {
					Log.info(LogCategory.GENERAL, "- " + entry.getKey());
				}
				for (Entry<Pattern, QuiltOverrides.ModOverrides> entry : overrides.patternOverrides.entrySet()) {
					Log.info(LogCategory.GENERAL, "- R\"" + entry.getKey() + "\"");
				}
			}
		} catch (ParseException | IOException e) {
			Exception[] mostRecentException = { e };
			MuonLoaderText title = MuonLoaderText.translate("error.quilt_overrides.io_parse.title");
			if (isNew) {
				title = MuonLoaderText.translate("error.quilt_overrides.new_blank_file.title");
			}
			MuonDisplayedError error = MuonLoaderGui.createError(title);
			if (isNew) {
				error.appendDescription(MuonLoaderText.translate("error.quilt_overrides.new_blank_file.desc"));
			} else {
				error.appendDescription(MuonLoaderText.of(e.getMessage()));
			}
			error.appendThrowable(e);
			error.addFileViewButton(MuonLoaderText.translate("button.view_file", "config/"), overrideFile).icon(MuonLoaderGui.iconFolder());
			error.addFileEditButton(overrideFile).icon(MuonLoaderGui.iconJsonFile());
			error.addOpenLinkButton(MuonLoaderText.translate("error.quilt_overrides.button.wiki"), "https://github.com/QuiltMC/quilt-loader/wiki/Dependency-Overrides");
			final boolean changeTitleToo = isNew;
			error.addActionButton(MuonLoaderText.translate("button.reload"), () -> {
				try {
					overrides = new QuiltOverrides(overrideFile);
					error.setFixed();
				} catch (ParseException | IOException e2) {
					mostRecentException[0] = e2;
					e2.printStackTrace();
					if (changeTitleToo) {
						// TODO: Change the title back!
					}
					error.clearDescription();
					error.appendDescription(MuonLoaderText.of(e2.getMessage()));
				}
			}).icon(MuonLoaderGui.iconReload());

			try {
				MuonLoaderGui.openErrorGui(error);
				return;
			} catch (LoaderGuiException ex) {
				mostRecentException[0].addSuppressed(ex);
			} catch (LoaderGuiClosed closed) {
				// Either closed correctly or ignored
			}

			if (overrides == null) {
				Exception ex = mostRecentException[0];
				MuonDisplayedError error2 = context().reportError(title);
				error2.appendDescription(MuonLoaderText.of(ex.getMessage()));
				error2.addFileViewButton(overrideFile);
				error2.appendReportText("Failed to read the quilt-loader-overrides.json file!");
				error2.appendThrowable(ex);
				context().haltLoading();
			}
		}
	}

	public boolean hasDepsChanged(ModLoadOption mod) {
		QuiltOverrides.ModOverrides modOverrides = overrides.pathOverrides.get(context().manager().describePath(mod.from()));
		return modOverrides != null && modOverrides.hasDepsChanged();
	}

	public boolean hasDepsRemoved(ModLoadOption mod) {
		QuiltOverrides.ModOverrides modOverrides = overrides.pathOverrides.get(context().manager().describePath(mod.from()));
		return modOverrides != null && modOverrides.hasDepsRemoved();
	}

	public void addBuiltinMods(GameProvider game) {
		int gameIndex = 1;
		for (BuiltinMod mod : game.getBuiltinMods()) {
			addBuiltinMod(mod, "game-" + gameIndex);
			gameIndex++;
		}

		String javaVersion = System.getProperty("java.specification.version").replaceFirst("^1\\.", "");
		V1ModMetadataBuilder javaMeta = new V1ModMetadataBuilder();
		javaMeta.id = "java";
		javaMeta.group = "builtin";
		javaMeta.version = Version.of(javaVersion);
		javaMeta.name = System.getProperty("java.vm.name");
		String javaHome = System.getProperty("java.home");
		Path javaPath = new File(javaHome).toPath();
		if (javaPath.getNameCount() == 0) {
			throw new Error("Invalid java.home value? '" + javaHome + "' for vm '" + javaMeta.name + "'");
		}
		addSystemMod(new BuiltinMod(Collections.singletonList(javaPath), javaMeta.build()), "java");
	}

	private void addSystemMod(BuiltinMod mod, String name) {
		addInternalMod(mod, name, true);
	}

	private void addBuiltinMod(BuiltinMod mod, String name) {
		addInternalMod(mod, name, false);
	}

	private void addInternalMod(BuiltinMod mod, String name, boolean system) {

		boolean changed = false;
		List<Path> openedPaths = new ArrayList<>();

		for (Path from : mod.paths) {

			Path inside = null;

			Path fileName = from.getFileName();
			if (fileName != null && fileName.toString().endsWith(".jar")) {
				try {
					inside = FileSystems.newFileSystem(from, (ClassLoader) null).getPath("/");
				} catch (IOException e) {
					// A bit odd, but not necessarily a crash-worthy issue
					e.printStackTrace();
				}
			}

			if (inside == null) {
				openedPaths.add(from);
			} else {
				changed = true;
				openedPaths.add(inside);
			}
		}

		Path from = join(mod.paths, name);
		Path inside = changed ? join(openedPaths, name) : from;

		// We don't go via context().addModOption since we don't really have a good gui node to base it off
		context().ruleContext().addOption(
			system //
				? new SystemModOption(context(), mod.metadata, from, inside) //
				: new BuiltinModOption(context(), mod.metadata, from, inside)
		);
	}

	private static Path join(List<Path> paths, String name) {
		if (paths.size() == 1) {
			return paths.get(0);
		} else {
			return new MuonJoinedFileSystem(name, paths).getRoot();
		}
	}

	@Override
	public ModLoadOption[] scanZip(Path root, ModLocation location, PluginGuiTreeNode guiNode) throws IOException {

		Path parent = context().manager().getParent(root);

		if (!parent.getFileName().toString().endsWith(".jar")) {
			return null;
		}

		return scan0(root, MuonLoaderGui.iconJarFile(), location, true, guiNode);
	}

	@Override
	public ModLoadOption[] scanFolder(Path folder, ModLocation location, PluginGuiTreeNode guiNode) throws IOException {
		return scan0(folder, MuonLoaderGui.iconFolder(), location, false, guiNode);
	}

	private ModLoadOption[] scan0(Path root, MuonLoaderIcon fileIcon, ModLocation location, boolean isZip,
		PluginGuiTreeNode guiNode) throws IOException {

		Path qmj = root.resolve("muon.mod.json");
		if (!FasterFiles.isRegularFile(qmj)) {
			return null;
		}

		try {
			InternalModMetadata meta = ModMetadataReader.read(qmj, context().manager(), guiNode);

			Path from = root;
			if (isZip) {
				from = context().manager().getParent(root);
			}

			jars: for (String jar : meta.jars()) {
				Path inner = root;
				for (String part : jar.split("/")) {
					if ("..".equals(part)) {
						continue jars;
					}
					inner = inner.resolve(part);
				}

				if (inner == from) {
					continue;
				}

				PluginGuiTreeNode jarNode = guiNode.addChild(MuonLoaderText.of(jar), SortOrder.ALPHABETICAL_ORDER);
				if (DISBALE_BUILTIN_MIXIN_EXTRAS) {
					if (MuonLoaderImpl.MOD_ID.equals(meta.id())) {
						if (inner.toString().startsWith("/META-INF/jars/mixinextras-")) {
							Log.info(LogCategory.GENERAL, "Disabling loader's builtin mixin extras library due to command line flag");
							jarNode.addChild(MuonLoaderText.translate("mixin_extras.disabled"));
							jarNode.mainIcon(MuonLoaderGui.iconDisabled());
							continue;
						}
					}
				}
				context().addFileToScan(inner, jarNode, false);
			}

			// a mod needs to be remapped if we are in a development environment, and the mod
			// did not come from the classpath
			boolean requiresRemap = !location.onClasspath() && MuonLoader.isDevelopmentEnvironment();
			return new ModLoadOption[] { new MuonModOption(
				context(), meta, from, fileIcon, root, location.isDirect(), requiresRemap
			) };
		} catch (ParseException parse) {
			MuonLoaderText title = MuonLoaderText.translate(
				"gui.text.invalid_metadata.title", "muon.mod.json", parse.getMessage()
			);
			MuonDisplayedError error = context().reportError(title);
			String describedPath = context().manager().describePath(qmj);
			error.appendReportText("Invalid 'muon.mod.json' metadata file:" + describedPath);
			error.appendDescription(MuonLoaderText.translate("gui.text.invalid_metadata.desc.0", describedPath));
			error.appendThrowable(parse);
			context().manager().getRealContainingFile(root).ifPresent(real ->
					error.addFileViewButton(real)
							.icon(MuonLoaderGui.iconJarFile().withDecoration(MuonLoaderGui.iconQuilt()))
			);

			guiNode.addChild(MuonLoaderText.translate("gui.text.invalid_metadata", parse.getMessage()))//
				.setError(parse, error);
			return null;
		}
	}

	@Override
	public void onLoadOptionAdded(LoadOption option) {

		// We handle dependency solving for all plugins that don't tell us not to.

		if (option instanceof AliasedLoadOption) {
			AliasedLoadOption alias = (AliasedLoadOption) option;
			if (alias.getTarget() != null) {
				return;
			}
		}

		if (option instanceof ModLoadOption) {
			ModLoadOption mod = (ModLoadOption) option;
			ModMetadataExt metadata = mod.metadata();
			RuleContext ctx = context().ruleContext();

			OptionalModIdDefintion def = modDefinitions.get(mod.id());
			if (def == null) {
				def = new OptionalModIdDefintion(context().manager(), ctx, mod.id());
				modDefinitions.put(mod.id(), def);
				ctx.addRule(def);
			}

			// TODO: this minecraft-specific extension should be moved to its own plugin
			// If the mod's environment doesn't match the current one,
			// then add a rule so that the mod is never loaded.
			if (!metadata.environment().matches(context().manager().getEnvironment())) {
				ctx.addRule(new DisabledModIdDefinition(context(), mod));
			} else if (mod.isMandatory()) {
				ctx.addRule(new MandatoryModIdDefinition(context(), mod));
			}

			if (metadata.shouldQuiltDefineProvides()) {
				Collection<? extends ProvidedMod> provides = metadata.provides();

				for (ProvidedMod provided : provides) {
					PluginGuiTreeNode guiNode = context().manager().getGuiNode(mod)//
						.addChild(MuonLoaderText.translate("gui.text.providing", provided.id()));
					guiNode.mainIcon(MuonLoaderGui.iconUnknownFile());
					context().addModLoadOption(new ProvidedModOption(mod, provided), guiNode);
				}
			}

			if (metadata.shouldQuiltDefineDependencies()) {

				Path path = mod.from();
				String described = context().manager().describePath(path);
				if (Boolean.getBoolean(SystemProperties.DEBUG_DUMP_OVERRIDE_PATHS)) {
					Log.info(LogCategory.DISCOVERY, "Override path: '" + described + "'");
				}

				Collection<ModDependency> depends = metadata.depends();
				Collection<ModDependency> breaks = metadata.breaks();

				List<SingleOverrideEntry> overrideList = new ArrayList<>();
				QuiltOverrides.ModOverrides byPath = overrides.pathOverrides.get(described);
				if (byPath != null) {
					overrideList.add(new SingleOverrideEntry(byPath, true));
				}

				for (Entry<Pattern, QuiltOverrides.ModOverrides> entry : overrides.patternOverrides.entrySet()) {
					if (!entry.getKey().matcher(mod.id()).matches()) {
						continue;
					}

					overrideList.add(new SingleOverrideEntry(entry.getValue(), false));
				}

				depends = new HashSet<>(depends);
				breaks = new HashSet<>(breaks);

				for (SingleOverrideEntry override : overrideList) {
					replace(override.fuzzy, override.overrides.dependsOverrides, depends);
					replace(override.fuzzy, override.overrides.breakOverrides, breaks);
				}

				if (MuonLoaderImpl.MOD_ID.equals(metadata.id())) {
					if (DISBALE_BUILTIN_MIXIN_EXTRAS) {
						depends.removeIf(dep -> dep instanceof ModDependency.Only && ((ModDependency.Only) dep).id().id().equals("mixinextras"));
					}
				}

				for (ModDependency dep : depends) {
					if (!dep.shouldIgnore()) {
						ctx.addRule(createModDepLink(context().manager(), ctx, mod, dep));
					}
				}

				for (ModDependency dep : breaks) {
					if (!dep.shouldIgnore()) {
						ctx.addRule(createModBreaks(context().manager(), ctx, mod, dep));
					}
				}
			}
		}
	}

	private static void warn(String msg) {
		Log.warn(LogCategory.DISCOVERY, "'" + msg);
	}

	static final class SingleOverrideEntry {
		final QuiltOverrides.ModOverrides overrides;
		final boolean fuzzy;

		public SingleOverrideEntry(QuiltOverrides.ModOverrides overrides, boolean fuzzy) {
			this.overrides = overrides;
			this.fuzzy = fuzzy;
		}
	}

	private static void replace(boolean fuzzy, QuiltOverrides.SpecificOverrides overrides, Collection<ModDependency> in) {
		for (Map.Entry<ModDependency, ModDependency> entry : overrides.replacements.entrySet()) {
			if (remove(fuzzy, in, entry.getKey(), "replace")) {
				in.add(entry.getValue());
			}
		}

		for (ModDependency removal : overrides.removals) {
			remove(fuzzy, in, removal, "remove");
		}

		in.addAll(overrides.additions);
	}

	private static boolean remove(boolean fuzzy, Collection<ModDependency> in, ModDependency removal, String name) {
		if (in.remove(removal)) {
			return true;
		}

		if (fuzzy && removal instanceof ModDependency.Only) {
			ModDependency.Only specific = (ModDependency.Only) removal;
			if (specific.versionRange() == VersionRange.ANY && specific.unless() == null) {
				List<ModDependency> matches = new ArrayList<>();
				for (ModDependency dep : in) {
					if (!(dep instanceof ModDependency.Only)) {
						continue;
					}
					ModDependency.Only current = (ModDependency.Only) dep;
					if (!current.id().equals(specific.id())) {
						continue;
					}
					matches.add(current);
				}

				if (matches.size() == 1) {
					in.remove(matches.get(0));
					return true;
				} else if (matches.size() > 1) {
					warn("Found multiple matching ModDependency " + name + " when using using fuzzy matching!");
					logModDep("", "", removal);
					warn("Comparison:");
					if (in.isEmpty()) {
						warn("  (None left)");
					}
					int index = 0;
					for (ModDependency with : in) {
						logCompare(" ", "[" + index++ + "]: ", removal, with);
					}
					return false;
				}
			}
		}

		warn("Failed to find the ModDependency 'from' to " + name + "!");
		logModDep("", "", removal);
		warn("Comparison:");
		if (in.isEmpty()) {
			warn("  (None left)");
		}
		int index = 0;
		for (ModDependency with : in) {
			logCompare(" ", "[" + index++ + "]: ", removal, with);
		}
		return false;
	}

	private static void logModDep(String indent, String firstPrefix, ModDependency value) {
		if (value instanceof ModDependency.Only) {
			ModDependency.Only only = (ModDependency.Only) value;
			warn(indent + firstPrefix + only.id() + " versions " + only.versionRange() + //
				(only.optional() ? "(optional)" : "(mandatory)"));
			if (only.unless() != null) {
				logModDep(indent + "  ", "unless ", value);
			}
		} else if (value instanceof ModDependency.All) {
			ModDependency.All all = (ModDependency.All) value;
			warn(indent + firstPrefix + " all of: ");
			for (ModDependency.Only on : all) {
				logModDep(indent + "  ", "", on);
			}
		} else {
			ModDependency.Any all = (ModDependency.Any) value;
			warn(indent + firstPrefix + " any of: ");
			for (ModDependency.Only on : all) {
				logModDep(indent + "  ", "", on);
			}
		}
	}

	private static void logCompare(String indent, String firstPrefix, ModDependency from, ModDependency with) {
		if (Objects.equals(from, with)) {
			warn(indent + firstPrefix + "matches!");
			return;
		}
		if (from instanceof ModDependency.Only) {
			if (with instanceof ModDependency.Only) {
				ModDependency.Only f = (ModDependency.Only) from;
				ModDependency.Only t = (ModDependency.Only) with;
				warn(indent + firstPrefix + "on:");
				logCompareValue(indent + "  id ", f.id(), t.id());
				logCompareValue(indent + "  versions ", f.versionRange(), t.versionRange());
				logCompareValue(indent + "  optional? ", f.optional(), t.optional());
				if (f.unless() == null && t.unless() == null) {
					logCompare(indent + "  ", "unless ", f.unless(), t.unless());
				}
			} else {
				warn(indent + firstPrefix + "'from' is a direct dependency, but 'with' is not.");
				logModDep(indent + "  ", "from: ", from);
			}
		} else if (from instanceof ModDependency.All) {
			if (with instanceof ModDependency.All) {
				ModDependency.All f = (ModDependency.All) from;
				ModDependency.All t = (ModDependency.All) with;
				warn(indent + firstPrefix + "all of:");
				logListCompare(indent, f, t);
			} else {
				warn(indent + firstPrefix + "'from' is an all-of dependency list, but 'with' is not.");
				logModDep(indent + "  ", "from: ", from);
			}
		} else if (from != null) {
			if (with instanceof ModDependency.Any) {
				ModDependency.Any f = (ModDependency.Any) from;
				ModDependency.Any t = (ModDependency.Any) with;
				warn(indent + firstPrefix + "any of:");
				logListCompare(indent, f, t);
			} else {
				warn(indent + firstPrefix + "'from' is an any-of dependency list, but 'with' is not.");
				logModDep(indent + "  ", "from: ", from);
			}
		} else {
			warn(indent + firstPrefix + "'from' is missing, but 'with' is not.");
		}
	}

	private static void logListCompare(String indent, Collection<ModDependency.Only> f, Collection<ModDependency.Only> t) {
		Iterator<ModDependency.Only> fromIter = f.iterator();
		Iterator<ModDependency.Only> withIter = t.iterator();
		int index = 0;
		while (true) {
			ModDependency.Only from = fromIter.hasNext() ? fromIter.next() : null;
			ModDependency.Only with = withIter.hasNext() ? withIter.next() : null;
			logCompare(indent + "  ", "[" + index++ + "]", from, with);
		}
	}

	private static void logCompareValue(String start, Object a, Object b) {
		warn(start + a + (a.equals(b) ? " == " : " != ") + b);
	}

	public static MuonRuleDep createModDepLink(
			MuonPluginManager manager, RuleContext ctx, LoadOption option,
		ModDependency dep) {

		if (dep instanceof ModDependency.Any) {
			ModDependency.Any any = (ModDependency.Any) dep;

			return new MuonRuleDepAny(manager, ctx, option, any);
		} else {
			ModDependency.Only only = (ModDependency.Only) dep;

			return new MuonRuleDepOnly(manager, ctx, option, only);
		}
	}

	public static MuonRuleBreak createModBreaks(
			MuonPluginManager manager, RuleContext ctx, LoadOption option,
		ModDependency dep) {
		if (dep instanceof ModDependency.All) {
			ModDependency.All any = (ModDependency.All) dep;

			return new MuonRuleBreakAll(manager, ctx, option, any);
		} else {
			ModDependency.Only only = (ModDependency.Only) dep;

			return new MuonRuleBreakOnly(manager, ctx, option, only);
		}
	}
}
