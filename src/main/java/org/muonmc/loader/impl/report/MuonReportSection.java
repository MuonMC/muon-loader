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

package org.muonmc.loader.impl.report;

import java.io.PrintWriter;

import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public abstract class MuonReportSection implements Comparable<MuonReportSection> {

	private final int ordering;
	final String name;
	boolean showInLogs = true;

	public MuonReportSection(int ordering, String name) {
		this.ordering = ordering;
		this.name = name;
	}

	@Override
	public final int compareTo(MuonReportSection o) {
		return Integer.compare(ordering, o.ordering);
	}

	public abstract void write(PrintWriter to);

	public void setShowInLogs(boolean b) {
		showInLogs = b;
	}
}
