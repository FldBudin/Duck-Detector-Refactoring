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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RootManagerCatalogTest {

    private val signatures = RootManagerCatalog.signatures

    @Test
    fun `every family is described exactly once`() {
        assertEquals(enumValues<RootManagerFamily>().toList(), signatures.map { it.family })
    }

    @Test
    fun `no default package name is claimed by two families`() {
        val packageNames = signatures.flatMap { it.defaultPackageNames }

        assertEquals(packageNames.distinct(), packageNames)
        assertNotEquals(0, packageNames.size)
    }

    @Test
    fun `every family can be matched by at least one strong anchor`() {
        signatures.forEach { signature ->
            assertTrue(
                signature.family.displayName,
                signature.defaultPackageNames.isNotEmpty() ||
                    signature.applicationClassSuffixes.isNotEmpty() ||
                    signature.zygotePreloadNameSuffixes.isNotEmpty() ||
                    signature.zygotePreloadNamespaces.isNotEmpty(),
            )
        }
    }
}
