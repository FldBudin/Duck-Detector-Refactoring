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
import org.junit.Assert.assertEquals
import org.junit.Test

class RootManagersReportStatusTest {

    @Test
    fun `loading and failed scans are informational`() {
        assertEquals(DetectorStatus.info(InfoKind.SUPPORT), RootManagersReport.loading().toDetectorStatus())
        assertEquals(DetectorStatus.info(InfoKind.ERROR), RootManagersReport.failed("failure").toDetectorStatus())
    }

    @Test
    fun `a scan that never enumerated the profiles is not clean`() {
        assertEquals(DetectorStatus.info(InfoKind.SUPPORT), RootManagersReport.undecidable("no profiles").toDetectorStatus())
        assertEquals(DetectorStatus.info(InfoKind.ERROR), RootManagersReport.unavailable("denied").toDetectorStatus())
    }

    @Test
    fun `an evaluated scan without entries is clean`() {
        val report = RootManagersReport.evaluated(entries = emptyList(), profileScans = twoProfiles())

        assertEquals(DetectorStatus.allClear(), report.toDetectorStatus())
    }

    @Test
    fun `a strong hit is a warning`() {
        val report = RootManagersReport.evaluated(
            entries = listOf(entry(RootManagerConfidence.HIGH)),
            profileScans = twoProfiles(),
        )

        assertEquals(DetectorStatus.warning(), report.toDetectorStatus())
    }

    @Test
    fun `a weak-only hit stays informational`() {
        val report = RootManagersReport.evaluated(
            entries = listOf(entry(RootManagerConfidence.LOW)),
            profileScans = twoProfiles(),
        )

        assertEquals(DetectorStatus.info(InfoKind.SUPPORT), report.toDetectorStatus())
    }

    private fun twoProfiles() = listOf(
        RootManagersProfileScan(profileUserId = 0),
        RootManagersProfileScan(profileUserId = 10),
    )

    private fun entry(confidence: RootManagerConfidence) = RootManagerEntry(
        family = RootManagerFamily.KERNEL_SU,
        packageName = "me.weishu.kernelsu",
        displayName = "KernelSU",
        profileUserId = 0,
        componentClassName = "me.weishu.kernelsu.ui.MainActivity",
        applicationClassName = "me.weishu.kernelsu.KernelSUApplication",
        sourceDir = null,
        uid = null,
        firstInstallTime = null,
        anchors = setOf(RootManagerAnchor.PACKAGE_NAME, RootManagerAnchor.APPLICATION_CLASS),
        confidence = confidence,
    )
}
