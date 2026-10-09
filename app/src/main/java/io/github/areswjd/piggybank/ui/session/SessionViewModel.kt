package io.github.areswjd.piggybank.ui.session

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.areswjd.piggybank.AppContainer
import io.github.areswjd.piggybank.R
import io.github.areswjd.piggybank.data.backup.BackupOutcome
import io.github.areswjd.piggybank.data.backup.RemoteBackup
import io.github.areswjd.piggybank.data.drive.GoogleDriveAuth
import io.github.areswjd.piggybank.data.session.SessionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 로그인 화면 상태. [restoreOffer]가 있으면 "백업을 불러올까요?" 팝업을 띄운다. */
data class LoginUiState(
    val busy: Boolean = false,
    @StringRes val errorRes: Int? = null,
    val restoreOffer: RemoteBackup? = null,
)

/**
 * 로그인 흐름: 계정 선택 → 드라이브 권한 동의 → 드라이브 백업 확인(있으면 불러올지 묻기) → 로그인 완료.
 */
class SessionViewModel(private val container: AppContainer) : ViewModel() {
    private val session = container.session
    private val auth = container.driveAuth
    private val backupManager = container.backupManager

    val state: StateFlow<SessionState> =
        session.state.stateIn(viewModelScope, SharingStarted.Eagerly, SessionState.Loading)

    var login by mutableStateOf(LoginUiState())
        private set

    private var pendingEmail: String? = null

    private val _consentRequests = Channel<PendingIntent>(Channel.BUFFERED)

    /** 화면이 띄워야 하는 구글 권한 동의 화면. */
    val consentRequests: Flow<PendingIntent> = _consentRequests.receiveAsFlow()

    fun onAccountChosen(email: String) {
        pendingEmail = email
        login = LoginUiState(busy = true)
        viewModelScope.launch {
            guard {
                when (val result = auth.authorize(email)) {
                    is GoogleDriveAuth.Result.Token -> checkRemoteBackup(email)
                    is GoogleDriveAuth.Result.NeedsConsent -> _consentRequests.send(result.pendingIntent)
                }
            }
        }
    }

    fun onConsentResult(resultCode: Int, data: Intent?) {
        val email = pendingEmail
        if (resultCode != Activity.RESULT_OK || email == null) {
            login = LoginUiState()
            return
        }
        viewModelScope.launch {
            guard {
                auth.tokenFromConsentResult(data)
                checkRemoteBackup(email)
            }
        }
    }

    fun onAccountPickerCancelled() {
        login = LoginUiState()
    }

    /** 백업 불러오기 팝업에서 고른 결과. */
    fun onRestoreDecision(restore: Boolean) {
        val email = pendingEmail ?: return
        login = login.copy(busy = true, restoreOffer = null)
        viewModelScope.launch {
            guard {
                if (restore) {
                    val outcome = backupManager.restore(email)
                    if (outcome is BackupOutcome.Failed || outcome == BackupOutcome.NeedsReauth) {
                        login = LoginUiState(errorRes = R.string.settings_restore_error)
                        return@guard
                    }
                }
                finish(email)
            }
        }
    }

    private suspend fun checkRemoteBackup(email: String) {
        val remote = backupManager.findRemoteBackup(email)
        if (remote != null) {
            login = login.copy(busy = false, restoreOffer = remote)
        } else {
            finish(email)
        }
    }

    private suspend fun finish(email: String) {
        session.completeSignIn(email)
        pendingEmail = null
        login = LoginUiState()
    }

    private suspend fun guard(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            login = LoginUiState(errorRes = R.string.login_failed)
        }
    }
}
