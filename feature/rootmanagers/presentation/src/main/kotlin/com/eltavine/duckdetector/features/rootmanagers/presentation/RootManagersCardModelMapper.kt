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
import com.eltavine.duckdetector.features.rootmanagers.domain.LauncherActivityRecord
import com.eltavine.duckdetector.features.rootmanagers.domain.RootManagerConfidence
import com.eltavine.duckdetector.features.rootmanagers.domain.RootManagerEntry
import com.eltavine.duckdetector.features.rootmanagers.domain.RootManagersEnumerationState
import com.eltavine.duckdetector.features.rootmanagers.domain.RootManagersProfileScan
import com.eltavine.duckdetector.features.rootmanagers.domain.RootManagersReport
import com.eltavine.duckdetector.features.rootmanagers.domain.RootManagersStage
import com.eltavine.duckdetector.features.rootmanagers.domain.toDetectorStatus
import com.eltavine.duckdetector.features.rootmanagers.presentation.model.RootManagersCardModel
import com.eltavine.duckdetector.features.rootmanagers.presentation.model.RootManagersDetailRowModel

class RootManagersCardModelMapper {

    fun map(report: RootManagersReport): RootManagersCardModel = RootManagersCardModel(
        title = TITLE,
        subtitle = subtitle(report),
        status = report.toDetectorStatus(),
        verdict = verdict(report),
        summary = summary(report),
        entryRows = report.entries.map(::entryRow),
        profileRows = report.profileScans.map(::profileRow),
        scanRows = scanRows(report),
    )

    private fun subtitle(report: RootManagersReport): String = when (report.stage) {
        RootManagersStage.LOADING -> SUBTITLE
        RootManagersStage.FAILED -> SUBTITLE
        RootManagersStage.READY ->
            "${report.entries.size} matched · ${report.launcherActivitiesSeen} launcher activities · " +
                "${report.profilesScanned} profile(s) searched"
    }

    private fun verdict(report: RootManagersReport): String = when (report.stage) {
        RootManagersStage.LOADING -> "Scanning"
        RootManagersStage.FAILED -> "Scan failed"
        RootManagersStage.READY -> when {
            report.entries.any { entry -> entry.confidence != RootManagerConfidence.LOW } ->
                "Root manager apps surfaced"

            report.entries.isNotEmpty() -> "Weak root manager match"
            report.enumerationState == RootManagersEnumerationState.UNAVAILABLE ->
                "Launcher enumeration unavailable"

            report.enumerationState == RootManagersEnumerationState.UNDECIDABLE ->
                "Profiles not enumerated"

            else -> "No root manager apps"
        }
    }

    private fun summary(report: RootManagersReport): String = when (report.stage) {
        RootManagersStage.LOADING ->
            "Enumerating launcher-visible apps across the accessible profiles."

        RootManagersStage.FAILED ->
            report.issues.firstOrNull() ?: "The scan failed before the enumeration ran."

        RootManagersStage.READY -> when {
            report.entries.any { entry -> entry.confidence != RootManagerConfidence.LOW } ->
                "The launcher visibility enumeration matched ${report.entries.size} app(s) across " +
                    "${report.profilesScanned} profile(s). A match is evidence an app is present, " +
                    "not proof that the device is rooted."

            report.entries.isNotEmpty() ->
                "Only weak anchors matched, so these apps are informational, never warnings."

            report.enumerationState == RootManagersEnumerationState.UNAVAILABLE ->
                report.issues.firstOrNull()
                    ?: "The launcher enumeration was unavailable, so the absence of matches says nothing."

            report.enumerationState == RootManagersEnumerationState.UNDECIDABLE ->
                "No profile could be enumerated, so the absence of matches says nothing."

            else ->
                "The enumeration searched ${report.profilesScanned} profile(s), saw " +
                    "${report.launcherActivitiesSeen} launcher activities, and matched no root manager app."
        }
    }

    private fun entryRow(entry: RootManagerEntry): RootManagersDetailRowModel {
        val anchors = anchorList(entry)
        val detail = buildString {
            append("via launcher visibility @user ${entry.profileUserId}")
            append(" · anchors: $anchors")
            entry.applicationClassName?.let { applicationClass -> append(" · application: $applicationClass") }
        }
        return RootManagersDetailRowModel(
            label = "${entry.displayName} (${entry.packageName})",
            value = entry.confidence.displayName,
            status = statusFor(entry.confidence),
            detail = detail,
            hiddenCopyText = entryDiagnostics(entry),
        )
    }

    private fun profileRow(scan: RootManagersProfileScan): RootManagersDetailRowModel {
        val value = if (scan.denied) "Denied" else "${scan.launcherActivitiesSeen} activities"
        val status = if (scan.denied) {
            DetectorStatus.info(InfoKind.ERROR)
        } else {
            DetectorStatus.allClear()
        }
        return RootManagersDetailRowModel(
            label = "user ${scan.profileUserId}",
            value = value,
            status = status,
            hiddenCopyText = profileDiagnostics(scan),
        )
    }

    private fun profileDiagnostics(scan: RootManagersProfileScan): String = buildString {
        appendLine("Root Managers profile scan")
        appendLine("Profile user id: ${scan.profileUserId}")
        appendLine("Denied: ${scan.denied}")
        appendLine("Launcher activities seen: ${scan.launcherActivitiesSeen}")
        if (scan.records.isNotEmpty()) {
            appendLine("Seen launcher activities:")
            scan.records.forEach { record -> appendLine("  ${recordLine(record)}") }
        }
    }

