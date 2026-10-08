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
 * Turns launcher-visible records into root manager entries, matching per field.
 *
 * The fields are independent evidence, so a family counts as matched when any strong field matched
 * (a surviving package name, application class, or zygote preload name), or when two weak fields did.
 * A single weak field never produces an entry: it is a word a launcher label or a conventional
 * `.MainActivity` could carry by coincidence.
 */
object RootManagerEntryRules {

    fun entries(
        records: List<LauncherActivityRecord>,
        signatures: List<RootManagerSignature> = RootManagerCatalog.signatures,
    ): List<RootManagerEntry> = records
        .mapNotNull { record -> match(record, signatures) }
        // An app can expose several launcher activities; keep the anchor-richest one per (user, package).
        .groupBy { entry -> entry.profileUserId to entry.packageName }
        .values
        .map { group -> group.maxByOrNull { entry -> entry.anchors.size } ?: group.first() }
        .sortedWith(compareBy<RootManagerEntry>({ it.profileUserId }, { it.family.ordinal }, { it.packageName }))

    fun match(
        record: LauncherActivityRecord,
        signatures: List<RootManagerSignature> = RootManagerCatalog.signatures,
    ): RootManagerEntry? {
        val best = signatures
            .mapIndexedNotNull { index, signature -> candidate(record, signature, index) }
            .maxWithOrNull(CANDIDATE_ORDER)
        return best?.toEntry()
    }

    private fun candidate(
        record: LauncherActivityRecord,
        signature: RootManagerSignature,
        index: Int,
    ): Candidate? {
        val anchors = matchedAnchors(record, signature)
        if (!isMatch(anchors)) {
            return null
        }
        return Candidate(
            record = record,
            signature = signature,
            anchors = anchors,
            confidence = confidence(anchors),
            index = index,
            labelPrefixLength = longestMatch(record.label, signature.labelPrefixes, prefix = true),
            applicationClassSuffixLength = longestMatch(
                value = record.applicationClassName,
                candidates = signature.applicationClassSuffixes,
                prefix = false,
            ),
        )
    }

    private fun matchedAnchors(
        record: LauncherActivityRecord,
        signature: RootManagerSignature,
    ): Set<RootManagerAnchor> {
        val anchors = mutableSetOf<RootManagerAnchor>()
        if (record.packageName in signature.defaultPackageNames ||
            signature.namespacePrefixes.any { record.packageName.startsWith(it) }
        ) {
            anchors += RootManagerAnchor.PACKAGE_NAME
        }
        val applicationClass = record.applicationClassName
        if (applicationClass != null &&
            signature.applicationClassSuffixes.any { applicationClass.endsWith(it) }
        ) {
            anchors += RootManagerAnchor.APPLICATION_CLASS
        }
        // The zygote name's suffix and namespace are two readings of one field, so they count once.
        val zygotePreload = record.zygotePreloadName
        if (zygotePreload != null &&
            (signature.zygotePreloadNameSuffixes.any { zygotePreload.endsWith(it) } ||
                signature.zygotePreloadNamespaces.any { zygotePreload.startsWith(it) })
        ) {
            anchors += RootManagerAnchor.ZYGOTE_PRELOAD
        }
        val label = record.label
        if (label != null && signature.labelPrefixes.any { label.startsWith(it) }) {
            anchors += RootManagerAnchor.LABEL
        }
        val componentClass = record.componentClassName
        if (componentClass != null &&
            signature.launcherClassSuffixes.any { componentClass.endsWith(it) }
        ) {
            anchors += RootManagerAnchor.LAUNCHER_CLASS
        }
        return anchors
    }

    private fun isMatch(anchors: Set<RootManagerAnchor>): Boolean =
        anchors.any { it.strong } || anchors.size >= 2

    private fun confidence(anchors: Set<RootManagerAnchor>): RootManagerConfidence = when {
        anchors.none { it.strong } -> RootManagerConfidence.LOW
        anchors.size >= 2 -> RootManagerConfidence.HIGH
        else -> RootManagerConfidence.MEDIUM
    }

    private fun longestMatch(value: String?, candidates: Set<String>, prefix: Boolean): Int {
        val text = value ?: return 0
        return candidates
            .filter { candidate -> if (prefix) text.startsWith(candidate) else text.endsWith(candidate) }
            .maxOfOrNull { it.length } ?: 0
    }

    /**
     * Ranks the families one record matched, so a record becomes a single entry under its best fit:
     * the shared `.KernelSUApplication` makes several signatures match, and the ranking lands the
     * record on its most specific family (ties fall back to catalogue order).
     */
    private val CANDIDATE_ORDER: Comparator<Candidate> =
        compareBy<Candidate> { if (RootManagerAnchor.PACKAGE_NAME in it.anchors) 1 else 0 }
            .thenBy { it.labelPrefixLength }
            .thenBy { it.applicationClassSuffixLength }
            .thenBy { it.anchors.size }
            .thenByDescending { it.index }

    private data class Candidate(
        val record: LauncherActivityRecord,
        val signature: RootManagerSignature,
        val anchors: Set<RootManagerAnchor>,
        val confidence: RootManagerConfidence,
        val index: Int,
        val labelPrefixLength: Int,
        val applicationClassSuffixLength: Int,
    ) {
        fun toEntry(): RootManagerEntry = RootManagerEntry(
            family = signature.family,
            packageName = record.packageName,
            displayName = record.label?.takeIf { it.isNotBlank() } ?: signature.family.displayName,
            profileUserId = record.profileUserId,
            componentClassName = record.componentClassName,
            applicationClassName = record.applicationClassName,
            sourceDir = record.sourceDir,
            uid = record.uid,
            firstInstallTime = record.firstInstallTime,
            anchors = anchors,
            confidence = confidence,
        )
    }
}
