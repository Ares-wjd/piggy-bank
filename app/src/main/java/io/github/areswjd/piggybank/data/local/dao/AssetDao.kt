package io.github.areswjd.piggybank.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.areswjd.piggybank.data.local.entity.AssetEntity
import kotlinx.coroutines.flow.Flow

/** 활성 자산과 현재 잔액. */
data class AssetBalance(
    val id: Long,
    val groupId: Long,
    val name: String,
    val initialBalance: Long,
    val sortOrder: Int,
    val balance: Long,
)

/**
 * 자산 a의 현재 잔액 = 초기 잔액 + 수입 − 지출 − 이체(출금) + 이체(입금).
 * a가 assetId 쪽이면 수입만 더하고 지출·이체 출금은 뺀다.
 */
private const val BALANCE_EXPR = """
    a.initialBalance
    + COALESCE((SELECT SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE -t.amount END)
                FROM transactions t WHERE t.assetId = a.id), 0)
    + COALESCE((SELECT SUM(t.amount)
                FROM transactions t WHERE t.type = 'TRANSFER' AND t.toAssetId = a.id), 0)
"""

@Dao
interface AssetDao {
    @Query(
        "SELECT a.id, a.groupId, a.name, a.initialBalance, a.sortOrder, ($BALANCE_EXPR) AS balance " +
            "FROM assets a WHERE a.deletedAt IS NULL ORDER BY a.sortOrder, a.id",
    )
    fun observeActiveWithBalance(): Flow<List<AssetBalance>>

    @Query("SELECT ($BALANCE_EXPR) FROM assets a WHERE a.id = :id")
    suspend fun balanceOf(id: Long): Long?

    @Query("SELECT * FROM assets WHERE id = :id")
    suspend fun get(id: Long): AssetEntity?

    @Query("SELECT COUNT(*) FROM assets WHERE deletedAt IS NULL AND groupId = :groupId")
    suspend fun countActiveInGroup(groupId: Long): Int

    @Query(
        "SELECT COUNT(*) FROM assets " +
            "WHERE deletedAt IS NULL AND groupId = :groupId AND name = :name AND id != :excludeId",
    )
    suspend fun countActiveWithName(groupId: Long, name: String, excludeId: Long): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM assets WHERE groupId = :groupId")
    suspend fun maxSortOrder(groupId: Long): Int

    @Insert
    suspend fun insert(asset: AssetEntity): Long

    @Update
    suspend fun update(asset: AssetEntity)

    @Query("SELECT * FROM assets ORDER BY id")
    suspend fun getAll(): List<AssetEntity>

    @Insert
    suspend fun insertAll(items: List<AssetEntity>)

    @Query("DELETE FROM assets")
    suspend fun deleteAll()
}
