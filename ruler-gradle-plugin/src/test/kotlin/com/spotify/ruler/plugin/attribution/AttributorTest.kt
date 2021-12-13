/*
* Copyright 2021 Spotify AB
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.spotify.ruler.plugin.attribution

import com.google.common.truth.Truth.assertThat
import com.spotify.ruler.models.AppFile
import com.spotify.ruler.models.ComponentType
import com.spotify.ruler.models.FileType
import com.spotify.ruler.plugin.dependency.DependencyComponent
import org.junit.jupiter.api.Test

class AttributorTest {
    private val attributor = Attributor(DependencyComponent(":default", ComponentType.INTERNAL))

    @Test
    fun `Class files are attributed correctly`() {
        val files = listOf(AppFile("com.spotify.MainActivity", FileType.CLASS, 100, 200))
        val dependencies = mapOf("com.spotify.MainActivity" to listOf(
            DependencyComponent(":lib", ComponentType.INTERNAL),
        ))
        val map = attributor.attribute(files, dependencies)

        assertThat(map).containsEntry(DependencyComponent(":lib", ComponentType.INTERNAL), files)
    }

    @Test
    fun `Dagger factories are attributed correctly`() {
        val files = listOf(AppFile("com.spotify.TestClass_Factory", FileType.CLASS, 100, 200))
        val dependencies = mapOf("com.spotify.TestClass" to listOf(
            DependencyComponent(":lib", ComponentType.INTERNAL),
        ))
        val map = attributor.attribute(files, dependencies)

        assertThat(map).containsEntry(DependencyComponent(":lib", ComponentType.INTERNAL), files)
    }

    @Test
    fun `Dagger modules are attributed correctly`() {
        val files = listOf(AppFile("com.spotify.Module_ProvideModuleFactory", FileType.CLASS, 100, 200))
        val dependencies = mapOf("com.spotify.Module" to listOf(
            DependencyComponent(":lib", ComponentType.INTERNAL),
        ))
        val map = attributor.attribute(files, dependencies)

        assertThat(map).containsEntry(DependencyComponent(":lib", ComponentType.INTERNAL), files)
    }

    @Test
    fun `Lambdas are attributed correctly`() {
        val files = listOf(AppFile("com.spotify.-\$\$Lambda\$sadfjliajsdf", FileType.CLASS, 100, 200))
        val dependencies = mapOf("com.spotify.MainActivity" to listOf(
            DependencyComponent(":lib", ComponentType.INTERNAL),
        ))
        val map = attributor.attribute(files, dependencies)

        assertThat(map).containsEntry(DependencyComponent(":lib", ComponentType.INTERNAL), files)
    }

    @Test
    fun `External synthetic classes are attributed correctly`() {
        val files = listOf(AppFile("a.TestClass\$\$ExternalSyntheticLambda1", FileType.CLASS, 100, 200))
        val dependencies = mapOf("com.spotify.TestClass" to listOf(
            DependencyComponent(":lib", ComponentType.INTERNAL),
        ))
        val map = attributor.attribute(files, dependencies)

        assertThat(map).containsEntry(DependencyComponent(":lib", ComponentType.INTERNAL), files)
    }

    @Test
    fun `External synthetic classes are not attributed if there is no unambiguous component`() {
        val files = listOf(AppFile("a.TestClass\$\$ExternalSyntheticLambda1", FileType.CLASS, 100, 200))
        val dependencies = mapOf(
            "com.spotify.TestClass" to listOf(DependencyComponent(":lib", ComponentType.INTERNAL)),
            "com.spotify.other.TestClass" to listOf(DependencyComponent(":other", ComponentType.INTERNAL)),
        )
        val map = attributor.attribute(files, dependencies)

        assertThat(map).containsEntry(DependencyComponent(":default", ComponentType.INTERNAL), files)
    }

    @Test
    fun `Classes are attributed based on their package name`() {
        val files = listOf(AppFile("com.spotify.UnknownClass", FileType.CLASS, 100, 200))
        val dependencies = mapOf("com.spotify.MainActivity" to listOf(
            DependencyComponent(":lib", ComponentType.INTERNAL),
        ))
        val map = attributor.attribute(files, dependencies)

        assertThat(map).containsEntry(DependencyComponent(":lib", ComponentType.INTERNAL), files)
    }

    @Test
    fun `Resources are attributed correctly`() {
        val files = listOf(AppFile("/res/layout/activity_main.xml", FileType.RESOURCE, 100, 200))
        val dependencies = mapOf("/layout/activity_main.xml" to listOf(
            DependencyComponent(":lib", ComponentType.INTERNAL),
        ))
        val map = attributor.attribute(files, dependencies)

        assertThat(map).containsEntry(DependencyComponent(":lib", ComponentType.INTERNAL), files)
    }

    @Test
    fun `Assets are attributed correctly`() {
        val files = listOf(AppFile("/assets/licenses.html", FileType.ASSET, 100, 200))
        val dependencies = mapOf("/licenses.html" to listOf(
            DependencyComponent(":lib", ComponentType.INTERNAL),
        ))
        val map = attributor.attribute(files, dependencies)

        assertThat(map).containsEntry(DependencyComponent(":lib", ComponentType.INTERNAL), files)
    }

    @Test
    fun `Native libs are attributed correctly`() {
        val files = listOf(AppFile("/lib/arm64-v8a/lib.so", FileType.NATIVE_LIB, 100, 200))
        val dependencies = mapOf("/arm64-v8a/lib.so" to listOf(
            DependencyComponent(":lib", ComponentType.INTERNAL),
        ))
        val map = attributor.attribute(files, dependencies)

        assertThat(map).containsEntry(DependencyComponent(":lib", ComponentType.INTERNAL), files)
    }

    @Test
    fun `LZMA-compressed native libs are attributed correctly`() {
        val files = listOf(AppFile("/lib/arm64-v8a/lib.lzma.so", FileType.NATIVE_LIB, 100, 200))
        val dependencies = mapOf("/arm64-v8a/lib.so" to listOf(
            DependencyComponent(":lib", ComponentType.INTERNAL),
        ))
        val map = attributor.attribute(files, dependencies)

        assertThat(map).containsEntry(DependencyComponent(":lib", ComponentType.INTERNAL), files)
    }

    @Test
    fun `Other files are attributed correctly`() {
        val files = listOf(AppFile("/test.properties", FileType.OTHER, 100, 200))
        val dependencies = mapOf("/test.properties" to listOf(
            DependencyComponent(":lib", ComponentType.INTERNAL),
        ))
        val map = attributor.attribute(files, dependencies)

        assertThat(map).containsEntry(DependencyComponent(":lib", ComponentType.INTERNAL), files)
    }

    @Test
    fun `Files which can be attributed to multiple components belong to the default component`() {
        val files = listOf(AppFile("/test.properties", FileType.OTHER, 100, 200))
        val dependencies = mapOf("/test.properties" to listOf(
            DependencyComponent(":lib", ComponentType.INTERNAL),
            DependencyComponent("com.spotify:main:1.0.0", ComponentType.EXTERNAL),
        ))
        val map = attributor.attribute(files, dependencies)

        assertThat(map).containsEntry(DependencyComponent(":default", ComponentType.INTERNAL), files)
    }

    @Test
    fun `Files which can't be attributed are belong to the default component`() {
        val files = listOf(AppFile("/test.properties", FileType.OTHER, 100, 200))
        val map = attributor.attribute(files, emptyMap())

        assertThat(map).containsEntry(DependencyComponent(":default", ComponentType.INTERNAL), files)
    }
}
