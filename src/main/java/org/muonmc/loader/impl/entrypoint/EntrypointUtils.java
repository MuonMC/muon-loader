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

package org.muonmc.loader.impl.entrypoint;

import org.muonmc.loader.api.ModContainer;
import org.muonmc.loader.api.entrypoint.EntrypointContainer;
import org.muonmc.loader.impl.MuonLoaderImpl;
import org.muonmc.loader.impl.util.ExceptionUtil;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;
import org.muonmc.loader.impl.util.log.Log;
import org.muonmc.loader.impl.util.log.LogCategory;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public final class EntrypointUtils {
	public static <T> void invoke(String name, Class<T> type, Consumer<? super T> invoker) {
		invokeContainer(name, type, container -> invoker.accept(container.getEntrypoint()));
	}

	public static <T> void invoke(String name, Class<T> type, BiConsumer<T, ModContainer> invoker) {
		invokeContainer(name, type, container -> invoker.accept(container.getEntrypoint(), container.getProvider()));
	}

	public static <T> void invokeContainer(String name, Class<T> type, Consumer<EntrypointContainer<T>> invoker) {
		MuonLoaderImpl loader = MuonLoaderImpl.INSTANCE;

		if (!loader.hasEntrypoints(name)) {
			Log.debug(LogCategory.ENTRYPOINT, "No subscribers for entrypoint '" + name + "'");
		} else {
			invoke0(name, type, invoker);
		}
	}

	private static <T> void invoke0(String name, Class<T> type, Consumer<EntrypointContainer<T>> invoker) {
		MuonLoaderImpl loader = MuonLoaderImpl.INSTANCE;
		RuntimeException exception = null;
		Collection<EntrypointContainer<T>> entrypoints = loader.getEntrypointContainers(name, type);

		Log.debug(LogCategory.ENTRYPOINT, "Iterating over entrypoint '%s'", name);

		for (EntrypointContainer<T> container : entrypoints) {
			try {
				invoker.accept(container);
			} catch (Throwable t) {
				exception = ExceptionUtil.gatherExceptions(t,
						exception,
						exc -> new RuntimeException(String.format("Could not execute entrypoint stage '%s' due to errors, provided by '%s'!",
								name, container.getProvider().metadata().id()),
								exc));
			}
		}

		if (exception != null) {
			throw exception;
		}
	}
}
