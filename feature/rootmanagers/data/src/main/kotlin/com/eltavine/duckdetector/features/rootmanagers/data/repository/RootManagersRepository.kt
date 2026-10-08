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

package com.eltavine.duckdetector.features.rootmanagers.data.repository

import android.content.Context
import com.eltavine.duckdetector.core.detector.DetectorScanner
import com.eltavine.duckdetector.core.evidence.FailureName
import com.eltavine.duckdetector.features.rootmanagers.data.probes.LauncherEnumerationResult
import com.eltavine.duckdetector.features.rootmanagers.data.probes.LauncherProfileEnumerator
import com.eltavine.duckdetector.features.rootmanagers.domain.RootManagerEntryRules
import com.eltavine.duckdetector.features.rootmanagers.domain.RootManagersReport
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Collects Root Managers evidence off the main thread; it enumerates and maps, never judges or words. */
class RootManagersRepository(private val context: Context) : DetectorScanner<RootManagersReport> {

    override suspend fun scan(): RootManagersReport = withContext(Dispatchers.IO) {
        try {
            collect()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            RootManagersReport.failed(FailureName.describe(failure))
        }
    }

    private fun collect(): RootManagersReport =
        when (val result = LauncherProfileEnumerator(context.applicationContext).enumerate()) {
            is LauncherEnumerationResult.Enumerated -> RootManagersReport.evaluated(
                entries = RootManagerEntryRules.entries(result.profileScans.flatMap { scan -> scan.records }),
                profileScans = result.profileScans,
                issues = result.issues,
            )

            is LauncherEnumerationResult.Unavailable -> RootManagersReport.unavailable(result.reason)

            is LauncherEnumerationResult.Undecidable -> RootManagersReport.undecidable(result.reason)
        }
}
