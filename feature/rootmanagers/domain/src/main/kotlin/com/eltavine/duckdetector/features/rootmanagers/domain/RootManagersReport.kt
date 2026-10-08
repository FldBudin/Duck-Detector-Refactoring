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

enum class RootManagersStage {
    LOADING,
    READY,
    FAILED,
}

/**
 * Whether the launcher enumeration actually ran and produced a profile list.
 *
 * This is what separates "the enumeration looked and matched nothing" from "the enumeration never
 * ran": only [EVALUATED] lets an empty [RootManagersReport.entries] mean the profiles were searched.
 */
enum class RootManagersEnumerationState {
    /** Profiles were enumerated; an empty entry list means nothing matched, not that nothing was seen. */
    EVALUATED,

    /** The platform refused or failed the enumeration, so absence of entries proves nothing. */
    UNAVAILABLE,

    /** No profile was returned, so there was nothing to enumerate; absence of entries proves nothing. */
    UNDECIDABLE,
}

data class RootManagersReport(
    val stage: RootManagersStage,
    val enumerationState: RootManagersEnumerationState,
    val profileScans: List<RootManagersProfileScan> = emptyList(),
    val entries: List<RootManagerEntry> = emptyList(),
    val issues: List<String> = emptyList(),
) {
    /** Profiles the enumeration actually read; a denied profile is recorded but not counted. */
    val profilesScanned: Int
        get() = profileScans.count { scan -> !scan.denied }

    /** Launcher activities seen across every profile, matched or not, so an empty result is explained. */
    val launcherActivitiesSeen: Int
        get() = profileScans.sumOf { scan -> scan.launcherActivitiesSeen }

    companion object {
        fun loading(): RootManagersReport = RootManagersReport(
            stage = RootManagersStage.LOADING,
            enumerationState = RootManagersEnumerationState.UNDECIDABLE,
        )

        fun failed(message: String): RootManagersReport = RootManagersReport(
            stage = RootManagersStage.FAILED,
            enumerationState = RootManagersEnumerationState.UNAVAILABLE,
            issues = listOf(message),
        )

        fun evaluated(
            entries: List<RootManagerEntry>,
            profileScans: List<RootManagersProfileScan>,
            issues: List<String> = emptyList(),
        ): RootManagersReport = RootManagersReport(
            stage = RootManagersStage.READY,
            enumerationState = RootManagersEnumerationState.EVALUATED,
            profileScans = profileScans,
            entries = entries,
            issues = issues,
        )

        fun unavailable(reason: String): RootManagersReport = RootManagersReport(
            stage = RootManagersStage.READY,
            enumerationState = RootManagersEnumerationState.UNAVAILABLE,
            issues = listOf(reason),
        )

        fun undecidable(reason: String): RootManagersReport = RootManagersReport(
            stage = RootManagersStage.READY,
            enumerationState = RootManagersEnumerationState.UNDECIDABLE,
            issues = listOf(reason),
        )
    }
}
