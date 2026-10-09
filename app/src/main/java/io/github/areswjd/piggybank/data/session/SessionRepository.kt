package io.github.areswjd.piggybank.data.session

import io.github.areswjd.piggybank.data.backup.BackupManager
import io.github.areswjd.piggybank.data.preferences.UserPreferences
import io.github.areswjd.piggybank.data.repository.AssetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val email: String) : SessionState
}

/** 로그인 상태. 로그인은 필수이며, 로그아웃해도 기기 데이터는 지우지 않는다. */
class SessionRepository(
    private val preferences: UserPreferences,
    private val assetRepository: AssetRepository,
    private val backupManager: BackupManager,
) {
    val state: Flow<SessionState> = preferences.accountEmail.map { email ->
        if (email == null) SessionState.SignedOut else SessionState.SignedIn(email)
    }

    /** 로그인 마무리: 기본 자산을 준비하고 계정을 저장한다. 이때부터 메인 화면이 열린다. */
    suspend fun completeSignIn(email: String) {
        assetRepository.seedDefaultsIfEmpty()
        preferences.setAccountEmail(email)
    }

    suspend fun signOut() {
        backupManager.cancelScheduled()
        preferences.setAccountEmail(null)
    }
}
