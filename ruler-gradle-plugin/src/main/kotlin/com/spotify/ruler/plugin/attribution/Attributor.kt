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

import com.spotify.ruler.models.AppFile
import com.spotify.ruler.models.FileType

/**
 * Responsible for attributing files to the components they are coming from.
 *
 * @param defaultComponent Component to which files will be assigned, if they can't be attributed to any other component
 */
class Attributor(private val defaultComponent: String) {

    /**
     * Attributes files contained in the final app to the component that they are coming from.
     *
     * @param files List of files contained in the APK(s).
     * @param dependencies Map of file names to a list of all components which include this file
     * @return Map of component names to the list of app files attributed to this component
     */
    fun attribute(files: List<AppFile>, dependencies: Map<String, List<String>>): Map<String, List<AppFile>> {
        val components = mutableMapOf<String, MutableList<AppFile>>()
        files.forEach { file ->
            val component = when(file.type) {
                FileType.CLASS -> getComponentForClass(file.name, dependencies)
                FileType.RESOURCE -> getComponentForResource(file.name, dependencies)
                FileType.ASSET -> getComponentForAsset(file.name, dependencies)
                FileType.NATIVE_LIB -> getComponentForNativeLib(file.name, dependencies)
                FileType.OTHER -> getComponentForFile(file.name, dependencies)
            } ?: defaultComponent

            components.getOrPut(component) { ArrayList() }.add(file)
        }
        return components
    }

    /** Tries to determine the component for a certain class. */
    @Suppress("ReturnCount")
    private fun getComponentForClass(name: String, dependencies: Map<String, List<String>>): String? {
        if (dependencies[name]?.size == 1) {
            return dependencies.getValue(name).single()
        }

        // Attribute Dagger factories like the type they produce
        val daggerFactoryName = name.removeSuffix("_Factory")
        if (dependencies[daggerFactoryName]?.size == 1) {
            return dependencies.getValue(daggerFactoryName).single()
        }

        // Attribute Dagger modules like their abstract class/interface
        val daggerModuleName = name.substringBefore("_Provide")
        if (dependencies[daggerModuleName]?.size == 1) {
            return dependencies.getValue(daggerModuleName).single()
        }

        // Try to attribute lambdas based on their package
        if (name.contains(".-\$\$Lambda\$")) {
            val packageName = name.substringBefore(".-\$\$Lambda\$")
            val component = getComponentForPackage(packageName, dependencies)
            if (component != null) {
                return component
            }
        }

        // If everything else fails, try matching based on package name
        val packageName = name.substringBeforeLast('.')
        return getComponentForPackage(packageName, dependencies)
    }

    /** Tries to determine the component for a certain resource file. */
    private fun getComponentForResource(name: String, dependencies: Map<String, List<String>>): String? {
        val resourceName = name.removePrefix("/res")
        return dependencies[resourceName]?.singleOrNull()
    }

    /** Tries to determine the component for a certain asset file. */
    private fun getComponentForAsset(name: String, dependencies: Map<String, List<String>>): String? {
        val assetName = name.removePrefix("/assets")
        return dependencies[assetName]?.singleOrNull()
    }

    /** Tries to determine the component for a certain native library. */
    private fun getComponentForNativeLib(name: String, dependencies: Map<String, List<String>>): String? {
        val nativeLibName = name.removePrefix("/lib")
        if (dependencies[nativeLibName]?.size == 1) {
            return dependencies.getValue(nativeLibName).single()
        }

        // Attribute LZMA-compressed files to their original source
        val lzmaName = nativeLibName.replace(".lzma.", ".")
        return dependencies[lzmaName]?.singleOrNull()
    }

    /** Tries to determine the component for a certain file. */
    private fun getComponentForFile(name: String, dependencies: Map<String, List<String>>): String? {
        return dependencies[name]?.singleOrNull()
    }

    /** Tries to determine the component for a certain package. */
    private fun getComponentForPackage(name: String, dependencies: Map<String, List<String>>): String? {
        val candidates = dependencies.filter { it.key.substringBeforeLast('.') == name }.values.flatten()
        return candidates.distinct().singleOrNull()
    }
}
