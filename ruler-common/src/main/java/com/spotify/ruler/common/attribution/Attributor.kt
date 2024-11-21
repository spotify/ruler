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

package com.spotify.ruler.common.attribution

import com.spotify.ruler.common.dependency.DependencyComponent
import com.spotify.ruler.models.AppFile
import com.spotify.ruler.models.FileType

private typealias Dependencies = Map<String, List<DependencyComponent>>
private typealias StaticRegexDependencies = Map<Regex, List<DependencyComponent>>
/**
 * Responsible for attributing files to the components they are coming from.
 *
 * @param defaultComponent Component to which files will be assigned, if they can't be attributed to any other component
 */
class Attributor(
    private val defaultComponent: DependencyComponent,
    private val staticDependencies: StaticRegexDependencies = emptyMap()
) {

    private val resourceVersionRegex = "(/res/[a-z][^/])*-(.*?)(?=/)".toRegex()
    private val resourceMultipleVectorRegex = "\\\$(\\D+)__\\d+\\.xml\$".toRegex()

    /**
     * Attributes files contained in the final app to the component that they are coming from.
     *
     * @param files List of files contained in the APK(s).
     * @param dependencies Map of file names to a list of all components which include this file
     * @return Map of component names to the list of app files attributed to this component
     */
    fun attribute(files: List<AppFile>, dependencies: Dependencies): Map<DependencyComponent, List<AppFile>> {
        val components = mutableMapOf<DependencyComponent, MutableList<AppFile>>()
        val sortedDependenciesMap = sortStaticDependenciesMap()
        files.forEach { file ->
            val component = when (file.type) {
                FileType.CLASS -> getComponentForClass(file.name, dependencies)
                FileType.RESOURCE -> getComponentForResource(file.name, dependencies)
                FileType.ASSET -> getComponentForAsset(file.name, dependencies)
                FileType.NATIVE_LIB -> getComponentForNativeLib(file.name, dependencies)
                FileType.NATIVE_FILE -> getComponentFromStaticDependenciesMap(sortedDependenciesMap, file.name)
                FileType.OTHER -> getComponentForFile(file.name, dependencies)
            } ?: getComponentFromStaticDependenciesMap(sortedDependenciesMap, file.name) ?: defaultComponent

            components.getOrPut(component) { ArrayList() }.add(file)
        }
        return components
    }


    /**
     * This method organizes the provided `Regex` patterns to prioritize more specific patterns over
     * general ones, using the length of each pattern as an approximation of specificity. Patterns with
     * longer lengths are considered more specific and are sorted to appear earlier in the list.
     *
     * The sorting is necessary in the attribution step to always try to match more specific paths first.
     *
     * @return A new sorted map of StaticRegexDependencies
     *
     * Example:
     * ```
     * val patterns = listOf(
     *     Regex("client-core"),
     *     Regex("client-core/shared/playlist")
     * )
     *
     * val sortedPatterns = sortPatternsBySpecificity(patterns)
     * // Returns: [Regex("client-core/shared/playlist"), Regex("client-core")]
     * ```
     */
    private fun sortStaticDependenciesMap(): StaticRegexDependencies {
        return staticDependencies
            .toList() // Convert to a list of pairs (to enable sorting)
            .sortedByDescending { it.first.pattern.length } // Sort by regex pattern length
            .toMap() // Convert back to a map
    }


    /** Tries to determine the component for a certain class. */
    @Suppress("ReturnCount")
    private fun getComponentForClass(name: String, dependencies: Dependencies): DependencyComponent? {
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

        // Attribute external synthetic classes based on their simple class name
        if (name.contains("\$\$ExternalSynthetic")) {
            val simpleClassName = name.substringBefore("\$\$ExternalSynthetic").substringAfterLast('.')
            val candidates = dependencies.filter { it.key.substringAfterLast('.') == simpleClassName }.values.flatten()
            val component = candidates.distinct().singleOrNull()
            if (component != null) {
                return component
            }
        }

        // If everything else fails, try matching based on package name
        val packageName = name.substringBeforeLast('.')
        return getComponentForPackage(packageName, dependencies)
    }

    /**
     *  Tries to determine the component for a certain resource file.
     *
     *  If the resourceName is missing from the dependencies map we try to strip the autogenerated
     *  prefix for example by version.
     *  res/layout-v21/name.xml       -> res/layout/name.xml
     *  res/layout-watch-v22/name.xml -> res/layout/name.xml
     *
     *  For some vector drawables multiple files are generated
     *  /res/drawable-xxhdpi-v4/vector_drawable.png is split into
     *  /res/drawable-anydpi-v24/$ic_car_mode_onboarding_fallback__1.xml
     *  /res/drawable-anydpi-v24/$ic_car_mode_onboarding_fallback__2.xml
     *  /res/drawable-anydpi-v24/$ic_car_mode_onboarding_fallback__3.xml
     *  If no component is found we remove the {$} and the {__X} value.
     * */
    private fun getComponentForResource(name: String, dependencies: Dependencies): DependencyComponent? {
        var resourceName = name.removePrefix("/res")
        var dependencyComponent = dependencies[resourceName]?.singleOrNull()

        if (dependencyComponent == null && name.contains(resourceVersionRegex)) {
            resourceName = name.replace(resourceVersionRegex, "").removePrefix("/res")
            dependencyComponent = dependencies[resourceName]?.singleOrNull()
        }

        if (dependencyComponent == null && name.contains(resourceMultipleVectorRegex)) {
            resourceName = resourceMultipleVectorRegex.replace(name, "\$1.xml").removePrefix("/res")
            dependencyComponent = dependencies[resourceName]?.singleOrNull()
        }

        return dependencyComponent
    }

    /** Tries to determine the component for a certain asset file. */
    private fun getComponentForAsset(name: String, dependencies: Dependencies): DependencyComponent? {
        val assetName = name.removePrefix("/assets")
        return dependencies[assetName]?.singleOrNull()
    }

    /** Tries to determine the component for a certain native library. */
    private fun getComponentForNativeLib(name: String, dependencies: Dependencies): DependencyComponent? {
        val nativeLibName = name.removePrefix("/lib")
        if (dependencies[nativeLibName]?.size == 1) {
            return dependencies.getValue(nativeLibName).single()
        }

        // Attribute LZMA-compressed files to their original source
        val lzmaName = nativeLibName.replace(".lzma.", ".")
        return dependencies[lzmaName]?.singleOrNull()
    }

    /** Tries to determine the component for a certain file. */
    private fun getComponentForFile(name: String, dependencies: Dependencies): DependencyComponent? {
        return dependencies[name]?.singleOrNull()
    }

    /** Tries to determine the component for a certain package. */
    private fun getComponentForPackage(name: String, dependencies: Dependencies): DependencyComponent? {
        val candidates = dependencies.filter { it.key.substringBeforeLast('.') == name }.values.flatten()
        return candidates.distinct().singleOrNull()
    }

    private fun getComponentFromStaticDependenciesMap(
        map: StaticRegexDependencies,
        name: String
    ): DependencyComponent? {
        return map[map.keys.find { it.containsMatchIn(name) }]?.firstOrNull()
    }
}
