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

/** GitHub Releases에 올라온 앱 버전 하나. */
data class AppRelease(
    val version: String,
    val notes: String,
    val apkUrl: String,
    val apkSize: Long,
)

/** 릴리스에 첨부하는 APK 파일 이름. 설치 링크(…/releases/latest/download/piggybank.apk)도 이 이름을 쓴다. */
const val RELEASE_APK_NAME = "piggybank.apk"

@Serializable
private data class ReleaseDto(
    @SerialName("tag_name") val tagName: String,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<AssetDto> = emptyList(),
)

@Serializable
private data class AssetDto(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    val size: Long = 0,
)

private val json = Json { ignoreUnknownKeys = true }

/** GitHub "latest release" 응답을 읽는다. APK가 아직 첨부되지 않았거나 초안/사전 릴리스면 null. */
internal fun parseLatestRelease(text: String): AppRelease? {
    val release = json.decodeFromString(ReleaseDto.serializer(), text)
    if (release.draft || release.prerelease) return null
    val apk = release.assets.firstOrNull { it.name == RELEASE_APK_NAME } ?: return null
    return AppRelease(
        version = release.tagName.removePrefix("v"),
        notes = release.body.orEmpty().trim(),
        apkUrl = apk.downloadUrl,
        apkSize = apk.size,
    )
}

/** "1.2.10" > "1.2.9" 처럼 숫자 단위로 비교한다. 앞의 v와 "-beta" 같은 꼬리는 무시한다. */
fun isNewerVersion(candidate: String, current: String): Boolean {
    fun parts(version: String) = version.removePrefix("v").substringBefore('-')
        .split('.').map { it.toIntOrNull() ?: 0 }
    val a = parts(candidate)
    val b = parts(current)
    for (i in 0 until maxOf(a.size, b.size)) {
        val diff = a.getOrElse(i) { 0 } - b.getOrElse(i) { 0 }
        if (diff != 0) return diff > 0
    }
    return false
}

/** 공개 저장소의 GitHub Releases에서 최신 버전을 확인하고 APK를 내려받는다. 로그인이나 토큰은 쓰지 않는다. */
class GitHubReleaseClient(private val repository: String) {

    /** 최신 릴리스. 릴리스가 하나도 없으면 null. */
    suspend fun latest(): AppRelease? = withContext(Dispatchers.IO) {
        val connection = open("https://api.github.com/repos/$repository/releases/latest")
        try {
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK ->
                    parseLatestRelease(connection.inputStream.bufferedReader().use { it.readText() })
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
