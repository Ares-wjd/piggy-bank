package io.github.areswjd.piggybank.ui.settings

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.areswjd.piggybank.R
import io.github.areswjd.piggybank.data.backup.BackupManager
import io.github.areswjd.piggybank.data.backup.BackupOutcome
import io.github.areswjd.piggybank.data.preferences.BackupStatus
import io.github.areswjd.piggybank.data.preferences.UserPreferences
import io.github.areswjd.piggybank.data.session.SessionRepository
import io.github.areswjd.piggybank.data.update.CheckResult
import io.github.areswjd.piggybank.data.update.UpdateManager
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val session: SessionRepository,
    private val backupManager: BackupManager,
    private val updateManager: UpdateManager,
    preferences: UserPreferences,
) : ViewModel() {

    val email: StateFlow<String?> =
        preferences.accountEmail.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val backupStatus: StateFlow<BackupStatus?> =
        preferences.backupStatus.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 백업/복원 중이면 true. 버튼을 막고 진행 표시를 한다. */
    var busy by mutableStateOf(false)
        private set

    private val _messages = Channel<Int>(Channel.BUFFERED)
    val messages: Flow<Int> = _messages.receiveAsFlow()

    fun backupNow() = runBusy {
        message(backupManager.backupNow(), success = R.string.settings_backup_done, failure = R.string.settings_backup_error)
    }

    fun restore() = runBusy {
        val account = email.value ?: return@runBusy
        message(backupManager.restore(account), success = R.string.settings_restore_done, failure = R.string.settings_restore_error)
    }

    /** 새 버전이 있으면 앱 최상단의 업데이트 팝업이 뜬다. 그 밖의 결과만 안내줄로 알린다. */
    fun checkForUpdate() = runBusy {
        val res = when (updateManager.check()) {
            CheckResult.AVAILABLE -> return@runBusy
            CheckResult.UP_TO_DATE -> R.string.settings_up_to_date
            CheckResult.FAILED -> R.string.settings_update_check_failed
            CheckResult.DISABLED -> R.string.settings_update_disabled
        }
        _messages.send(res)
    }

    fun logout() {
        viewModelScope.launch { session.signOut() }
    }

    private suspend fun message(outcome: BackupOutcome, @StringRes success: Int, @StringRes failure: Int) {
        val res = when (outcome) {
            BackupOutcome.Success -> success
            BackupOutcome.NoBackup -> R.string.settings_no_backup
            BackupOutcome.NeedsReauth, BackupOutcome.NotSignedIn -> R.string.settings_reauth_needed
            is BackupOutcome.Failed -> failure
        }
        _messages.send(res)
    }

    private fun runBusy(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try {
                block()
            } finally {
                busy = false
            }
        }
    }
}
