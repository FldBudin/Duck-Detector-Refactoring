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
 * One field of a launcher-visible app that a family signature can match, and how much a match proves.
 *
 * Strong anchors are the fields a manager cannot rename away cheaply: the application class and the
 * zygote preload name survive an applicationId-only rename, and a surviving namespace keeps the
 * package anchor alive. Weak anchors are family words or conventional slots an unrelated app could
 * also carry, so they only ever corroborate.
 */
enum class RootManagerAnchor(
    val displayName: String,
    val strong: Boolean,
) {
    /** `ApplicationInfo.packageName`: the family's default package name, or a surviving namespace. */
    PACKAGE_NAME("package name", strong = true),

    /** `ApplicationInfo.className`: the app's `Application` class, such as `...KernelSUApplication`. */
    APPLICATION_CLASS("application class", strong = true),

    /** `ApplicationInfo.zygotePreloadName`: the app zygote preload class, such as `...AppZygotePreload`. */
    ZYGOTE_PRELOAD("zygote preload", strong = true),

    LABEL("label", strong = false),

    LAUNCHER_CLASS("launcher class", strong = false),
}
