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

import java.util.concurrent.atomic.AtomicInteger;

import org.muonmc.loader.api.ModDependency;
import org.muonmc.loader.api.gui.MuonLoaderText;
import org.muonmc.loader.api.plugin.solver.LoadOption;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

/** Used to indicate part of a {@link ModDependency} from muon.mod.json. */
@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public class MuonModDepOption extends LoadOption {
	private static final AtomicInteger IDS = new AtomicInteger();

	public final ModDependency dep;
	private final int id = IDS.incrementAndGet();

	public MuonModDepOption(ModDependency dep) {
		this.dep = dep;
	}

	@Override
	public String toString() {
		return dep.toString();
	}

	@Override
	public MuonLoaderText describe() {
		return MuonLoaderText.translate("solver.option.dep_technical", id, dep.toString());
	}
}
