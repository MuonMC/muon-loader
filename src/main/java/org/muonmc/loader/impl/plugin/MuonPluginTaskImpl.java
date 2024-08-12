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

package org.muonmc.loader.impl.plugin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.muonmc.loader.api.plugin.MuonPluginTask;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public final class MuonPluginTaskImpl<V> implements MuonPluginTask<V> {

	final CompletableFuture<V> future;

	MuonPluginTaskImpl() {
		this.future = new CompletableFuture<>();
	}

	/** @return A new {@link MuonPluginTask} that has already been completed successfully, with the given result. */
	public static <V> MuonPluginTask<V> createFinished(V result) {
		MuonPluginTaskImpl<V> task = new MuonPluginTaskImpl<>();
		task.future.complete(result);
		return task;
	}

	/** @return A new {@link MuonPluginTask} that has already been completed unsuccessfully, with the given
	 *         exception. */
	public static <V> MuonPluginTask<V> createFailed(Throwable cause) {
		MuonPluginTaskImpl<V> task = new MuonPluginTaskImpl<>();
		task.future.completeExceptionally(cause);
		return task;
	}

	@Override
	public boolean isDone() {
		return future.isDone();
	}

	@Override
	public Throwable getException() {
		if (!future.isDone()) {
			return null;
		}

		try {
			future.get(0, TimeUnit.NANOSECONDS);
			return null;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return new ExecutionException(e);
		} catch (ExecutionException e) {
			return e;
		} catch (TimeoutException e) {
			throw new IllegalStateException(
				"Apparently the CompletableFuture was done, but it threw a TimeoutException?", e
			);
		}
	}

	@Override
	public V getResult() throws ExecutionException {
		if (!future.isDone()) {
			throw new IllegalStateException("Task not complete yet!");
		}

		try {
			return future.get(0, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ExecutionException(e);
		} catch (ExecutionException e) {
			throw e;
		} catch (TimeoutException e) {
			throw new IllegalStateException(
				"Apparently the CompletableFuture was done, but it threw a TimeoutException?", e
			);
		}
	}
}
