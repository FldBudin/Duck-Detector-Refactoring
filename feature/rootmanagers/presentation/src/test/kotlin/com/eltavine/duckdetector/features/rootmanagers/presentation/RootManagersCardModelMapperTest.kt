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

package com.eltavine.duckdetector.features.rootmanagers.presentation

import com.eltavine.duckdetector.core.evidence.DetectorStatus
import com.eltavine.duckdetector.core.evidence.InfoKind
import com.eltavine.duckdetector.core.report.ReportBlock
import com.eltavine.duckdetector.features.rootmanagers.domain.LauncherActivityRecord
import com.eltavine.duckdetector.features.rootmanagers.domain.RootManagerAnchor
import com.eltavine.duckdetector.features.rootmanagers.domain.RootManagerConfidence
import com.eltavine.duckdetector.features.rootmanagers.domain.RootManagerEntry
import com.eltavine.duckdetector.features.rootmanagers.domain.RootManagerFamily
import com.eltavine.duckdetector.features.rootmanagers.domain.RootManagersProfileScan
import com.eltavine.duckdetector.features.rootmanagers.domain.RootManagersReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RootManagersCardModelMapperTest {

    private val mapper = RootManagersCardModelMapper()

    @Test
    fun `a scan that is still running reads as scanning`() {
        val model = mapper.map(RootManagersReport.loading())

        assertEquals(DetectorStatus.info(InfoKind.SUPPORT), model.status)
        assertEquals("Scanning", model.verdict)
        assertEquals(0, model.entryRows.size)
    }

    @Test
    fun `an unavailable enumeration is informational, not clean`() {
        val model = mapper.map(RootManagersReport.unavailable("denied"))

        assertEquals(DetectorStatus.info(InfoKind.ERROR), model.status)
        assertEquals("Launcher enumeration unavailable", model.verdict)
    }

    @Test
    fun `a strong match is a warning with one row and one exported row`() {
        val model = mapper.map(report(listOf(entry(RootManagerConfidence.HIGH))))
        val export = model.toDetectorReport()

        assertEquals(DetectorStatus.warning(), model.status)
        assertEquals(1, model.entryRows.size)
        assertEquals(model.verdict, export.verdict)
        assertEquals(model.status.severity, export.severity)
        val rows = export.blocks
            .filterIsInstance<ReportBlock.Rows>()
            .single { it.title == "Matched root managers" }
            .rows
        assertEquals(1, rows.size)
    }

    @Test
    fun `a weak-only match stays informational`() {
        val model = mapper.map(report(listOf(entry(RootManagerConfidence.LOW))))

        assertEquals(DetectorStatus.info(InfoKind.SUPPORT), model.status)
        assertEquals("Weak root manager match", model.verdict)
    }

    @Test
    fun `an evaluated scan without matches still shows the profiles it searched`() {
        val model = mapper.map(report(emptyList()))

        assertEquals(DetectorStatus.allClear(), model.status)
        assertEquals("No root manager apps", model.verdict)
        assertEquals(2, model.profileRows.size)
        assertEquals("2 of 2", model.scanRows.single { it.label == "Profiles searched" }.value)
        assertEquals("52", model.scanRows.single { it.label == "Launcher activities seen" }.value)
    }

    @Test
    fun `every node row carries the detail a double tap copies`() {
        val model = mapper.map(report(listOf(entry(RootManagerConfidence.HIGH))))

        assertTrue(model.entryRows.all { row -> row.hiddenCopyText != null })
        assertTrue(model.profileRows.all { row -> row.hiddenCopyText != null })
        assertTrue(model.scanRows.all { row -> row.hiddenCopyText != null })
        assertTrue(model.entryRows.single().hiddenCopyText.orEmpty().contains("me.weishu.kernelsu"))
        assertTrue(model.scanRows.single().hiddenCopyText.orEmpty().contains("Launcher activities seen: 52"))
        // The profile node copies the activities it actually saw, not only a count.
        assertTrue(model.profileRows.first().hiddenCopyText.orEmpty().contains("com.example.0.app0"))
    }

    @Test
    fun `a denied profile row shows denied and copies the denial`() {
        val model = mapper.map(
            RootManagersReport.evaluated(
                entries = emptyList(),
                profileScans = listOf(
                    RootManagersProfileScan(profileUserId = 0, records = records(0, 40)),
                    RootManagersProfileScan(profileUserId = 10, denied = true),
                ),
            ),
        )

        val denied = model.profileRows.single { row -> row.label == "user 10" }
        assertEquals("Denied", denied.value)
        assertEquals(DetectorStatus.info(InfoKind.ERROR), denied.status)
        assertTrue(denied.hiddenCopyText.orEmpty().contains("Denied: true"))
        assertEquals("1 of 2", model.scanRows.single { it.label == "Profiles searched" }.value)
    }

    private fun report(entries: List<RootManagerEntry>) = RootManagersReport.evaluated(
        entries = entries,
        profileScans = listOf(
            RootManagersProfileScan(profileUserId = 0, records = records(0, 40)),
            RootManagersProfileScan(profileUserId = 10, records = records(10, 12)),
        ),
    )

    private fun records(profileUserId: Int, count: Int) = List(count) { index ->
        LauncherActivityRecord(
            profileUserId = profileUserId,
            packageName = "com.example.$profileUserId.app$index",
            componentClassName = "com.example.$profileUserId.app$index.MainActivity",
            applicationClassName = null,
            label = null,
            zygotePreloadName = null,
            sourceDir = null,
            processName = null,
            uid = null,
            firstInstallTime = null,
        )
    }

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
