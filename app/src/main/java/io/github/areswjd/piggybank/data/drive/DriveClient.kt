package io.github.areswjd.piggybank.data.drive

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.util.UUID

/** 드라이브 API가 2xx가 아닌 응답을 줬을 때. 401이면 토큰 만료. */
class DriveHttpException(val code: Int) : IOException("Drive API HTTP $code")

/**
 * Google Drive REST API v3로 앱 전용 폴더(appDataFolder)의 백업 파일 하나를 읽고 쓴다.
 * 무거운 Google API 클라이언트 라이브러리 대신 HttpURLConnection만 쓴다.
 */
class DriveClient {
    private val json = Json { ignoreUnknownKeys = true }

    data class RemoteFile(val id: String, val modifiedTime: Instant?)

    @Serializable
    private data class FileList(val files: List<DriveFile> = emptyList())

    @Serializable
    private data class DriveFile(val id: String, val modifiedTime: String? = null)

    /** 백업 파일을 찾는다. 여러 개면 가장 최근 것. */
    suspend fun findBackup(token: String): RemoteFile? {
        val query = URLEncoder.encode("name = '$BACKUP_FILE_NAME' and trashed = false", "UTF-8")
        val url = "$API/files?spaces=appDataFolder&q=$query&orderBy=modifiedTime%20desc" +
            "&fields=files(id,modifiedTime)&pageSize=10"
        val body = request("GET", url, token)
        val file = json.decodeFromString(FileList.serializer(), body).files.firstOrNull() ?: return null
        return RemoteFile(file.id, file.modifiedTime?.let { runCatching { Instant.parse(it) }.getOrNull() })
    }

    suspend fun download(token: String, fileId: String): String =
        request("GET", "$API/files/$fileId?alt=media", token)

    /** [existingId]가 있으면 그 파일 내용을 바꾸고, 없으면 새로 만든다. */
    suspend fun upload(token: String, existingId: String?, content: String) {
        if (existingId != null) {
            // HttpURLConnection은 PATCH를 지원하지 않아 Google API의 메서드 덮어쓰기 헤더를 쓴다.
            request(
                "POST",
                "$UPLOAD_API/files/$existingId?uploadType=media",
                token,
                body = content.toByteArray(),
                contentType = JSON_TYPE,
                headers = mapOf("X-HTTP-Method-Override" to "PATCH"),
            )
            return
        }
        val boundary = "piggybank-${UUID.randomUUID()}"
        val metadata = """{"name":"$BACKUP_FILE_NAME","parents":["appDataFolder"]}"""
        val multipart = buildString {
            append("--$boundary\r\nContent-Type: $JSON_TYPE\r\n\r\n$metadata\r\n")
            append("--$boundary\r\nContent-Type: $JSON_TYPE\r\n\r\n$content\r\n")
            append("--$boundary--\r\n")
        }
        request(
            "POST",
            "$UPLOAD_API/files?uploadType=multipart&fields=id",
            token,
            body = multipart.toByteArray(),
            contentType = "multipart/related; boundary=$boundary",
        )
    }

    private suspend fun request(
        method: String,
        url: String,
        token: String,
        body: ByteArray? = null,
        contentType: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): String = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Authorization", "Bearer $token")
            headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
            if (body != null) {
                connection.doOutput = true
                contentType?.let { connection.setRequestProperty("Content-Type", it) }
                connection.setFixedLengthStreamingMode(body.size)
                connection.outputStream.use { it.write(body) }
            }
            val code = connection.responseCode
            if (code !in 200..299) throw DriveHttpException(code)
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val BACKUP_FILE_NAME = "piggybank-backup.json"
        private const val API = "https://www.googleapis.com/drive/v3"
        private const val UPLOAD_API = "https://www.googleapis.com/upload/drive/v3"
        private const val JSON_TYPE = "application/json; charset=UTF-8"
        private const val TIMEOUT_MS = 30_000
    }
}
