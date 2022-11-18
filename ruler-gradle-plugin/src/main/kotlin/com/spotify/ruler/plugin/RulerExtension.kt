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

package com.spotify.ruler.plugin

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

open class RulerExtension(objects: ObjectFactory) {
    val abi: Property<String> = objects.property(String::class.java)
    val locale: Property<String> = objects.property(String::class.java)
    val screenDensity: Property<Int> = objects.property(Int::class.java)
    val sdkVersion: Property<Int> = objects.property(Int::class.java)

    val ownershipFile: RegularFileProperty = objects.fileProperty()
    val defaultOwner: Property<String> = objects.property(String::class.java)

    val omitFileBreakdown: Property<Boolean> = objects.property(Boolean::class.java)

    // Set up default values
    init {
        defaultOwner.convention("unknown")
        omitFileBreakdown.convention(false)
    }
}
