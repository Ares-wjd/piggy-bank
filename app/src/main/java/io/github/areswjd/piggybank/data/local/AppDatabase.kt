package io.github.areswjd.piggybank.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.areswjd.piggybank.data.local.dao.AssetDao
import io.github.areswjd.piggybank.data.local.dao.AssetGroupDao
import io.github.areswjd.piggybank.data.local.dao.TransactionDao
import io.github.areswjd.piggybank.data.local.entity.AssetEntity
import io.github.areswjd.piggybank.data.local.entity.AssetGroupEntity
import io.github.areswjd.piggybank.data.local.entity.TransactionEntity

// TODO(배포 준비): 첫 배포 전에 exportSchema를 켜고 스키마 JSON을 커밋해 이후 마이그레이션 기준으로 삼는다.
@Database(
    entities = [AssetGroupEntity::class, AssetEntity::class, TransactionEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun assetGroupDao(): AssetGroupDao
    abstract fun assetDao(): AssetDao
    abstract fun transactionDao(): TransactionDao
}
