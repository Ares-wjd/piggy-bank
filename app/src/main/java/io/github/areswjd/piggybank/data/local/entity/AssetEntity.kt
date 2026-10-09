package io.github.areswjd.piggybank.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 세부 자산 (예: 은행 > 국민). 금액은 원 단위 정수.
 * [deletedAt]이 있으면 삭제된 자산이다 — 거래 내역에 이름이 남아야 하므로 행은 지우지 않는다.
 */
@Entity(
    tableName = "assets",
    foreignKeys = [
        ForeignKey(
            entity = AssetGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("groupId")],
)
data class AssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val name: String,
    val initialBalance: Long,
    val sortOrder: Int,
    val deletedAt: Long? = null,
)
