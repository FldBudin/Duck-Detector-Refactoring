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
 * What one family looks like through the launcher-visible fields of an installed app.
 *
 * Each set matches a launcher-visible field and is independent evidence (see [RootManagerEntryRules]);
 * a set may be empty when a fork ships that anchor only in some versions. Suffix sets match the end
 * of a class name so a renamed package segment still matches; namespace sets match its start.
 */
data class RootManagerSignature(
    val family: RootManagerFamily,
    val defaultPackageNames: Set<String> = emptySet(),
    val namespacePrefixes: Set<String> = emptySet(),
    val applicationClassSuffixes: Set<String> = emptySet(),
    val labelPrefixes: Set<String> = emptySet(),
    val zygotePreloadNameSuffixes: Set<String> = emptySet(),
    val zygotePreloadNamespaces: Set<String> = emptySet(),
    val launcherClassSuffixes: Set<String> = emptySet(),
)
