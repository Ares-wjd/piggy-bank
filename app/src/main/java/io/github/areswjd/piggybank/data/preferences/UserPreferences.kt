package io.github.areswjd.piggybank.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

/** 백업 상태. [hasPendingChanges]는 마지막 백업 이후 바뀐 기록이 있는지. */
data class BackupStatus(
    val lastBackupAt: Long?,
    val lastBackupFailed: Boolean,
    val hasPendingChanges: Boolean,
)

/** 화면 설정과 로그인·백업 상태. 금액이나 내용 같은 가계부 데이터는 여기에 두지 않는다. */
class UserPreferences(context: Context) {
    private val dataStore = context.applicationContext.dataStore

    /** 로그인한 구글 계정 이메일. null이면 로그아웃 상태. */
    val accountEmail: Flow<String?> = dataStore.data.map { it[ACCOUNT_EMAIL] }

    val summaryHidden: Flow<Boolean> = dataStore.data.map { it[SUMMARY_HIDDEN] ?: false }

    val backupStatus: Flow<BackupStatus> = dataStore.data.map {
        BackupStatus(
            lastBackupAt = it[LAST_BACKUP_AT],
            lastBackupFailed = it[LAST_BACKUP_FAILED] ?: false,
            hasPendingChanges = (it[CHANGE_VERSION] ?: 0L) > (it[BACKED_UP_VERSION] ?: 0L),
        )
    }

    suspend fun accountEmailNow(): String? = accountEmail.first()

    suspend fun lastAssetId(): Long? = dataStore.data.first()[LAST_ASSET_ID]

    suspend fun setAccountEmail(email: String?) {
        dataStore.edit { if (email == null) it.remove(ACCOUNT_EMAIL) else it[ACCOUNT_EMAIL] = email }
    }

    suspend fun setSummaryHidden(hidden: Boolean) {
        dataStore.edit { it[SUMMARY_HIDDEN] = hidden }
    }

    suspend fun setLastAssetId(id: Long) {
        dataStore.edit { it[LAST_ASSET_ID] = id }
    }

    /** 기록이 바뀔 때마다 부른다. */
    suspend fun markChanged() {
        dataStore.edit { it[CHANGE_VERSION] = (it[CHANGE_VERSION] ?: 0L) + 1 }
    }

    suspend fun changeVersion(): Long = dataStore.data.first()[CHANGE_VERSION] ?: 0L

    /** [version]까지의 변경을 백업했다고 기록한다. 백업하는 동안 생긴 변경은 다음 백업 대상으로 남는다. */
    suspend fun markBackedUp(version: Long, at: Long) {
        dataStore.edit {
            it[BACKED_UP_VERSION] = maxOf(version, it[BACKED_UP_VERSION] ?: 0L)
            it[LAST_BACKUP_AT] = at
            it[LAST_BACKUP_FAILED] = false
        }
    }

    suspend fun markBackupFailed() {
        dataStore.edit { it[LAST_BACKUP_FAILED] = true }
    }

    /** 복원 직후: 기기 데이터가 드라이브와 같으므로 밀린 변경이 없는 상태로 맞춘다. */
    suspend fun markRestored(backupAt: Long) {
        dataStore.edit {
            val version = it[CHANGE_VERSION] ?: 0L
            it[BACKED_UP_VERSION] = version
            it[LAST_BACKUP_AT] = backupAt
            it[LAST_BACKUP_FAILED] = false
        }
    }

    private companion object {
        val ACCOUNT_EMAIL = stringPreferencesKey("account_email")
        val SUMMARY_HIDDEN = booleanPreferencesKey("summary_hidden")
        val LAST_ASSET_ID = longPreferencesKey("last_asset_id")
        val CHANGE_VERSION = longPreferencesKey("change_version")
        val BACKED_UP_VERSION = longPreferencesKey("backed_up_version")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        val LAST_BACKUP_FAILED = booleanPreferencesKey("last_backup_failed")
    }
}
