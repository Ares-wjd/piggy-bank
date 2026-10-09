package io.github.areswjd.piggybank.data.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.areswjd.piggybank.PiggyBankApplication

/** 자동 백업. 네트워크 오류면 몇 번 다시 시도하고, 로그인·권한 문제면 그만둔다(설정 화면에 실패로 표시됨). */
class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val backupManager = (applicationContext as PiggyBankApplication).container.backupManager
        return when (backupManager.backupNow()) {
            is BackupOutcome.Failed -> if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
            else -> Result.success()
        }
    }

    private companion object {
        const val MAX_RETRIES = 3
    }
}
