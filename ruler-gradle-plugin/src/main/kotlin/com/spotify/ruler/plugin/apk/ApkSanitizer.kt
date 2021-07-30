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

package com.spotify.ruler.plugin.apk

import com.spotify.ruler.models.AppFile
import com.spotify.ruler.models.FileType
import com.spotify.ruler.plugin.common.ClassNameSanitizer

/**
 * Responsible for sanitizing APK entries, so they can be attributed easier.
 *
 * @param classNameSanitizer Used for sanitizing class names
 */
class ApkSanitizer(private val classNameSanitizer: ClassNameSanitizer) {

    /**
     * Sanitizes a list of APK entries, to ease further processing. Sanitizing could mean that certain entries are
     * removed, merged or have their sizes adapted.
     *
     * @param entries List of raw entries parsed from an APK file
     * @return Sanitized list of entries
     */
    fun sanitize(entries: List<ApkEntry>): List<AppFile> {
        val buckets = listOf(
            DexFileBucket(),
            AndroidManifestBucket(),
            BundletoolBucket(),
            ResourcesArscBucket(),
            TyepAssigningBucket()
        )

        // Separate entries into their different sanitization bucket
        entries.forEach { entry ->
            val bucket = buckets.first { it.isApplicable(entry) }
            bucket.add(entry)
        }

        // Empty each bucket and merge the results
        return buckets.flatMap(SanitizationBucket::sanitize)
    }

    /** Entries are split into different sanitization buckets, each bucket sanitizes its entries in a different way. */
    private abstract class SanitizationBucket {
        protected val entries = mutableListOf<ApkEntry>()

        /** Checks if a certain entry belongs to this bucket. */
        abstract fun isApplicable(entry: ApkEntry): Boolean

        /** Sanitizes and returns all gathered entries. */
        abstract fun sanitize(): List<AppFile>

        fun add(entry: ApkEntry) = entries.add(entry)
    }

    /**
     * Unpack DEX files to their classes. Each class name has to be de-obfuscated. The sum of the size of all classes is
     * bigger than the size of the containing DEX file (because they are compressed). So just unpacking them would skew
     * the overall app size. To combat this, we have to calculate the proportional size of each class, so that summing
     * their sizes results in the correct value.
     */
    private inner class DexFileBucket : SanitizationBucket() {
        override fun isApplicable(entry: ApkEntry) = entry is ApkEntry.Dex
        override fun sanitize() = entries.flatMap { entry -> sanitizeEntry(entry as ApkEntry.Dex) }

        private fun sanitizeEntry(entry: ApkEntry.Dex): List<AppFile> {
            val sizeOfAllClasses = entry.classes.sumOf(ApkEntry::installSize)
            return entry.classes.map { classEntry ->
                val name = classNameSanitizer.sanitize(classEntry.name)
                val downloadSize = classEntry.downloadSize * entry.downloadSize / sizeOfAllClasses
                val installSize = classEntry.installSize * entry.installSize / sizeOfAllClasses
                AppFile(name, FileType.CLASS, downloadSize, installSize)
            }
        }
    }

    /**
     * Each split APK contains a separate Android manifest, but we're only interested in the one of the base APK. We
     * can safely assume that the largest manifest is the one from the base APK, so we can discard all the others.
     */
    private class AndroidManifestBucket : SanitizationBucket() {
        override fun isApplicable(entry: ApkEntry) = entry.name == "/AndroidManifest.xml"

        override fun sanitize(): List<AppFile> {
            val entry = entries.maxByOrNull(ApkEntry::installSize) ?: return emptyList()
            return listOf(AppFile("/AndroidManifest.xml", FileType.OTHER, entry.downloadSize, entry.installSize))
        }
    }

    /**
     * Bundletool adds some files to each APK (namely a manifest file and a splits.xml). Since those files are not
     * present in the APKs from the Play Store, we can discard them.
     */
    private class BundletoolBucket : SanitizationBucket() {

        override fun isApplicable(entry: ApkEntry): Boolean {
            return entry.name == "/META-INF/MANIFEST.MF" || entry.name.matches(Regex("/res/xml/splits\\d+\\.xml"))
        }

        override fun sanitize() = emptyList<AppFile>()
    }

    /**
     * It's possible that multiple APKs contain compiled resources. In this case, we can just merge them and pretend
     * it's a single file.
     */
    private class ResourcesArscBucket : SanitizationBucket() {
        override fun isApplicable(entry: ApkEntry) = entry.name == "/resources.arsc"

        override fun sanitize(): List<AppFile> {
            if (entries.isEmpty()) return emptyList()

            val downloadSize = entries.sumOf(ApkEntry::downloadSize)
            val installSize = entries.sumOf(ApkEntry::installSize)
            return listOf(AppFile("/resources.arsc", FileType.OTHER, downloadSize, installSize))
        }
    }

    /** For files that are not sanitized in any other way, we just have to assign the correct file type. */
    private class TyepAssigningBucket : SanitizationBucket() {
        override fun isApplicable(entry: ApkEntry) = true
        override fun sanitize() = entries.map(::sanitizeEntry)

        private fun sanitizeEntry(entry: ApkEntry): AppFile {
            val type = when {
                entry.name.startsWith("/res/") -> FileType.RESOURCE
                entry.name.startsWith("/assets/") -> FileType.ASSET
                entry.name.startsWith("/lib/") -> FileType.NATIVE_LIB
                else -> FileType.OTHER
            }
            return AppFile(entry.name, type, entry.downloadSize, entry.installSize)
        }
    }
}
