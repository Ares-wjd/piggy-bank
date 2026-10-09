package io.github.areswjd.piggybank.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseParsingTest {

    @Test
    fun newerVersion_comparesNumerically() {
        assertTrue(isNewerVersion("1.0.1", "1.0.0"))
        assertTrue(isNewerVersion("v1.2.10", "1.2.9"))
        assertTrue(isNewerVersion("2.0", "1.9.9"))
        assertTrue(isNewerVersion("1.0.0.1", "1.0.0"))
        assertFalse(isNewerVersion("1.0.0", "1.0.0"))
        assertFalse(isNewerVersion("v1.0", "1.0.0"))
        assertFalse(isNewerVersion("0.9.9", "1.0.0"))
        assertFalse(isNewerVersion("1.0.0-beta", "1.0.0"))
    }

    private fun release(
        tag: String = "v1.0.1",
        assetName: String = RELEASE_APK_NAME,
        draft: Boolean = false,
        prerelease: Boolean = false,
    ) = """
        {
          "tag_name": "$tag",
          "name": "Piggy bank $tag",
          "draft": $draft,
          "prerelease": $prerelease,
          "body": "  - 버그 수정\n",
          "assets": [
            {"name": "$assetName", "size": 1234,
             "browser_download_url": "https://github.com/o/r/releases/download/$tag/$assetName"}
          ],
          "author": {"login": "someone"}
        }
    """.trimIndent()

    @Test
    fun parsesReleaseWithApk() {
        val parsed = parseLatestRelease(release())!!
        assertEquals("1.0.1", parsed.version)
        assertEquals("- 버그 수정", parsed.notes)
        assertEquals(1234L, parsed.apkSize)
        assertEquals("https://github.com/o/r/releases/download/v1.0.1/piggybank.apk", parsed.apkUrl)
    }

    @Test
    fun ignoresReleaseWithoutApkOrNotFinal() {
        assertNull(parseLatestRelease(release(assetName = "other.zip")))
        assertNull(parseLatestRelease(release(draft = true)))
        assertNull(parseLatestRelease(release(prerelease = true)))
    }
}
