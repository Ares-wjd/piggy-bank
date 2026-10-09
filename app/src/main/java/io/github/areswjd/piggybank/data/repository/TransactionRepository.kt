package io.github.areswjd.piggybank.data.repository

import androidx.room.withTransaction
import io.github.areswjd.piggybank.data.local.AppDatabase
import io.github.areswjd.piggybank.data.local.dao.TransactionDetail
import io.github.areswjd.piggybank.data.local.entity.TransactionEntity
import io.github.areswjd.piggybank.model.TransactionDraft
import io.github.areswjd.piggybank.model.TransactionType
import io.github.areswjd.piggybank.model.ValidationError
import io.github.areswjd.piggybank.model.ValidationException
import io.github.areswjd.piggybank.model.validateTransaction
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth

/** 수입·지출·이체 거래. 규칙을 어기면 [ValidationException]을 던진다. */
class TransactionRepository(
    private val db: AppDatabase,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val transactionDao = db.transactionDao()
    private val assetDao = db.assetDao()

    fun observeMonth(month: YearMonth): Flow<List<TransactionDetail>> =
        transactionDao.observeBetween(month.atDay(1), month.atEndOfMonth())

    fun observeMonthForAsset(assetId: Long, month: YearMonth): Flow<List<TransactionDetail>> =
        transactionDao.observeForAssetBetween(assetId, month.atDay(1), month.atEndOfMonth())

    suspend fun get(id: Long): TransactionEntity? = transactionDao.get(id)

    suspend fun add(draft: TransactionDraft): Long = db.withTransaction {
        val normalized = normalize(draft)
        requireUsableAssets(normalized, keepDeleted = emptySet())
        val now = clock()
        transactionDao.insert(
            TransactionEntity(
                type = normalized.type,
                date = normalized.date,
                amount = normalized.amount,
                assetId = normalized.assetId,
                toAssetId = normalized.toAssetId,
                memo = normalized.memo,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    /** 유형까지 바꿀 수 있다. 원래 쓰던 자산이 그사이 삭제됐다면 그 자산은 계속 쓸 수 있다. */
    suspend fun update(id: Long, draft: TransactionDraft) = db.withTransaction {
        val existing = transactionDao.get(id) ?: fail(ValidationError.TRANSACTION_NOT_FOUND)
        val normalized = normalize(draft)
        requireUsableAssets(normalized, keepDeleted = setOfNotNull(existing.assetId, existing.toAssetId))
        transactionDao.update(
            existing.copy(
                type = normalized.type,
                date = normalized.date,
                amount = normalized.amount,
                assetId = normalized.assetId,
                toAssetId = normalized.toAssetId,
                memo = normalized.memo,
                updatedAt = clock(),
            ),
        )
    }

    suspend fun delete(id: Long) {
        if (transactionDao.delete(id) == 0) fail(ValidationError.TRANSACTION_NOT_FOUND)
    }

    /** 내용 앞뒤 공백을 지우고, 이체가 아니면 입금 자산을 비운 뒤 검사한다. */
    private fun normalize(draft: TransactionDraft): TransactionDraft {
        val normalized = draft.copy(
            memo = draft.memo.trim(),
            toAssetId = if (draft.type == TransactionType.TRANSFER) draft.toAssetId else null,
        )
        validateTransaction(normalized)?.let(::fail)
        return normalized
    }

    private suspend fun requireUsableAssets(draft: TransactionDraft, keepDeleted: Set<Long>) {
        listOfNotNull(draft.assetId, draft.toAssetId).forEach { assetId ->
            val asset = assetDao.get(assetId) ?: fail(ValidationError.ASSET_NOT_FOUND)
            if (asset.deletedAt != null && assetId !in keepDeleted) fail(ValidationError.ASSET_DELETED)
        }
    }
}
