package io.github.areswjd.piggybank.data.backup

import kotlinx.serialization.Serializable

/**
 * 드라이브에 올리는 백업 파일(JSON). 필드를 바꾸면 [SCHEMA_VERSION]을 올리고 이전 버전도 읽을 수 있게 한다.
 * 날짜는 "2026-10-09" 형식, 거래 유형은 INCOME/EXPENSE/TRANSFER.
 */
@Serializable
data class BackupFile(
    val schemaVersion: Int,
    val exportedAt: Long,
    val groups: List<BackupGroup>,
    val assets: List<BackupAsset>,
    val transactions: List<BackupTransaction>,
) {
    companion object {
        const val SCHEMA_VERSION = 1
    }
}

@Serializable
data class BackupGroup(
    val id: Long,
    val name: String,
    val sortOrder: Int,
    val deletedAt: Long? = null,
)

@Serializable
data class BackupAsset(
    val id: Long,
    val groupId: Long,
    val name: String,
    val initialBalance: Long,
    val sortOrder: Int,
    val deletedAt: Long? = null,
)

@Serializable
data class BackupTransaction(
    val id: Long,
    val type: String,
    val date: String,
    val amount: Long,
    val assetId: Long,
    val toAssetId: Long? = null,
    val memo: String = "",
    val createdAt: Long,
    val updatedAt: Long,
)
