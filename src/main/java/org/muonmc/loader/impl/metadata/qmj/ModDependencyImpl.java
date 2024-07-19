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

package org.muonmc.loader.impl.metadata.qmj;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;
import org.muonmc.loader.api.ModDependency;
import org.muonmc.loader.api.ModDependencyIdentifier;
import org.muonmc.loader.api.VersionRange;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
final class ModDependencyImpl {
	ModDependencyImpl() {
	}

	static abstract class CollectionImpl extends AbstractCollection<ModDependency.Only> implements ModDependency {
		private final String location;
		private final List<ModDependency.Only> conditions;

		CollectionImpl(String location, Collection<ModDependency> conditions) {
			this.location = location;
			// For simplicities sake we flatten here
			List<ModDependency.Only> flattened = new ArrayList<>();
			for (ModDependency dep : conditions) {
				if (dep instanceof ModDependency.Only) {
					flattened.add((ModDependency.Only) dep);
				} else {
					if (getClass() != dep.getClass()) {
						throw new IllegalArgumentException("You cannot mix any with all!");
					}
					flattened.addAll((CollectionImpl) dep);
				}
			}
			ModDependency.Only[] array = flattened.toArray(new ModDependency.Only[0]);
			this.conditions = Collections.unmodifiableList(Arrays.asList(array));
		}

		@Override
		public Iterator<ModDependency.Only> iterator() {
			return this.conditions.iterator();
		}

		@Override
		public int size() {
			return this.conditions.size();
		}

		@Override
		public String toString() {
			return location;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) {
				return false;
			}
			return conditions.equals(((CollectionImpl) obj).conditions);
		}

		@Override
		public int hashCode() {
			return conditions.hashCode();
		}
	}

	static final class AnyImpl extends CollectionImpl implements ModDependency.Any {
		AnyImpl(String location, Collection<ModDependency> conditions) {
			super(location, conditions);
		}
	}

	static final class AllImpl extends CollectionImpl  implements ModDependency.All {
		AllImpl(String location, Collection<ModDependency> conditions) {
			super(location, conditions);
		}
	}

	static final class OnlyImpl implements ModDependency.Only {
		private final String location;
		private final ModDependencyIdentifier id;
		private final VersionRange range;
		private final String reason;
		private final boolean optional;
		private final ModDependency unless;

		/**
		 * Creates a ModDependency that matches any version of a specific mod id.
		 */
		OnlyImpl(String location, ModDependencyIdentifier id) {
			this(location, id, VersionRange.ANY, "", false, null);
		}
		OnlyImpl(String location, ModDependencyIdentifier id, VersionRange range, @Nullable String reason, boolean optional, @Nullable ModDependency unless) {
			// We need to have at least one constraint
			if (range.isEmpty()) {
				throw new IllegalArgumentException("A ModDependency must have at least one constraint");
			}
			this.location = location;
			this.id = id;
			this.range = range;
			this.reason = reason != null ? reason : "";
			this.optional = optional;
			this.unless = unless;
		}

		@Override
		public ModDependencyIdentifier id() {
			return this.id;
		}

		@Override
		public VersionRange versionRange() {
			return range;
		}

		@Override
		public String reason() {
			return this.reason;
		}

		@Override
		public @Nullable ModDependency unless() {
			return this.unless;
		}

		@Override
		public boolean optional() {
			return this.optional;
		}

		@Override
		public boolean shouldIgnore() {
			// TODO: Read fields for loader plugins, and check them earlier and store the result in a field!
			return false;
		}

		@Override
		public String toString() {
			return location;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;
			if (!(obj instanceof OnlyImpl)) {
				return false;
			}
			OnlyImpl other = (OnlyImpl) obj;
			return optional == other.optional//
				&& Objects.equals(id, other.id)//
				&& Objects.equals(range, other.range)//
				&& Objects.equals(unless, other.unless);
		}

		@Override
		public int hashCode() {
			return Objects.hash(id, range, optional, unless);
		}
	}
}