    private fun recordLine(record: LauncherActivityRecord): String = buildString {
        append("package=${record.packageName}")
        append(" component=${record.componentClassName ?: "none"}")
        append(" application=${record.applicationClassName ?: "none"}")
        append(" label=${record.label ?: "none"}")
        append(" zygote=${record.zygotePreloadName ?: "none"}")
    }

    private fun scanRows(report: RootManagersReport): List<RootManagersDetailRowModel> {
        if (report.stage != RootManagersStage.READY) {
            return emptyList()
        }
        val diagnostics = enumerationDiagnostics(report)
        return buildList {
            add(
                RootManagersDetailRowModel(
                    label = "Enumeration",
                    value = enumerationLabel(report.enumerationState),
                    status = enumerationStatus(report.enumerationState),
                    hiddenCopyText = diagnostics,
                ),
            )
            add(
                RootManagersDetailRowModel(
                    label = "Profiles searched",
                    value = "${report.profilesScanned} of ${report.profileScans.size}",
                    status = if (report.profileScans.any { scan -> scan.denied }) {
                        DetectorStatus.info(InfoKind.SUPPORT)
                    } else {
                        DetectorStatus.allClear()
                    },
                    hiddenCopyText = diagnostics,
                ),
            )
            add(
                RootManagersDetailRowModel(
                    label = "Launcher activities seen",
                    value = report.launcherActivitiesSeen.toString(),
                    status = DetectorStatus.allClear(),
                    hiddenCopyText = diagnostics,
                ),
            )
            add(
                RootManagersDetailRowModel(
                    label = "Matched apps",
                    value = report.entries.size.toString(),
                    status = report.toDetectorStatus(),
                    hiddenCopyText = diagnostics,
                ),
            )
            if (report.issues.isNotEmpty()) {
                add(
                    RootManagersDetailRowModel(
                        label = "Issues",
                        value = report.issues.size.toString(),
                        status = DetectorStatus.info(InfoKind.SUPPORT),
                        detail = report.issues.joinToString(separator = "\n"),
                        detailMonospace = true,
                        hiddenCopyText = diagnostics,
                    ),
                )
            }
        }
    }

    private fun enumerationLabel(state: RootManagersEnumerationState): String = when (state) {
        RootManagersEnumerationState.EVALUATED -> "Searched"
        RootManagersEnumerationState.UNAVAILABLE -> "Unavailable"
        RootManagersEnumerationState.UNDECIDABLE -> "No profiles"
    }

    private fun enumerationStatus(state: RootManagersEnumerationState): DetectorStatus = when (state) {
        RootManagersEnumerationState.EVALUATED -> DetectorStatus.allClear()
        RootManagersEnumerationState.UNAVAILABLE -> DetectorStatus.info(InfoKind.ERROR)
        RootManagersEnumerationState.UNDECIDABLE -> DetectorStatus.info(InfoKind.SUPPORT)
    }

    private fun anchorList(entry: RootManagerEntry): String =
        entry.anchors.sortedBy { anchor -> anchor.ordinal }.joinToString(", ") { anchor -> anchor.displayName }

    private fun statusFor(confidence: RootManagerConfidence): DetectorStatus = when (confidence) {
        RootManagerConfidence.HIGH,
        RootManagerConfidence.MEDIUM -> DetectorStatus.warning()

        RootManagerConfidence.LOW -> DetectorStatus.info(InfoKind.SUPPORT)
    }

    private fun entryDiagnostics(entry: RootManagerEntry): String = buildString {
        appendLine("Root manager match")
        appendLine("Family: ${entry.family.displayName}")
        appendLine("Package: ${entry.packageName}")
        appendLine("Label: ${entry.displayName}")
        appendLine("Profile user id: ${entry.profileUserId}")
        appendLine("Confidence: ${entry.confidence.displayName}")
        appendLine("Anchors: ${anchorList(entry)}")
        appendLine("Component class: ${entry.componentClassName ?: "none"}")
        appendLine("Application class: ${entry.applicationClassName ?: "none"}")
        appendLine("Source dir: ${entry.sourceDir ?: "none"}")
        appendLine("UID: ${entry.uid?.toString() ?: "unknown"}")
        append("First install time: ${entry.firstInstallTime?.toString() ?: "unknown"}")
    }

    private fun enumerationDiagnostics(report: RootManagersReport): String = buildString {
        appendLine("Root Managers enumeration")
        appendLine("Stage: ${report.stage}")
        appendLine("Enumeration state: ${report.enumerationState}")
        appendLine("Profiles searched: ${report.profilesScanned} of ${report.profileScans.size}")
        appendLine("Launcher activities seen: ${report.launcherActivitiesSeen}")
        appendLine("Matched apps: ${report.entries.size}")
        if (report.profileScans.isNotEmpty()) {
            appendLine("Profile scans:")
            report.profileScans.forEach { scan ->
                appendLine("  user ${scan.profileUserId}: denied=${scan.denied}, activities=${scan.launcherActivitiesSeen}")
            }
        }
        if (report.issues.isNotEmpty()) {
            appendLine("Issues:")
            report.issues.forEach { issue -> appendLine("  $issue") }
        }
    }

    private companion object {
        const val TITLE = "Root Managers"
        const val SUBTITLE = "Root manager visibility"
    }
}
