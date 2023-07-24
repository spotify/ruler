import com.spotify.ruler.common.compare.RulerComparer
import com.spotify.ruler.models.AppFile
import com.spotify.ruler.models.FileType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RulerComparerTest {

    @Test
    fun `test compareBuilds when head and base are identical`() {
        val head = listOf(
            AppFile("file1", FileType.RESOURCE, 100, 100),
            AppFile("file2", FileType.RESOURCE, 200, 200)
        )
        val base = listOf(
            AppFile("file1", FileType.RESOURCE, 100, 100),
            AppFile("file2", FileType.RESOURCE, 200, 200)
        )

        val comparer = RulerComparer()
        val result = comparer.compareBuilds(head, base)

        assertEquals(300, result.newAppDownloadSize)
        assertEquals(300, result.newAppInstallSize)
        assertEquals(0, result.totalSizeDifference)
        assertEquals(0, result.filesChanged.size)
    }

    @Test
    fun `test compareBuilds with different file sizes`() {
        val head = listOf(
            AppFile("file1", FileType.RESOURCE, 150, 150),
            AppFile("file2", FileType.RESOURCE, 250, 250)
        )
        val base = listOf(
            AppFile("file1", FileType.RESOURCE, 100, 100),
            AppFile("file2", FileType.RESOURCE, 200, 200)
        )

        val comparer = RulerComparer()
        val result = comparer.compareBuilds(head, base)

        assertEquals(400, result.newAppDownloadSize)
        assertEquals(400, result.newAppInstallSize)
        assertEquals(-100, result.totalSizeDifference)
        assertEquals(2, result.filesChanged.size)
        assertEquals("file2", result.filesChanged[0].name)
        assertEquals(-50, result.filesChanged[0].difference)
        assertEquals("file1", result.filesChanged[1].name)
        assertEquals(-50, result.filesChanged[1].difference)
    }

    @Test
    fun `test compareBuilds with files in head that don't exist in base`() {
        val head = listOf(
            AppFile("file1", FileType.RESOURCE, 100, 100),
            AppFile("file2", FileType.RESOURCE, 200, 200),
            AppFile("file3", FileType.RESOURCE, 300, 300)
        )
        val base = listOf(
            AppFile("file1", FileType.RESOURCE, 100, 100),
            AppFile("file2", FileType.RESOURCE, 200, 200)
        )

        val comparer = RulerComparer()
        val result = comparer.compareBuilds(head, base)

        assertEquals(600, result.newAppDownloadSize)
        assertEquals(600, result.newAppInstallSize)
        assertEquals(-300, result.totalSizeDifference)
        assertEquals(1, result.filesChanged.size)
        assertEquals("file3", result.filesChanged[0].name)
        assertEquals(-300, result.filesChanged[0].difference)
    }

    @Test
    fun `test compareBuilds with files in base that don't exist in head`() {
        val head = listOf(
            AppFile("file1", FileType.RESOURCE, 100, 100),
            AppFile("file2", FileType.RESOURCE, 200, 200)
        )
        val base = listOf(
            AppFile("file1", FileType.RESOURCE, 100, 100),
            AppFile("file2", FileType.RESOURCE, 200, 200),
            AppFile("file3", FileType.RESOURCE, 300, 300)
        )

        val comparer = RulerComparer()
        val result = comparer.compareBuilds(head, base)

        assertEquals(300, result.newAppDownloadSize)
        assertEquals(300, result.newAppInstallSize)
        assertEquals(0, result.totalSizeDifference)
        assertEquals(0, result.filesChanged.size)
    }
}
