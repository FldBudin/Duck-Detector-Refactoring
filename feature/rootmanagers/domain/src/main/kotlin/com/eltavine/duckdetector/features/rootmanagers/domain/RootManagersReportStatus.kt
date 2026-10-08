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

import com.eltavine.duckdetector.core.evidence.DetectorStatus
import com.eltavine.duckdetector.core.evidence.InfoKind

/**
 * A strong hit is a warning; a weak-only hit and a scan that never enumerated the profiles are both
 * informational, because neither is evidence of absence.
 */
fun RootManagersReport.toDetectorStatus(): DetectorStatus = when (stage) {
    RootManagersStage.LOADING -> DetectorStatus.info(InfoKind.SUPPORT)
    RootManagersStage.FAILED -> DetectorStatus.info(InfoKind.ERROR)
    RootManagersStage.READY -> when {
        entries.any { it.confidence != RootManagerConfidence.LOW } -> DetectorStatus.warning()
        entries.isNotEmpty() -> DetectorStatus.info(InfoKind.SUPPORT)
        enumerationState == RootManagersEnumerationState.UNAVAILABLE -> DetectorStatus.info(InfoKind.ERROR)
        enumerationState == RootManagersEnumerationState.UNDECIDABLE -> DetectorStatus.info(InfoKind.SUPPORT)
        else -> DetectorStatus.allClear()
    }
}
