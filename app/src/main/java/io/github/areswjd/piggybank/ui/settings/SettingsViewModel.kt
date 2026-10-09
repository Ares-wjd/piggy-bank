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
