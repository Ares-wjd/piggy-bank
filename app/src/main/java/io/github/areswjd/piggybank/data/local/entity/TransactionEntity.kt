package io.github.areswjd.piggybank.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.areswjd.piggybank.model.TransactionType
import java.time.LocalDate

/**
 * 거래 한 건. [assetId]는 수입/지출 대상이자 이체의 출금 자산, [toAssetId]는 이체의 입금 자산이다.
 * 금액은 원 단위 정수(1 이상).
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["assetId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = AssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["toAssetId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("date"), Index("assetId"), Index("toAssetId")],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    val date: LocalDate,
    val amount: Long,
    val assetId: Long,
    val toAssetId: Long?,
    val memo: String,
    val createdAt: Long,
    val updatedAt: Long,
)
