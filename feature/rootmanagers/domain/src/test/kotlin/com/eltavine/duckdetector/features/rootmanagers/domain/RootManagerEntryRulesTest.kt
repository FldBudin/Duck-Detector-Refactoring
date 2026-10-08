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
import org.junit.Assert.assertNull
import org.junit.Test

class RootManagerEntryRulesTest {

    @Test
    fun `a KernelSU install matches every field as a high-confidence entry`() {
        val entry = RootManagerEntryRules.match(
            record(
                packageName = "me.weishu.kernelsu",
                applicationClassName = "me.weishu.kernelsu.KernelSUApplication",
                componentClassName = "me.weishu.kernelsu.ui.MainActivity",
                label = "KernelSU",
                zygotePreloadName = "me.weishu.kernelsu.magica.AppZygotePreload",
            ),
        )

        assertEquals(RootManagerFamily.KERNEL_SU, entry?.family)
        assertEquals(RootManagerConfidence.HIGH, entry?.confidence)
        assertEquals(
            setOf(
                RootManagerAnchor.PACKAGE_NAME,
                RootManagerAnchor.APPLICATION_CLASS,
                RootManagerAnchor.ZYGOTE_PRELOAD,
                RootManagerAnchor.LABEL,
                RootManagerAnchor.LAUNCHER_CLASS,
            ),
            entry?.anchors,
        )
    }

    @Test
    fun `a renamed KernelSU-Next is still matched through its surviving application class`() {
        val entry = RootManagerEntryRules.match(
            record(
                packageName = "yhaxhr.birgvn.bmwbne",
                applicationClassName = "yhaxhr.birgvn.bmwbne.KernelSUApplication",
                componentClassName = "yhaxhr.birgvn.bmwbne.ui.MainActivity",
                label = "KernelSU-Next",
            ),
        )

        // The shared `.KernelSUApplication` matches several KSU-family signatures; the record must land on the most specific one.
        assertEquals(RootManagerFamily.KERNEL_SU_NEXT, entry?.family)
        assertEquals(RootManagerConfidence.HIGH, entry?.confidence)
        assertEquals(
            setOf(
                RootManagerAnchor.APPLICATION_CLASS,
                RootManagerAnchor.LABEL,
                RootManagerAnchor.LAUNCHER_CLASS,
            ),
            entry?.anchors,
        )
    }

    @Test
    fun `a SukiSU package is attributed to SukiSU rather than the shared application class`() {
        val entry = RootManagerEntryRules.match(
            record(
                packageName = "com.sukisu.ultra",
                applicationClassName = "com.sukisu.ultra.KernelSUApplication",
                label = "SukiSU Ultra",
            ),
        )

        assertEquals(RootManagerFamily.SUKI_SU, entry?.family)
        assertEquals(
            setOf(
                RootManagerAnchor.PACKAGE_NAME,
                RootManagerAnchor.APPLICATION_CLASS,
                RootManagerAnchor.LABEL,
            ),
            entry?.anchors,
        )
    }

    @Test
    fun `a single weak field does not produce an entry`() {
        val entry = RootManagerEntryRules.match(record(packageName = "com.example.app", label = "KernelSU"))

        assertNull(entry)
    }

    @Test
    fun `two weak fields produce a low-confidence entry`() {
        val entry = RootManagerEntryRules.match(
            record(
                packageName = "com.example.app",
                componentClassName = "com.example.app.MainActivity",
                label = "PermissionManager",
            ),
        )

        assertEquals(RootManagerFamily.SK_ROOT, entry?.family)
        assertEquals(RootManagerConfidence.LOW, entry?.confidence)
        assertEquals(
            setOf(RootManagerAnchor.LABEL, RootManagerAnchor.LAUNCHER_CLASS),
            entry?.anchors,
        )
    }

    @Test
    fun `the same app in two profiles yields two entries`() {
        val entries = RootManagerEntryRules.entries(
            listOf(
                record(packageName = "me.weishu.kernelsu", profileUserId = 0),
                record(packageName = "me.weishu.kernelsu", profileUserId = 10),
            ),
        )

        assertEquals(listOf(0, 10), entries.map { it.profileUserId })
    }

    @Test
    fun `several launcher activities of one app collapse to the richest entry`() {
        val entries = RootManagerEntryRules.entries(
            listOf(
                record(packageName = "me.weishu.kernelsu"),
                record(
                    packageName = "me.weishu.kernelsu",
                    applicationClassName = "me.weishu.kernelsu.KernelSUApplication",
                    componentClassName = "me.weishu.kernelsu.ui.MainActivity",
                    label = "KernelSU",
                ),
            ),
        )

        assertEquals(1, entries.size)
        assertEquals(4, entries.single().anchors.size)
    }

    @Test
    fun `an empty enumeration produces no entries`() {
        assertEquals(emptyList<RootManagerEntry>(), RootManagerEntryRules.entries(emptyList()))
    }

    private fun record(
        packageName: String,
        profileUserId: Int = 0,
        applicationClassName: String? = null,
        componentClassName: String? = null,
        label: String? = null,
        zygotePreloadName: String? = null,
    ): LauncherActivityRecord = LauncherActivityRecord(
        profileUserId = profileUserId,
        packageName = packageName,
        componentClassName = componentClassName,
        applicationClassName = applicationClassName,
        label = label,
        zygotePreloadName = zygotePreloadName,
        sourceDir = "/data/app/$packageName/base.apk",
        processName = packageName,
        uid = 10_123,
        firstInstallTime = 1L,
    )
}
