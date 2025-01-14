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

package org.muonmc.loader.impl.gui;

import org.muonmc.loader.impl.util.MuonLoaderInternal;
import org.muonmc.loader.impl.util.MuonLoaderInternalType;

@MuonLoaderInternal(MuonLoaderInternalType.INTERNAL)
public final class ForkCommNames {
	private ForkCommNames() {}

	public static final String ID_EXCEPTION = "QuiltLoader:Exception";
	public static final String ID_UPLOAD_ICON = "QuiltLoader:UploadIcon";
	public static final String ID_GUI_OBJECT_CREATE = "QuiltLoader:GuiObjectCreate";
	public static final String ID_GUI_OBJECT_UPDATE = "QuiltLoader:GuiObjectUpdate";
	public static final String ID_GUI_OBJECT_DESTROY = "QuiltLoader:GuiObjectDestroy";
}
