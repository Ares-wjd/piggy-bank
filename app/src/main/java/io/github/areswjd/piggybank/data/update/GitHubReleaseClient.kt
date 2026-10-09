package io.github.areswjd.piggybank.data.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** GitHub 릴리스 dev-latest에 올라온 빌드 하나. */
data class AppRelease(
    val versionCode: Long,
    val notes: String,
    val apkUrl: String,
    val apkSize: Long,
)

/** CI가 빌드할 때마다 새로 만드는 릴리스의 태그. 설치 링크: …/releases/download/dev-latest/piggybank.apk */
const val DEV_RELEASE_TAG = "dev-latest"

@Serializable
private data class ReleaseDto(
    val body: String? = null,
    val draft: Boolean = false,
    val assets: List<AssetDto> = emptyList(),
)

@Serializable
private data class AssetDto(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    val size: Long = 0,
)

private val json = Json { ignoreUnknownKeys = true }
private val versionCodeLine = Regex("""^\s*versionCode:\s*(\d+)\s*$""")

/**
 * 릴리스 응답을 읽는다. 설명 첫 줄의 "versionCode: N"이 빌드 번호, 둘째 줄부터가 변경 내용이다.
 * 빌드 번호나 .apk 첨부가 없으면(아직 올라가는 중 등) null.
 */
internal fun parseDevRelease(text: String): AppRelease? {
    val release = json.decodeFromString(ReleaseDto.serializer(), text)
    if (release.draft) return null
    val lines = release.body.orEmpty().replace("\r\n", "\n").lines()
    val versionCode = lines.firstOrNull()?.let { versionCodeLine.find(it) }?.groupValues?.get(1)?.toLongOrNull()
        ?: return null
    val apk = release.assets.firstOrNull { it.name.endsWith(".apk") } ?: return null
    return AppRelease(
        versionCode = versionCode,
        notes = lines.drop(1).joinToString("\n").trim(),
        apkUrl = apk.downloadUrl,
        apkSize = apk.size,
    )
}

/** 공개 저장소의 GitHub 릴리스에서 최신 빌드를 확인하고 APK를 내려받는다. 로그인이나 토큰은 쓰지 않는다. */
class GitHubReleaseClient(private val repository: String) {

    /** dev-latest 릴리스. 아직 없으면 null. */
    suspend fun devLatest(): AppRelease? = withContext(Dispatchers.IO) {
        val connection = open("https://api.github.com/repos/$repository/releases/tags/$DEV_RELEASE_TAG")
        try {
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK ->
                    parseDevRelease(connection.inputStream.bufferedReader().use { it.readText() })
                HttpURLConnection.HTTP_NOT_FOUND -> null
                else -> throw IOException("GitHub API HTTP $code")
            }
        } finally {
            connection.disconnect()
        }
    }

    /** [url]의 파일을 [target]에 저장한다. 진행률(0~1)을 알 수 없으면 null로 알린다. */
    suspend fun download(url: String, target: File, onProgress: (Float?) -> Unit) = withContext(Dispatchers.IO) {
        val connection = open(url)
        try {
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw IOException("Download HTTP $code")
            val total = connection.contentLengthLong
            var received = 0L
            var lastPercent = -1
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        received += read
                        if (total > 0) {
                            val percent = (received * 100 / total).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent / 100f)
                            }
                        } else {
                            onProgress(null)
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("User-Agent", "PiggyBank-Android")
        }

    private companion object {
        const val TIMEOUT_MS = 30_000
    }
}
