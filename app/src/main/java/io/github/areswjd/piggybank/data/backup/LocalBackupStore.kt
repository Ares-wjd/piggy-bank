package io.github.areswjd.piggybank.data.backup

import androidx.room.withTransaction
import io.github.areswjd.piggybank.data.local.AppDatabase
import io.github.areswjd.piggybank.data.local.entity.AssetEntity
import io.github.areswjd.piggybank.data.local.entity.AssetGroupEntity
import io.github.areswjd.piggybank.data.local.entity.TransactionEntity
import io.github.areswjd.piggybank.model.TransactionType
import kotlinx.serialization.json.Json
import java.time.LocalDate

/** 이 앱보다 새 버전에서 만든 백업이라 읽을 수 없을 때. */
class UnsupportedBackupException(version: Int) : Exception("Unsupported backup schema version: $version")

/** 기기 DB ↔ 백업 파일 변환. */
class LocalBackupStore(private val db: AppDatabase) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** DB 전체를 한 시점의 스냅샷으로 꺼낸다. */
    suspend fun export(exportedAt: Long): BackupFile = db.withTransaction {
        BackupFile(
            schemaVersion = BackupFile.SCHEMA_VERSION,
            exportedAt = exportedAt,
            groups = db.assetGroupDao().getAll().map { BackupGroup(it.id, it.name, it.sortOrder, it.deletedAt) },
            assets = db.assetDao().getAll().map {
                BackupAsset(it.id, it.groupId, it.name, it.initialBalance, it.sortOrder, it.deletedAt)
            },
            transactions = db.transactionDao().getAll().map {
                BackupTransaction(
                    id = it.id,
                    type = it.type.name,
                    date = it.date.toString(),
                    amount = it.amount,
                    assetId = it.assetId,
                    toAssetId = it.toAssetId,
                    memo = it.memo,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                )
            },
        )
    }

    /** 기기 DB를 비우고 백업 내용으로 채운다. 중간에 실패하면 원래 데이터가 그대로 남는다. */
    suspend fun import(file: BackupFile) {
        if (file.schemaVersion > BackupFile.SCHEMA_VERSION) throw UnsupportedBackupException(file.schemaVersion)
        val groups = file.groups.map { AssetGroupEntity(it.id, it.name, it.sortOrder, it.deletedAt) }
        val assets = file.assets.map { AssetEntity(it.id, it.groupId, it.name, it.initialBalance, it.sortOrder, it.deletedAt) }
        val transactions = file.transactions.map {
            TransactionEntity(
                id = it.id,
                type = TransactionType.valueOf(it.type),
                date = LocalDate.parse(it.date),
                amount = it.amount,
                assetId = it.assetId,
                toAssetId = it.toAssetId,
                memo = it.memo,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
            )
        }
        db.withTransaction {
            db.transactionDao().deleteAll()
            db.assetDao().deleteAll()
            db.assetGroupDao().deleteAll()
            db.assetGroupDao().insertAll(groups)
            db.assetDao().insertAll(assets)
            db.transactionDao().insertAll(transactions)
        }
    }

    fun encode(file: BackupFile): String = json.encodeToString(BackupFile.serializer(), file)

    fun decode(text: String): BackupFile = json.decodeFromString(BackupFile.serializer(), text)
}
