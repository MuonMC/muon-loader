/*
 * Copyright 2024 QuiltMC
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

package org.muonmc.loader.api.game;

import org.muonmc.loader.api.horoscope.ExperimentalApi;
import org.muonmc.loader.api.horoscope.muon.MuonFeatures;

/**
 * A customizable type that describes an analogous logical side. This is especially useful for non-Minecraft environments.
 *
 * @see Id
 */
@ExperimentalApi(MuonFeatures.LOGICAL_SIDE)
public final class LogicalSide {
	/**
	 * A client application in a server-client or peer-peer relationship. This does not include clients that have all the dedicated server code. Typically, the
	 * client has a user interface and cannot run headless.
	 */
	public static final LogicalSide CLIENT = new LogicalSide(Id.CLIENT);
	/**
	 * A dedicated server application in a server-client relationship. This includes server-only applications such as IRC or Discord bots.
	 */
	public static final LogicalSide DEDICATED_SERVER = new LogicalSide(Id.DEDICATED_SERVER);
	/**
	 * A universal application in a client, server-client, or peer-peer relationship. This includes clients that have all the dedicated server code. This also
	 * includes applications which do not have a moddable server.
	 */
	public static final LogicalSide UNIVERSAL = new LogicalSide(Id.UNIVERSAL);

	private final String id;

	public LogicalSide(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	/**
	 * A utility class used for comparing {@link LogicalSide}s in enums.
	 */
	public static final class Id {
		public static final String CLIENT = "client";
		public static final String DEDICATED_SERVER = "dedicated_server";
		public static final String UNIVERSAL = "universal";

		private Id() {}
	}
}
