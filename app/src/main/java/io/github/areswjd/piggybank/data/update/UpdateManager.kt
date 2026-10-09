package io.github.areswjd.piggybank.data.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import io.github.areswjd.piggybank.data.preferences.UserPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.IOException

sealed interface UpdateState {
    data object Idle : UpdateState
    data class Available(val release: AppRelease) : UpdateState
    data class Downloading(val release: AppRelease, val progress: Float?) : UpdateState
    data class ReadyToInstall(val release: AppRelease, val apk: File) : UpdateState
    data class Failed(val release: AppRelease) : UpdateState
}

enum class CheckResult { UP_TO_DATE, AVAILABLE, FAILED, DISABLED }

/**
 * 새 버전 확인 → APK 내려받기 → 설치 화면 띄우기.
 * 설치할 때 안드로이드가 서명이 같은지 확인하므로, 다른 키로 서명된 APK는 기존 앱을 덮어쓸 수 없다.
 */
class UpdateManager(
    context: Context,
    private val client: GitHubReleaseClient,
    private val preferences: UserPreferences,
    private val currentVersionCode: Long,
    /** CI에서 고정 키로 서명한 빌드에서만 켠다. 직접 빌드한 앱은 서명이 달라 업데이트를 설치할 수 없다. */
    val enabled: Boolean,
) {
    private val appContext = context.applicationContext
    private val updatesDir = File(appContext.cacheDir, "updates")

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    /**
     * 새 빌드가 있는지 확인한다. 자동 확인([manual] = false)에서는 "이 버전 건너뛰기"로 넘긴 빌드를 다시 묻지 않는다.
     * 설정의 업데이트 확인 버튼([manual] = true)은 건너뛴 빌드도 보여준다.
     */
    suspend fun check(manual: Boolean): CheckResult {
        if (!enabled) return CheckResult.DISABLED
        val release = try {
            client.devLatest()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return CheckResult.FAILED
        }
        if (release == null || release.versionCode <= currentVersionCode) return CheckResult.UP_TO_DATE
        if (!manual && preferences.skippedVersionCode.first() == release.versionCode) return CheckResult.UP_TO_DATE
        if (_state.value !is UpdateState.Downloading) _state.value = UpdateState.Available(release)
        return CheckResult.AVAILABLE
    }

    /** 지금 보여준 빌드를 건너뛴다. 더 새 빌드가 나오면 다시 묻는다. */
    suspend fun skip() {
        val release = (_state.value as? UpdateState.Available)?.release ?: return
        preferences.setSkippedVersionCode(release.versionCode)
        _state.value = UpdateState.Idle
    }

    fun dismiss() {
        _state.value = UpdateState.Idle
    }

    suspend fun download() {
        val release = when (val current = _state.value) {
            is UpdateState.Available -> current.release
            is UpdateState.Failed -> current.release
            else -> return
        }
        _state.value = UpdateState.Downloading(release, 0f)
        try {
            updatesDir.deleteRecursively()
            updatesDir.mkdirs()
            val apk = File(updatesDir, "update.apk")
            client.download(release.apkUrl, apk) { _state.value = UpdateState.Downloading(release, it) }
            verify(release, apk)
            _state.value = UpdateState.ReadyToInstall(release, apk)
        } catch (e: CancellationException) {
            _state.value = UpdateState.Available(release)
            throw e
        } catch (e: Exception) {
            _state.value = UpdateState.Failed(release)
        }
    }

    /** 받은 파일이 온전한지, 이 앱의 더 새 버전이 맞는지 확인한다. 서명 확인은 설치할 때 안드로이드가 한다. */
    private fun verify(release: AppRelease, apk: File) {
        if (release.apkSize > 0 && apk.length() != release.apkSize) throw IOException("Size mismatch")
        val info = appContext.packageManager.getPackageArchiveInfo(apk.path, 0) ?: throw IOException("Not an APK")
        if (info.packageName != appContext.packageName) throw IOException("Different package")
        if (PackageInfoCompat.getLongVersionCode(info) <= currentVersionCode) throw IOException("Not newer")
    }

    /** 설치 화면을 띄우는 Intent. */
    fun installIntent(apk: File): Intent {
        val uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", apk)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /** 이 앱이 다른 앱(업데이트 APK)을 설치해도 되는지. 처음에는 사용자가 설정에서 허용해야 한다. */
    fun canInstallPackages(): Boolean = appContext.packageManager.canRequestPackageInstalls()

    fun onInstallerLaunched() {
        _state.value = UpdateState.Idle
    }
}
