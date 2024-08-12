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

package org.muonmc.loader.impl.metadata.qmj;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.muonmc.loader.impl.metadata.qmj.ModDependencyIdentifierImpl;

public class ModDependencyIdentifierImplTester {
	@Test
	void testRawConstructor() {
		String id = "muon_loader";
		ModDependencyIdentifierImpl idIdentifier = new ModDependencyIdentifierImpl(id);
		Assertions.assertEquals("", idIdentifier.mavenGroup());
		Assertions.assertEquals(id, idIdentifier.id());
		Assertions.assertEquals(id, idIdentifier.toString());

		String groupId = "org.muonmc:muon_loader";
		ModDependencyIdentifierImpl groupIdIdentifier = new ModDependencyIdentifierImpl(groupId);
		Assertions.assertEquals("org.muonmc", groupIdIdentifier.mavenGroup());
		Assertions.assertEquals("muon_loader", groupIdIdentifier.id());
		Assertions.assertEquals(groupId, groupIdIdentifier.toString());
	}

	@Test
	void testIndividualConstructor() {
		String group = "org.muonmc";
		String id = "muon_loader";
		ModDependencyIdentifierImpl idIdentifier = new ModDependencyIdentifierImpl("", id);
		Assertions.assertEquals("", idIdentifier.mavenGroup());
		Assertions.assertEquals(id, idIdentifier.id());
		Assertions.assertEquals(id, idIdentifier.toString());

		ModDependencyIdentifierImpl groupIdIdentifier = new ModDependencyIdentifierImpl(group, id);
		Assertions.assertEquals(group, groupIdIdentifier.mavenGroup());
		Assertions.assertEquals(id, groupIdIdentifier.id());
		Assertions.assertEquals("org.muonmc:muon_loader", groupIdIdentifier.toString());
	}
}
