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
 * The launcher-visible identity of every mainstream root manager family.
 *
 * The anchors come from the manifest of each family's released APKs (see `EVIDENCE.md`), not from
 * source: a package name is only what the build shipped with, while the application class and
 * zygote preload name reveal the family even after a rename.
 */
object RootManagerCatalog {

    val signatures: List<RootManagerSignature> = listOf(
        RootManagerSignature(
            family = RootManagerFamily.KERNEL_SU,
            defaultPackageNames = setOf("me.weishu.kernelsu"),
            namespacePrefixes = setOf("me.weishu.kernelsu"),
            applicationClassSuffixes = setOf(".KernelSUApplication"),
            labelPrefixes = setOf("KernelSU"),
            // The official rename changes only applicationId; the namespace survives byte for byte, so the zygote name keeps it.
            zygotePreloadNameSuffixes = setOf(".magica.AppZygotePreload"),
            zygotePreloadNamespaces = setOf("me.weishu.kernelsu"),
            launcherClassSuffixes = setOf(".ui.MainActivity"),
        ),
        RootManagerSignature(
            family = RootManagerFamily.KERNEL_SU_NEXT,
            defaultPackageNames = setOf("com.rifsxd.ksunext"),
            applicationClassSuffixes = setOf(".KernelSUApplication"),
            labelPrefixes = setOf("KernelSU-Next"),
            launcherClassSuffixes = setOf(".ui.MainActivity"),
        ),
        RootManagerSignature(
            family = RootManagerFamily.SUKI_SU,
            defaultPackageNames = setOf("com.sukisu.ultra"),
            applicationClassSuffixes = setOf(".KernelSUApplication"),
            labelPrefixes = setOf("SukiSU"),
            launcherClassSuffixes = setOf(".ui.MainActivity"),
        ),
        RootManagerSignature(
            family = RootManagerFamily.RE_SUKI_SU,
            defaultPackageNames = setOf("com.resukisu.resukisu"),
            applicationClassSuffixes = setOf(".KernelSUApplication"),
            labelPrefixes = setOf("ReSukiSU"),
            zygotePreloadNameSuffixes = setOf(".magica.AppZygotePreload"),
            zygotePreloadNamespaces = setOf("com.resukisu.resukisu"),
            launcherClassSuffixes = setOf(".ui.MainActivity"),
        ),
        RootManagerSignature(
            family = RootManagerFamily.APATCH,
            defaultPackageNames = setOf("me.bmax.apatch"),
            applicationClassSuffixes = setOf(".APApplication"),
            labelPrefixes = setOf("APatch"),
            zygotePreloadNameSuffixes = setOf(".magica.AppZygotePreload"),
            zygotePreloadNamespaces = setOf("me.bmax.apatch"),
            launcherClassSuffixes = setOf(".ui.MainActivity"),
        ),
        RootManagerSignature(
            family = RootManagerFamily.SK_ROOT,
            defaultPackageNames = setOf("com.linux.permissionmanager"),
            // The shipped APK declares no application class, so the class anchors give no strong signal.
            labelPrefixes = setOf("PermissionManager"),
            // Seen in a 4.6.2 manifest but not in the decoded build, so this anchor is unverified.
            zygotePreloadNameSuffixes = setOf(".helper.AppZygotePreload"),
            zygotePreloadNamespaces = setOf("com.linux.permissionmanager"),
            launcherClassSuffixes = setOf(".MainActivity"),
        ),
        RootManagerSignature(
            family = RootManagerFamily.MAGISK,
            defaultPackageNames = setOf("com.topjohnwu.magisk"),
            // No `.core.App` suffix: it is too generic and would promote unrelated apps to a strong anchor.
            labelPrefixes = setOf("Magisk"),
            launcherClassSuffixes = setOf(".ui.MainActivity"),
        ),
    )
}
