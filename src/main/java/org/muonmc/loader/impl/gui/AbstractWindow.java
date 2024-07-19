/*
 * Copyright 2023 QuiltMC
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

package org.muonmc.loader.impl.gui;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.muonmc.loader.api.LoaderValue;
import org.muonmc.loader.api.LoaderValue.LObject;
import org.muonmc.loader.api.gui.LoaderGuiException;
import org.muonmc.loader.api.gui.MuonLoaderGui;
import org.muonmc.loader.api.gui.MuonLoaderIcon;
import org.muonmc.loader.api.gui.MuonLoaderText;
import org.muonmc.loader.api.gui.MuonLoaderWindow;

abstract class AbstractWindow<R> extends QuiltGuiSyncBase implements MuonLoaderWindow<R> {

	interface WindowChangeListener extends Listener {
		default void onTitleChanged() {}
		default void onIconChanged() {}
	}

	final CompletableFuture<Void> onClosedFuture = new CompletableFuture<>();
	private MuonLoaderText apiTitle = MuonLoaderText.EMPTY;
	String title = "";
	PluginIconImpl icon;
	private R returnValue;

	public AbstractWindow(R defaultReturnValue) {
		super(null);
		this.returnValue = defaultReturnValue;
		icon(MuonLoaderGui.iconQuilt());
	}

	public AbstractWindow(QuiltGuiSyncBase parent, LObject obj) throws IOException {
		super(parent, obj);
		if (parent != null) {
			throw new IOException("Root guis can't have parents!");
		}
		onClosedFuture.thenRun(this::onClosed);
		title = HELPER.expectString(obj, "title");
		icon = readChild(HELPER.expectValue(obj, "icon"), PluginIconImpl.class);
	}

	@Override
	protected void write0(Map<String, LoaderValue> map) {
		map.put("title", lvf().string(title));
		map.put("icon", writeChild(icon));
	}

	// Public API

	@Override
	public MuonLoaderText title() {
		return apiTitle;
	}

	@Override
	public void title(MuonLoaderText title) {
		this.apiTitle = Objects.requireNonNull(title);
		this.title = apiTitle.toString();
		if (shouldSendUpdates()) {
			Map<String, LoaderValue> map = new HashMap<>();
			map.put("title", lvf().string(this.title));
			sendUpdate("set_title", lvf().object(map));
		}
	}

	@Override
	public MuonLoaderIcon icon() {
		return this.icon;
	}

	@Override
	public void icon(MuonLoaderIcon icon) {
		this.icon = PluginIconImpl.fromApi(icon);
		if (shouldSendUpdates()) {
			Map<String, LoaderValue> map = new HashMap<>();
			map.put("icon", writeChild(this.icon));
			sendUpdate("set_icon", lvf().object(map));
		}
	}

	@Override
	public R returnValue() {
		return returnValue;
	}

	@Override
	public void returnValue(R value) {
		this.returnValue = value;
	}

	@Override
	public void addClosedListener(Runnable onCloseListener) {
		onClosedFuture.thenRun(onCloseListener);
	}

	// Internals

	void open() {
		sendSignal("open");
	}

	void onClosed() {
		sendSignal("closed");
	}

	void waitUntilClosed(Supplier<Error> errorChecker) throws LoaderGuiException {
		try {
			while (true) {
				try {
					onClosedFuture.get(1, TimeUnit.SECONDS);
					break;
				} catch (TimeoutException e) {
					if (QuiltForkComms.getCurrentComms() == null) {
						Error error = errorChecker.get();
						if (error == null) {
							throw new LoaderGuiException("Forked communication failure; check the log for details!", e);
						} else {
							LoaderGuiException ex = new LoaderGuiException("Forked communication failure!", error);
							ex.addSuppressed(e);
							throw ex;
						}
					}
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new LoaderGuiException(e);
		} catch (ExecutionException e) {
			throw new LoaderGuiException(e.getCause());
		}
	}

	@Override
	void handleUpdate(String name, LObject data) throws IOException {
		switch (name) {
			case "open": {
				if (!QuiltForkComms.isServer()) {
					throw new IOException("Can only open on the server!");
				}

				openOnServer();
				return;
			}
			case "closed": {
				if (!QuiltForkComms.isClient()) {
					throw new IOException("Can only receive 'closed' on the client!");
				}
				onClosedFuture.complete(null);
				return;
			}
			case "set_title": {
				this.title = HELPER.expectString(data, "title");
				invokeListeners(WindowChangeListener.class, WindowChangeListener::onTitleChanged);
				break;
			}
			case "set_icon": {
				this.icon = readChild(HELPER.expectValue(data, "icon"), PluginIconImpl.class);
				invokeListeners(WindowChangeListener.class, WindowChangeListener::onIconChanged);
				break;
			}
			default: {
				throw new IOException("Unknown update '" + name + "'");
			}
		}
	}

	protected abstract void openOnServer();
}
