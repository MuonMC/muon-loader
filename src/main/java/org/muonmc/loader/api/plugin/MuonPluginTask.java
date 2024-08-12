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

package org.muonmc.loader.api.plugin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.muonmc.loader.impl.plugin.MuonPluginTaskImpl;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

/** Quilt plugin version of a {@link Future}.
 * <p>
 * All implementations use {@link CompletableFuture} internally, since we only need to restrict the API surface of this,
 * rather than have a complete implementation of concurrent code. */
@MuonLoaderInternal(MuonLoaderInternalType.PLUGIN_API)
public interface MuonPluginTask<V> {

	/** @return True if this task has already finished - either successfully or exceptionally. */
	boolean isDone();

	/** @return The exception, if this task completed exceptionally, or null if it either hasn't completed or completed
	 *         successfully. */
	Throwable getException();

	/** @return The result, if the task completed successfully.
	 * @throws ExecutionException if the task completed unsuccessfully.
	 * @throws IllegalStateException if the task has not been completed yet. */
	V getResult() throws ExecutionException;

	/** @return A new {@link MuonPluginTask} that has already been completed successfully, with the given result. */
	public static <V> MuonPluginTask<V> createFinished(V result) {
		return MuonPluginTaskImpl.createFinished(result);
	}

	/** @return A new {@link MuonPluginTask} that has already been completed unsuccessfuly, with the given
	 *         exception. */
	public static <V> MuonPluginTask<V> createFailed(Throwable cause) {
		return MuonPluginTaskImpl.createFailed(cause);
	}
}
