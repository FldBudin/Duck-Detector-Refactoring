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

import org.junit.Assert.assertEquals
import org.junit.Test

class RootManagersReportTest {

    @Test
    fun `a denied profile is recorded but not counted as searched`() {
        val report = RootManagersReport.evaluated(
            entries = emptyList(),
            profileScans = listOf(
                RootManagersProfileScan(profileUserId = 0, records = records(3)),
                RootManagersProfileScan(profileUserId = 10, denied = true),
            ),
        )

        assertEquals(2, report.profileScans.size)
        assertEquals(1, report.profilesScanned)
        assertEquals(3, report.launcherActivitiesSeen)
    }

    @Test
    fun `an enumeration that never ran reports no scanned profiles`() {
        assertEquals(0, RootManagersReport.undecidable("no profiles").profilesScanned)
        assertEquals(0, RootManagersReport.undecidable("no profiles").launcherActivitiesSeen)
        assertEquals(0, RootManagersReport.unavailable("denied").profilesScanned)
    }

    private fun records(count: Int) = List(count) { index ->
        LauncherActivityRecord(
            profileUserId = 0,
            packageName = "com.example.app$index",
            componentClassName = null,
            applicationClassName = null,
            label = null,
            zygotePreloadName = null,
            sourceDir = null,
            processName = null,
            uid = null,
            firstInstallTime = null,
        )
    }
}
