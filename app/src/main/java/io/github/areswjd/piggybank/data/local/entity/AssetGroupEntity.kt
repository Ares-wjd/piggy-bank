package io.github.areswjd.piggybank.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 자산 그룹 (예: 은행). [deletedAt]이 있으면 삭제된 그룹이다 — 삭제된 자산이 계속 참조하므로 행은 지우지 않는다. */
@Entity(tableName = "asset_groups")
data class AssetGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int,
    val deletedAt: Long? = null,
)
