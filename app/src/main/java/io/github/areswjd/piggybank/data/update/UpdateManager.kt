package io.github.areswjd.piggybank.data.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val currentVersion: String,
    private val currentVersionCode: Long,
    /** 개발용(디버그) 빌드에서는 끈다 — 패키지 이름과 서명이 달라 릴리스로 업데이트할 수 없다. */
    val enabled: Boolean,
) {
    private val appContext = context.applicationContext
    private val updatesDir = File(appContext.cacheDir, "updates")

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    suspend fun check(): CheckResult {
        if (!enabled) return CheckResult.DISABLED
        val release = try {
            client.latest()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return CheckResult.FAILED
        }
        if (release == null || !isNewerVersion(release.version, currentVersion)) return CheckResult.UP_TO_DATE
        if (_state.value !is UpdateState.Downloading) _state.value = UpdateState.Available(release)
        return CheckResult.AVAILABLE
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
            val apk = File(updatesDir, RELEASE_APK_NAME)
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
