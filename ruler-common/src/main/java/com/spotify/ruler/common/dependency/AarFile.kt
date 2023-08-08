package com.spotify.ruler.common.dependency

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipFile


/**
 * The 'aar' bundle is the binary distribution of an Android Library Project.
 * The file extension is .aar, and the maven artifact type should be aar as well,
 * but the file itself a simple zip file with the following entries:
 * - /AndroidManifest.xml (mandatory)
 * - /classes.jar (optional)
 * - /res/ (mandatory)
 * - /R.txt (mandatory)
 * - /assets/ (optional)
 * - /libs/ *.jar (optional)
 * - /jni/<abi>/ *.so (optional)
 * - /proguard.txt (optional)
 * - /lint.jar (optional)
 * These entries are directly at the root of the zip file.
 * The R.txt file is the output of aapt with --output-text-symbols.
</abi> */
class AarFile(val file: File) : ZipFile(file) {
    val jarFile: File?
        get() = extractClassesJar(file, File(System.getProperty("java.io.tmpdir")))

    constructor(fileName: String) : this(File(fileName))

    /**
     * Extracts a zip file specified by the zipFilePath to a directory specified by
     * destDirectory (will be created if does not exists)
     * @param aarFile
     * @param tmpDir
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun extractClassesJar(aarFile: File, tmpDir: File): File? {
        ZipFile(aarFile).use { zip ->
            zip.getEntry(CLASSES_JAR)?.let { entry ->
                val outDir = File(
                    "${tmpDir}${File.separator}jarTmp${File.separator}${aarFile.name}"
                        .replace(".aar", "")
                )
                if (!outDir.exists()) outDir.mkdirs()
                val outFile = File("$outDir${File.separator}${entry.name}")
                if (!entry.isDirectory) {
                    extractFile(zip.getInputStream(entry), outFile)
                    return outFile
                }
            }
        }
        return null
    }

    /**
     * Extracts a zip entry (file entry)
     * @param zipIn
     * @param outFile
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun extractFile(zipIn: InputStream, outFile: File) {
        val bos = BufferedOutputStream(FileOutputStream(outFile))
        val bytesIn = ByteArray(BUFFER_SIZE)
        var read: Int
        while (zipIn.read(bytesIn).also { read = it } != -1) {
            bos.write(bytesIn, 0, read)
        }
        bos.close()
    }

    companion object {
        const val CLASSES_JAR = "classes.jar"

        private const val BUFFER_SIZE = 4096
    }
}