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

package com.eltavine.duckdetector.features.rootmanagers.data.probes

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.os.UserHandle
import com.eltavine.duckdetector.core.evidence.FailureName
import com.eltavine.duckdetector.features.rootmanagers.domain.LauncherActivityRecord
import com.eltavine.duckdetector.features.rootmanagers.domain.RootManagersProfileScan
import org.lsposed.hiddenapibypass.HiddenApiBypass

sealed interface LauncherEnumerationResult {

    data class Enumerated(
        val profileScans: List<RootManagersProfileScan>,
        val issues: List<String>,
    ) : LauncherEnumerationResult

    data class Unavailable(val reason: String) : LauncherEnumerationResult

    data class Undecidable(val reason: String) : LauncherEnumerationResult
}

/**
 * Enumerates every launcher activity the caller can see, in each profile `LauncherApps` exposes.
 *
 * For a normal caller `getProfiles()` returns `UserManager.getUserProfiles()`, so the owner (user 0)
 * is covered without a separate query; a caller inside a managed profile sees only itself. An empty
 * list is a real "nothing matched" answer, since `getActivityList` never returns null.
 */
class LauncherProfileEnumerator(private val context: Context) {

    fun enumerate(): LauncherEnumerationResult {
        val launcherApps = context.getSystemService(LauncherApps::class.java)
            ?: return LauncherEnumerationResult.Unavailable("This device exposes no LauncherApps service.")

        val profiles = try {
            launcherApps.profiles
        } catch (denied: SecurityException) {
            return LauncherEnumerationResult.Unavailable(
                "The platform denied the profile list: ${FailureName.messageOrName(denied)}",
            )
        }
        if (profiles.isEmpty()) {
            return LauncherEnumerationResult.Undecidable("The platform returned no accessible profiles.")
        }

        val profileScans = mutableListOf<RootManagersProfileScan>()
        val issues = mutableListOf<String>()

        // ApplicationInfo.zygotePreloadName is @hide and absent from the SDK, so install the LSPosed
        // hidden-api exemption before reading it reflectively, the way the TEE keystore probes do. A
        // failed install is recorded rather than swallowed, so the missing anchor is not silent.
        if (runCatching { HiddenApiBypass.addHiddenApiExemptions("") }.isFailure) {
            issues += "The hidden-api exemption was unavailable, so the zygote preload name was not read."
        }

        profiles.forEach { user ->
            // A denied profile must not discard the other profiles' results; record it and skip this one.
            val profileUserId = user.profileId()
            val activities = try {
                launcherApps.getActivityList(null, user)
            } catch (denied: SecurityException) {
                issues += "Profile $profileUserId was denied: ${FailureName.messageOrName(denied)}"
                profileScans += RootManagersProfileScan(profileUserId, denied = true)
                return@forEach
            }
            profileScans += RootManagersProfileScan(
                profileUserId = profileUserId,
                records = activities.map { activity -> activity.toRecord(profileUserId) },
            )
        }
        if (profileScans.none { scan -> !scan.denied }) {
            return LauncherEnumerationResult.Unavailable("No accessible profile could be enumerated.")
        }
        return LauncherEnumerationResult.Enumerated(profileScans, issues)
    }
}

private fun LauncherActivityInfo.toRecord(profileUserId: Int): LauncherActivityRecord {
    val applicationInfo = applicationInfo
    val component = componentName
    return LauncherActivityRecord(
        profileUserId = profileUserId,
        packageName = component.packageName,
        componentClassName = component.className,
        applicationClassName = applicationInfo?.className,
        label = label?.toString(),
        zygotePreloadName = applicationInfo?.hiddenZygotePreloadName(),
        sourceDir = applicationInfo?.sourceDir,
        processName = applicationInfo?.processName,
        uid = applicationInfo?.uid,
        firstInstallTime = firstInstallTime,
    )
}

// UserHandle.getIdentifier() is @hide/@SystemApi and invisible to a normal app, while the public
// hashCode() returns the same user id (AOSP UserHandle.hashCode() == mHandle); identify the profile
// with it. It only feeds dedupe and display, never the matching.
private fun UserHandle.profileId(): Int = hashCode()

private const val ZYGOTE_PRELOAD_FIELD = "zygotePreloadName"

/**
 * Reads the hidden `ApplicationInfo.zygotePreloadName` field, which is absent from the SDK, through
 * the exemption installed in [LauncherProfileEnumerator.enumerate]. A failed read leaves the zygote
 * anchor unmatched rather than failing the enumeration.
 */
private fun ApplicationInfo.hiddenZygotePreloadName(): String? = runCatching {
    val field = ApplicationInfo::class.java.getDeclaredField(ZYGOTE_PRELOAD_FIELD)
    field.isAccessible = true
    field.get(this) as? String
}.getOrNull()
