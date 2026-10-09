package io.github.areswjd.piggybank.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.areswjd.piggybank.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** 목록에 보여줄 거래 + 자산·그룹 이름. 삭제된 자산도 이름은 그대로 나온다. */
data class TransactionDetail(
    @Embedded val transaction: TransactionEntity,
    val assetName: String,
    val assetGroupName: String,
    val assetDeleted: Boolean,
    val toAssetName: String?,
    val toAssetGroupName: String?,
    val toAssetDeleted: Boolean,
)

private const val DETAIL_SELECT = """
    SELECT t.*,
           fa.name AS assetName, fg.name AS assetGroupName, (fa.deletedAt IS NOT NULL) AS assetDeleted,
           ta.name AS toAssetName, tg.name AS toAssetGroupName, (ta.deletedAt IS NOT NULL) AS toAssetDeleted
    FROM transactions t
    JOIN assets fa ON fa.id = t.assetId
    JOIN asset_groups fg ON fg.id = fa.groupId
    LEFT JOIN assets ta ON ta.id = t.toAssetId
    LEFT JOIN asset_groups tg ON tg.id = ta.groupId
"""

/** 최신 날짜부터, 같은 날짜 안에서는 최근 입력부터. */
private const val DETAIL_ORDER = " ORDER BY t.date DESC, t.createdAt DESC, t.id DESC"

@Dao
interface TransactionDao {
    @Query("$DETAIL_SELECT WHERE t.date BETWEEN :start AND :end $DETAIL_ORDER")
    fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<TransactionDetail>>

    @Query(
        "$DETAIL_SELECT WHERE (t.assetId = :assetId OR t.toAssetId = :assetId) " +
            "AND t.date BETWEEN :start AND :end $DETAIL_ORDER",
    )
    fun observeForAssetBetween(assetId: Long, start: LocalDate, end: LocalDate): Flow<List<TransactionDetail>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun get(id: Long): TransactionEntity?

    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long): Int

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
