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

package com.eltavine.duckdetector.features.rootmanagers.domain

/**
 * What the enumeration saw in one profile.
 *
 * It keeps the raw launcher-visible records, not just a count, so the card can show that a profile
 * was searched and copy the exact activities it held: a clean result is then explained instead of
 * silently reading as one, and a manager that was seen but not matched stays visible to a maintainer.
 */
data class RootManagersProfileScan(
    val profileUserId: Int,
    val records: List<LauncherActivityRecord> = emptyList(),
    val denied: Boolean = false,
) {
    /** Launcher activities seen in this profile. */
    val launcherActivitiesSeen: Int
        get() = records.size
}
