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

package org.muonmc.loader.impl.gui;

import java.util.Collections;
import java.util.IllegalFormatException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.muonmc.loader.impl.plugin.gui.I18n;
import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;
import org.muonmc.loader.api.gui.MuonLoaderText;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public final class MuonLoaderTextImpl implements MuonLoaderText {

	private static final Set<String> incorrectlyUntranslatedKeys = Collections.newSetFromMap(new ConcurrentHashMap<>());

	private final String translationKey;
	private final Object[] extra;
	boolean translate;

	public MuonLoaderTextImpl(String key, boolean translate, Object... args) {
		this.translationKey = key;
		this.extra = args;
		this.translate = translate;
		if (!translate && !key.equals(I18n.translate(key))) {
			if (incorrectlyUntranslatedKeys.add(key)) {
				new Throwable("Incorrectly untranslated key '" + key + "'").printStackTrace();
			}
		}
	}

	@Override
	public String toString() {
		try {
			final String format;

			if (translate) {
				format = I18n.translate(translationKey);

				if (format.equals(translationKey)) {
					return error("Missing translation for", null);
				}
			} else {
				format = translationKey;
			}

			return String.format(format, extra);
		} catch (IllegalFormatException e) {
			return error("Bad args for", e);
		}
	}

	private String error(String error, Throwable ex) {
		StringBuilder sb = new StringBuilder();
		sb.append(error);
		sb.append(" '");
		sb.append(translationKey);
		sb.append("'");
		if (ex != null) {
			sb.append(" ");
			sb.append(ex);
		}
		sb.append(" [");
		boolean first = true;
		for (Object o : extra) {
			if (!first) {
				sb.append(",");
			}
			first = false;
			sb.append(' ').append(o);
		}
		sb.append(" ]");

		return sb.toString();
	}
}
