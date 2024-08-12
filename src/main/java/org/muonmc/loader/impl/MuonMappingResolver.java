/*
 * Copyright 2016 FabricMC
 * Copyright 2022-2024 QuiltMC
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

package org.muonmc.loader.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import net.fabricmc.mappings.EntryTriple;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;
import org.muonmc.loader.api.MappingResolver;

import net.fabricmc.mapping.tree.ClassDef;
import net.fabricmc.mapping.tree.Descriptored;
import net.fabricmc.mapping.tree.TinyTree;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
class MuonMappingResolver implements MappingResolver {
	private final Supplier<TinyTree> mappingsSupplier;
	private final Set<String> namespaces;
	private final Map<String, NamespaceData> namespaceDataMap = new ConcurrentHashMap<>();
	private final String targetNamespace;

	private static class NamespaceData {
		private final Map<String, String> classNames = new HashMap<>();
		private final Map<String, String> classNamesInverse = new HashMap<>();
		private final Map<EntryTriple, String> fieldNames = new HashMap<>();
		private final Map<EntryTriple, String> methodNames = new HashMap<>();
	}

	MuonMappingResolver(Supplier<TinyTree> mappingsSupplier, String targetNamespace) {
		this.mappingsSupplier = mappingsSupplier;
		this.targetNamespace = targetNamespace;
		namespaces = Collections.unmodifiableSet(new HashSet<>(mappingsSupplier.get().getMetadata().getNamespaces()));
	}

	protected final NamespaceData getNamespaceData(String namespace) {
		return namespaceDataMap.computeIfAbsent(namespace, (fromNamespace) -> {
			if (!namespaces.contains(namespace)) {
				throw new IllegalArgumentException("Unknown namespace: " + namespace + " (we know about " + namespaces + ")");
			}

			NamespaceData data = new NamespaceData();
			TinyTree mappings = mappingsSupplier.get();
			Map<String, String> classNameMap = new HashMap<>();

			for (ClassDef classEntry : mappings.getClasses()) {
				String fromClass = mapClassName(classNameMap, classEntry.getName(fromNamespace));
				String toClass = mapClassName(classNameMap, classEntry.getName(targetNamespace));

				data.classNames.put(fromClass, toClass);
				data.classNamesInverse.put(toClass, fromClass);

				String mappedClassName = mapClassName(classNameMap, fromClass);

				recordMember(fromNamespace, classEntry.getFields(), data.fieldNames, mappedClassName);
				recordMember(fromNamespace, classEntry.getMethods(), data.methodNames, mappedClassName);
			}

			return data;
		});
	}

	private static String replaceSlashesWithDots(String cname) {
		return cname.replace('/', '.');
	}

	private String mapClassName(Map<String, String> classNameMap, String s) {
		return classNameMap.computeIfAbsent(s, MuonMappingResolver::replaceSlashesWithDots);
	}

	private <T extends Descriptored> void recordMember(String fromNamespace, Collection<T> descriptoredList, Map<EntryTriple, String> putInto, String fromClass) {
		for (T descriptored : descriptoredList) {
			EntryTriple fromEntry = new EntryTriple(fromClass, descriptored.getName(fromNamespace), descriptored.getDescriptor(fromNamespace));
			putInto.put(fromEntry, descriptored.getName(targetNamespace));
		}
	}

	@Override
	public Collection<String> getNamespaces() {
		return namespaces;
	}

	@Override
	public String getCurrentRuntimeNamespace() {
		return targetNamespace;
	}

	@Override
	public String mapClassName(String namespace, String className) {
		if (className.indexOf('/') >= 0) {
			throw new IllegalArgumentException("Class names must be provided in dot format: " + className);
		}

		return getNamespaceData(namespace).classNames.getOrDefault(className, className);
	}

	@Override
	public String unmapClassName(String namespace, String className) {
		if (className.indexOf('/') >= 0) {
			throw new IllegalArgumentException("Class names must be provided in dot format: " + className);
		}

		return getNamespaceData(namespace).classNamesInverse.getOrDefault(className, className);
	}

	@Override
	public String mapFieldName(String namespace, String owner, String name, String descriptor) {
		if (owner.indexOf('/') >= 0) {
			throw new IllegalArgumentException("Class names must be provided in dot format: " + owner);
		}

		return getNamespaceData(namespace).fieldNames.getOrDefault(new EntryTriple(owner, name, descriptor), name);
	}

	@Override
	public String mapMethodName(String namespace, String owner, String name, String descriptor) {
		if (owner.indexOf('/') >= 0) {
			throw new IllegalArgumentException("Class names must be provided in dot format: " + owner);
		}

		return getNamespaceData(namespace).methodNames.getOrDefault(new EntryTriple(owner, name, descriptor), name);
	}
}
