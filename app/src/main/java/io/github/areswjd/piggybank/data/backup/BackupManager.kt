package io.github.areswjd.piggybank.data.backup

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import io.github.areswjd.piggybank.data.drive.DriveClient
import io.github.areswjd.piggybank.data.drive.DriveHttpException
import io.github.areswjd.piggybank.data.drive.GoogleDriveAuth
import io.github.areswjd.piggybank.data.drive.ReauthRequiredException
import io.github.areswjd.piggybank.data.preferences.UserPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.util.concurrent.TimeUnit

sealed interface BackupOutcome {
    data object Success : BackupOutcome
    data object NotSignedIn : BackupOutcome
    data object NoBackup : BackupOutcome
    data object NeedsReauth : BackupOutcome
    data class Failed(val error: Throwable) : BackupOutcome
}

/** 드라이브 백업 파일 정보. */
data class RemoteBackup(val fileId: String, val modifiedTime: Instant?)

/** 기기 DB를 사용자의 구글 드라이브(앱 전용 폴더)에 백업하고 복원한다. */
class BackupManager(
    context: Context,
    private val store: LocalBackupStore,
    private val auth: GoogleDriveAuth,
    private val drive: DriveClient,
    private val preferences: UserPreferences,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val appContext = context.applicationContext
    private val mutex = Mutex()

    /** 지금 기기 데이터를 드라이브에 올린다. 로그인한 계정을 쓴다. */
    suspend fun backupNow(): BackupOutcome = mutex.withLock {
        val email = preferences.accountEmailNow() ?: return@withLock BackupOutcome.NotSignedIn
        val version = preferences.changeVersion()
        val outcome = runDrive(email) { token ->
            val content = store.encode(store.export(clock()))
            drive.upload(token, drive.findBackup(token)?.id, content)
        }
        if (outcome == BackupOutcome.Success) preferences.markBackedUp(version, clock()) else preferences.markBackupFailed()
        outcome
    }

    /** [email] 계정의 드라이브에 백업이 있는지 본다. 네트워크 오류 등은 예외로 던진다. */
    suspend fun findRemoteBackup(email: String): RemoteBackup? = withToken(email) { token ->
        drive.findBackup(token)?.let { RemoteBackup(it.id, it.modifiedTime) }
    }

    /** [email] 계정의 드라이브 백업으로 기기 데이터를 바꾼다. */
    suspend fun restore(email: String): BackupOutcome = mutex.withLock {
        var restoredAt = 0L
        val outcome = runDrive(email) { token ->
            val remote = drive.findBackup(token) ?: return@runDrive BackupOutcome.NoBackup
            val file = store.decode(drive.download(token, remote.id))
            store.import(file)
            restoredAt = remote.modifiedTime?.toEpochMilli() ?: file.exportedAt
            BackupOutcome.Success
        }
        if (outcome == BackupOutcome.Success) preferences.markRestored(restoredAt)
        outcome
    }

    /** 백업하지 않은 변경이 있으면 네트워크가 될 때 백그라운드로 백업한다(앱이 백그라운드로 갈 때 호출). */
    suspend fun scheduleIfPending() {
        if (preferences.accountEmailNow() == null) return
        if (!preferences.backupStatus.first().hasPendingChanges) return
        val request = OneTimeWorkRequestBuilder<BackupWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(AUTO_BACKUP_WORK, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }

    fun cancelScheduled() {
        WorkManager.getInstance(appContext).cancelUniqueWork(AUTO_BACKUP_WORK)
    }

    /** 드라이브 작업을 실행하고 결과를 [BackupOutcome]으로 바꾼다. */
    private suspend fun runDrive(email: String, block: suspend (String) -> Any?): BackupOutcome = try {
        when (val result = withToken(email, block)) {
            is BackupOutcome -> result
            else -> BackupOutcome.Success
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: ReauthRequiredException) {
        BackupOutcome.NeedsReauth
    } catch (e: Exception) {
        // 네트워크 오류, 드라이브 오류, 깨진 백업 파일 등
        BackupOutcome.Failed(e)
    }

    /** 토큰을 받아 [block]을 실행한다. 토큰이 만료돼 401이 오면 한 번 새로 받아 다시 시도한다. */
    private suspend fun <T> withToken(email: String, block: suspend (String) -> T): T {
        val token = auth.authorizeSilently(email)
        return try {
            block(token)
        } catch (e: DriveHttpException) {
            if (e.code != 401) throw e
            auth.clearToken(token)
            block(auth.authorizeSilently(email))
        }
    }

    private companion object {
        const val AUTO_BACKUP_WORK = "auto-backup"
    }
}
