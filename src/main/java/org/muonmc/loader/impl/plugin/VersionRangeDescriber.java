/*
 * Copyright 2023, 2024 QuiltMC
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

import org.muonmc.loader.api.VersionInterval;
import org.muonmc.loader.api.VersionRange;
import org.muonmc.loader.api.gui.MuonLoaderText;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

import java.util.stream.Collectors;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public class VersionRangeDescriber {
	public static MuonLoaderText describe(String modName, VersionRange range, String depName, boolean transitive) {
		return describe(MuonLoaderText.of(modName), range, depName, transitive);
	}

	public static MuonLoaderText describe(MuonLoaderText modFrom, VersionRange range, String depName, boolean transitive) {
		return describe(modFrom, range, depName, true, transitive);
	}

	public static MuonLoaderText describe(String modFrom, VersionRange range, String depName, boolean isDep, boolean transitive) {
		return describe(MuonLoaderText.of(modFrom), range, depName, isDep, transitive);
	}

	public static MuonLoaderText describe(MuonLoaderText modFrom, VersionRange range, String depName, boolean isDep, boolean transitive) {
		String titleKey = "error." + (isDep ? "dep." : "break.") + (transitive ? "transitive." : "direct.");

		if (range.size() != 1) {
			for (VersionInterval interval : range) {
				// Handle the specific case of { [A,A] U [B,B] } (of any length)
				if (interval.getMin() == null || !interval.getMin().equals(interval.getMax())) {
					return MuonLoaderText.translate(getTransKey(titleKey, "ranged"), modFrom, range, depName);
				}
			}

			VersionInterval finalV = range.last();
			//noinspection DataFlowIssue -- we've confirmed that it can never be null above
			String list = range.headSet(finalV).stream().map(i -> i.getMin().toString()).collect(Collectors.joining(", "));
			return MuonLoaderText.translate(getTransKey(titleKey, "exact_list"), modFrom, list, finalV.getMin(), depName);
		}

		VersionInterval interval = range.first();

		// Negative infinity
		if (interval.getMin() == null) {
			// Positive infinity
			if (interval.getMax() == null) {
				return MuonLoaderText.translate(getTransKey(titleKey, "any"), modFrom, depName);
			} else {
				return MuonLoaderText.translate(getTransKey(titleKey, interval.isMaxInclusive() ? "lesser_equal" : "lesser"), modFrom, interval.getMax(), depName);
			}
		}

		// positive infinity
		if (interval.getMax() == null) {
			return MuonLoaderText.translate(getTransKey(titleKey, interval.isMinInclusive() ? "greater_equal" : "greater"), modFrom, interval.getMin(), depName);
		}

		if (interval.getMax().equals(interval.getMin())) {
			return MuonLoaderText.translate(getTransKey(titleKey, "exact"), modFrom, interval.getMax(), depName);
		}

		// ranged
		String extra = "range_" + (interval.isMinInclusive() ? "inc_" : "exc_") + (interval.isMaxInclusive() ? "inc" : "exc");

		return MuonLoaderText.translate(getTransKey(titleKey, extra), modFrom, interval.getMin(), interval.getMax(), depName);
	}

	private static String getTransKey(String titleKey, String extra) {
		return titleKey + extra + ".title";
	}
}
