package io.github.areswjd.piggybank

import android.content.Context
import io.github.areswjd.piggybank.data.backup.BackupManager
import io.github.areswjd.piggybank.data.backup.LocalBackupStore
import io.github.areswjd.piggybank.data.drive.DriveClient
import io.github.areswjd.piggybank.data.drive.GoogleDriveAuth
import io.github.areswjd.piggybank.data.local.AppDatabase
import io.github.areswjd.piggybank.data.local.DatabaseFactory
import io.github.areswjd.piggybank.data.preferences.UserPreferences
import io.github.areswjd.piggybank.data.repository.AssetRepository
import io.github.areswjd.piggybank.data.repository.TransactionRepository
import io.github.areswjd.piggybank.data.session.SessionRepository
import io.github.areswjd.piggybank.data.update.GitHubReleaseClient
import io.github.areswjd.piggybank.data.update.UpdateManager

/** 앱 전체에서 함께 쓰는 객체. DB는 처음 쓸 때 연다. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val preferences = UserPreferences(appContext)
    val driveAuth = GoogleDriveAuth(appContext)

    val database: AppDatabase by lazy { DatabaseFactory.createEncrypted(appContext) }

    // 기록이 바뀌면 자동 백업 대상으로 표시한다.
    private val onDataChanged: suspend () -> Unit = { preferences.markChanged() }

    val assetRepository: AssetRepository by lazy { AssetRepository(database, onChanged = onDataChanged) }
    val transactionRepository: TransactionRepository by lazy { TransactionRepository(database, onChanged = onDataChanged) }

    val backupManager: BackupManager by lazy {
        BackupManager(appContext, LocalBackupStore(database), driveAuth, DriveClient(), preferences)
    }
    val session: SessionRepository by lazy { SessionRepository(preferences, assetRepository, backupManager) }

    val updateManager: UpdateManager by lazy {
        UpdateManager(
            appContext,
            GitHubReleaseClient(BuildConfig.UPDATE_REPOSITORY),
            currentVersion = BuildConfig.VERSION_NAME,
            currentVersionCode = BuildConfig.VERSION_CODE.toLong(),
            enabled = BuildConfig.UPDATE_CHECK_ENABLED,
        )
    }
}
