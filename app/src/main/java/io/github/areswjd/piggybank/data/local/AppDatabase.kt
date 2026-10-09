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

/** 스키마를 바꾸면 [version]을 올리고 Migration을 추가한다. 스키마 기록은 app/schemas 에 생성된다. */
@Database(
    entities = [AssetGroupEntity::class, AssetEntity::class, TransactionEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun assetGroupDao(): AssetGroupDao
    abstract fun assetDao(): AssetDao
    abstract fun transactionDao(): TransactionDao
}
