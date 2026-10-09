package io.github.areswjd.piggybank.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.areswjd.piggybank.data.local.entity.AssetGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetGroupDao {
    @Query("SELECT * FROM asset_groups WHERE deletedAt IS NULL ORDER BY sortOrder, id")
    fun observeActive(): Flow<List<AssetGroupEntity>>

    @Query("SELECT * FROM asset_groups WHERE id = :id")
    suspend fun get(id: Long): AssetGroupEntity?

    @Query("SELECT COUNT(*) FROM asset_groups")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM asset_groups WHERE deletedAt IS NULL AND name = :name AND id != :excludeId")
    suspend fun countActiveWithName(name: String, excludeId: Long): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM asset_groups")
    suspend fun maxSortOrder(): Int

    @Insert
    suspend fun insert(group: AssetGroupEntity): Long

    @Update
    suspend fun update(group: AssetGroupEntity)

    @Query("SELECT * FROM asset_groups ORDER BY id")
    suspend fun getAll(): List<AssetGroupEntity>

    @Insert
    suspend fun insertAll(items: List<AssetGroupEntity>)

    @Query("DELETE FROM asset_groups")
    suspend fun deleteAll()
}
