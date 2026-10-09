package io.github.areswjd.piggybank.data.drive

import android.accounts.Account
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/** 사용자의 동의 화면을 띄워야 해서 백그라운드에서는 진행할 수 없을 때. */
class ReauthRequiredException : Exception("Google Drive authorization requires user consent")

/**
 * 구글 드라이브 앱 전용 폴더(appDataFolder) 접근 권한을 받는다.
 *
 * 클라이언트 시크릿이나 클라이언트 ID를 코드에 두지 않는다. Google Cloud에 등록한 Android OAuth 클라이언트
 * (패키지명 + 서명 SHA-1)로 앱을 식별한다. 설정 방법은 docs/GOOGLE_CLOUD_SETUP.md.
 */
class GoogleDriveAuth(context: Context) {
    private val appContext = context.applicationContext

    sealed interface Result {
        data class Token(val accessToken: String) : Result

        /** 처음 로그인할 때처럼 사용자 동의가 필요하다. 화면에서 [pendingIntent]를 띄운다. */
        data class NeedsConsent(val pendingIntent: PendingIntent) : Result
    }

    private fun request(email: String) = AuthorizationRequest.builder()
        .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
        .setAccount(Account(email, GOOGLE_ACCOUNT_TYPE))
        .build()

    suspend fun authorize(email: String): Result {
        val result = Identity.getAuthorizationClient(appContext).authorize(request(email)).await()
        val pendingIntent = result.pendingIntent
        if (result.hasResolution() && pendingIntent != null) return Result.NeedsConsent(pendingIntent)
        return Result.Token(checkNotNull(result.accessToken) { "No access token" })
    }

    /** 동의 화면 없이 토큰을 받는다(자동 백업 등). 동의가 필요하면 [ReauthRequiredException]. */
    suspend fun authorizeSilently(email: String): String = when (val result = authorize(email)) {
        is Result.Token -> result.accessToken
        is Result.NeedsConsent -> throw ReauthRequiredException()
    }

    /** 동의 화면의 결과에서 토큰을 꺼낸다. */
    fun tokenFromConsentResult(data: Intent?): String {
        val result = Identity.getAuthorizationClient(appContext).getAuthorizationResultFromIntent(data)
        return checkNotNull(result.accessToken) { "No access token" }
    }

    /** 만료된 토큰을 캐시에서 지워 다음 요청 때 새 토큰을 받게 한다. */
    suspend fun clearToken(token: String) {
        withContext(Dispatchers.IO) { runCatching { GoogleAuthUtil.clearToken(appContext, token) } }
    }

    companion object {
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        private const val GOOGLE_ACCOUNT_TYPE = "com.google"
    }
}
