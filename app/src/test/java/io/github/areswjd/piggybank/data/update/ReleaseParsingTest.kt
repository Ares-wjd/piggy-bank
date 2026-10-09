package io.github.areswjd.piggybank.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReleaseParsingTest {

    private fun release(
        body: String = "versionCode: 57\\n화면 디자인 선택 추가\\n\\n- 버그 수정",
        assetName: String = "piggybank.apk",
        draft: Boolean = false,
    ) = """
        {
          "tag_name": "dev-latest",
          "name": "개발 버전 (빌드 #57)",
          "draft": $draft,
          "prerelease": true,
          "body": "$body",
          "assets": [
            {"name": "$assetName", "size": 1234,
             "browser_download_url": "https://github.com/o/r/releases/download/dev-latest/$assetName"}
          ],
          "author": {"login": "github-actions[bot]"}
        }
    """.trimIndent()

    @Test
    fun parsesVersionCodeNotesAndApk() {
        val parsed = parseDevRelease(release())!!
        assertEquals(57L, parsed.versionCode)
        assertEquals("화면 디자인 선택 추가\n\n- 버그 수정", parsed.notes)
        assertEquals(1234L, parsed.apkSize)
        assertEquals("https://github.com/o/r/releases/download/dev-latest/piggybank.apk", parsed.apkUrl)
    }

    @Test
    fun acceptsWindowsLineEndingsAndNoNotes() {
        assertEquals(8L, parseDevRelease(release(body = "versionCode: 8\\r\\n"))!!.versionCode)
        assertEquals("", parseDevRelease(release(body = "versionCode: 8"))!!.notes)
    }

    @Test
    fun ignoresReleaseWithoutVersionCodeOrApk() {
        assertNull(parseDevRelease(release(body = "그냥 설명")))
        assertNull(parseDevRelease(release(body = "변경\\nversionCode: 3")))
        assertNull(parseDevRelease(release(assetName = "notes.txt")))
        assertNull(parseDevRelease(release(draft = true)))
    }
}
