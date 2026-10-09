package io.github.areswjd.piggybank

import android.content.Context
import io.github.areswjd.piggybank.data.local.AppDatabase
import io.github.areswjd.piggybank.data.local.DatabaseFactory
import io.github.areswjd.piggybank.data.repository.AssetRepository
import io.github.areswjd.piggybank.data.repository.TransactionRepository

/** 앱 전체에서 함께 쓰는 객체. DB는 처음 쓸 때 연다. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase by lazy { DatabaseFactory.createEncrypted(appContext) }
    val assetRepository: AssetRepository by lazy { AssetRepository(database) }
    val transactionRepository: TransactionRepository by lazy { TransactionRepository(database) }
}
