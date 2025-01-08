package com.spotify.ruler.plugin

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationVariant
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

/**
 * Returns the bundle file that's going to be analyzed. DexGuard produces a separate bundle instead of overriding
 * the default one, so we have to handle that separately.
 */
internal fun Project.getBundleFile(
    variant: ApplicationVariant
): Provider<RegularFile> {
    val defaultBundleFile = variant.artifacts.get(SingleArtifact.BUNDLE)
    if (!hasDexGuard(project)) {
        return defaultBundleFile // No DexGuard means we can use the default bundle
    }

    // Bundle can still be in the default location, depending on the DexGuard config
    return defaultBundleFile.flatMap { bundle ->
        val dexGuardBundle =
            bundle.asFile.parentFile.resolve("${bundle.asFile.nameWithoutExtension}-protected.aab")
        if (dexGuardBundle.exists()) {
            project.layout.buildDirectory.file(dexGuardBundle.absolutePath) // File exists -> use it
        } else {
            defaultBundleFile // File doesn't exist -> fall back to default
        }
    }
}

/**
 * Returns the mapping file used for de-obfuscation. Different obfuscation tools like DexGuard and ProGuard place
 * their mapping files in different directories, so we have to handle those separately.
 */
internal fun Project.getMappingFile(
    variant: ApplicationVariant
): Provider<RegularFile> {
    val defaultMappingFile = variant.artifacts.get(SingleArtifact.OBFUSCATION_MAPPING_FILE)
    val mappingFilePath = when {
        hasDexGuard(project) -> "outputs/dexguard/mapping/bundle/${variant.name}/mapping.txt"
        hasProGuard(project) -> "outputs/proguard/${variant.name}/mapping/mapping.txt"
        else -> return defaultMappingFile // No special obfuscation plugin -> use default path
    }

    // Mapping files can also be missing, for example when obfuscation is disabled for a variant
    val mappingFileProvider = project.layout.buildDirectory.file(mappingFilePath)
    return mappingFileProvider.flatMap { mappingFile ->
        if (mappingFile.asFile.exists()) {
            mappingFileProvider // File exists -> use it
        } else {
            defaultMappingFile // File doesn't exist -> fall back to default
        }
    }
}

/**
 * Returns a mapping file to de-obfuscate resource names. DexGuard supports this feature by default, so we need to
 * handle it accordingly.
 */
internal fun Project.getResourceMappingFile(
    variant: ApplicationVariant
): Provider<RegularFile> {
    val defaultResourceMappingFile = project.objects.fileProperty() // Empty by default
    @Suppress("SpellCheckingInspection")
    val resourceMappingFilePath = when {
        hasDexGuard(project) -> "outputs/dexguard/mapping/bundle/${variant.name}/resourcefilenamemapping.txt"
        else -> return defaultResourceMappingFile // No DexGuard plugin -> use default empty file
    }

    // Mapping file can still be missing, for example if resource obfuscation is disabled for a variant
    val resourceMappingFileProvider =
        project.layout.buildDirectory.file(resourceMappingFilePath)
    return resourceMappingFileProvider.flatMap { resourceMappingFile ->
        if (resourceMappingFile.asFile.exists()) {
            resourceMappingFileProvider // File exists -> use it
        } else {
            defaultResourceMappingFile // File doesn't exist -> fall back to default
        }
    }
}

/** Checks if the given [project] is using DexGuard for obfuscation, instead of R8. */
private fun hasDexGuard(project: Project): Boolean {
    return project.pluginManager.hasPlugin("dexguard")
}

/** Checks if the given [project] is using ProGuard for obfuscation, instead of R8. */
private fun hasProGuard(project: Project): Boolean {
    @Suppress("SpellCheckingInspection")
    return project.pluginManager.hasPlugin("com.guardsquare.proguard")
}
