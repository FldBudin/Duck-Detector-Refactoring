/*
 * Copyright 2026 Duck Apps Contributor
 * If you have any questions, suggestions, or other inquiries, please email Eltavine <me@eltavine.com>.
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

package com.eltavine.duckdetector.core.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

/**
 * Puts [text] on the clipboard under [label], and shows [confirmation] as a toast.
 *
 * The toast shows on every Android version: the hidden double-tap copy on a detector card has no
 * other visible sign that it fired. From Android 13 the system also shows its own clipboard
 * confirmation, so the two appear together.
 *
 * @return false when the device offers no clipboard.
 */
public fun copyPlainTextToClipboard(
    context: Context,
    label: String,
    text: String,
    confirmation: String,
): Boolean {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return false
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, confirmation, Toast.LENGTH_SHORT).show()
    return true
}
