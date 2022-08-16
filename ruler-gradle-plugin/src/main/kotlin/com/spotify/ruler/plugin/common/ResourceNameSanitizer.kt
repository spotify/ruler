package com.spotify.ruler.plugin.common

import java.io.File
import java.io.StringReader

/**
 * Responsible for sanitizing resource file names.
 */
class ResourceNameSanitizer {
    private val resourceNameMap = ResourceNameMap()

    constructor(mappingFile: File?) { mappingFile?.let(resourceNameMap::readFromFile) }
    constructor(mapping: String) { resourceNameMap.readFromReader(StringReader(mapping)) }

    /** Sanitizes a given [resourceName], which includes deobfuscation (if applicable). */
    fun sanitize(resourceName: String): String {
        return resourceNameMap.getResourceName(resourceName) // /res/raw/dVo.xml -> /res/drawable/foo.xml
    }
}
