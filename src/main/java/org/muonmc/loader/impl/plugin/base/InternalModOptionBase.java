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

package org.muonmc.loader.impl.plugin.base;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;
import org.muonmc.loader.impl.metadata.qmj.InternalModMetadata;
import org.muonmc.loader.api.gui.MuonLoaderIcon;
import org.muonmc.loader.api.gui.MuonLoaderText;
import org.muonmc.loader.api.plugin.ModMetadataExt;
import org.muonmc.loader.api.plugin.MuonPluginContext;
import org.muonmc.loader.api.plugin.solver.ModLoadOption;
import org.muonmc.loader.api.plugin.solver.QuiltFileHasher;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public abstract class InternalModOptionBase extends ModLoadOption {

	protected final MuonPluginContext pluginContext;
	protected final ModMetadataExt metadata;
	protected final Path from, resourceRoot;
	protected final boolean mandatory;
	protected final boolean requiresRemap;
	protected final MuonLoaderIcon fileIcon;

	public InternalModOptionBase(
			MuonPluginContext pluginContext, ModMetadataExt meta, Path from,
		MuonLoaderIcon fileIcon, Path resourceRoot, boolean mandatory, boolean requiresRemap) {

		this.pluginContext = pluginContext;
		this.metadata = Objects.requireNonNull(meta, "meta");
		this.from = Objects.requireNonNull(from, "from");
		this.fileIcon = Objects.requireNonNull(fileIcon, "fileIcon");
		this.resourceRoot = Objects.requireNonNull(resourceRoot, "resourceRoot");
		this.mandatory = mandatory;
		this.requiresRemap = requiresRemap;
	}

	@Override
	public ModMetadataExt metadata() {
		return metadata;
	}

	@Override
	public Path from() {
		return from;
	}

	@Override
	public MuonLoaderIcon modFileIcon() {
		return fileIcon;
	}

	@Override
	public Path resourceRoot() {
		return resourceRoot;
	}

	@Override
	public boolean isMandatory() {
		return mandatory;
	}

	@Override
	public String toString() {
		return "{" + getClass().getName() + " '" + metadata.id() + "' from " //
			+ pluginContext.manager().describePath(from) + "}";
	}

	@Override
	public MuonPluginContext loader() {
		return pluginContext;
	}

	@Override
	public String shortString() {
		return toString();
	}

	@Override
	public String getSpecificInfo() {
		return toString();
	}

	protected abstract String nameOfType();

	@Override
	public MuonLoaderText describe() {
		return MuonLoaderText.translate("solver.option.mod.quilt_impl", nameOfType(), metadata.id(), pluginContext.manager().describePath(from));
	}

	@Override
	public @Nullable String namespaceMappingFrom() {
		if (requiresRemap) {
			if (!(metadata instanceof InternalModMetadata)) {
				throw new AbstractMethodError("// TODO: Handle remapping mods from plugins! (" + metadata.getClass() + ")");
			}
			return ((InternalModMetadata) metadata).intermediateMappings();
		} else {
			return null;
		}
	}

	@Override
	public boolean needsTransforming() {
		return true;
	}

	@Override
	public byte[] computeOriginHash(QuiltFileHasher hasher) throws IOException {
		return hasher.computeNormalHash(from);
	}
}
