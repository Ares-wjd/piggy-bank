package io.github.areswjd.piggybank.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.areswjd.piggybank.data.local.AppDatabase
import io.github.areswjd.piggybank.data.repository.AssetRepository
import io.github.areswjd.piggybank.data.repository.TransactionRepository
import io.github.areswjd.piggybank.model.TransactionDraft
import io.github.areswjd.piggybank.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // SDK 35 이상은 Robolectric에서 JDK 21이 필요해서 34로 고정
class LocalBackupStoreTest {
    private lateinit var source: AppDatabase
    private lateinit var target: AppDatabase

    private fun newDb() = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()

    @Before
    fun setUp() {
        source = newDb()
        target = newDb()
    }

    @After
    fun tearDown() {
        source.close()
        target.close()
    }

    @Test
    fun exportThenImport_reproducesData_includingDeletedAssets() = runTest {
        val assets = AssetRepository(source)
        val transactions = TransactionRepository(source)
        val bank = assets.addGroup("은행")
        val kb = assets.addAsset(bank, "국민", 10_000)
        val old = assets.addAsset(bank, "옛날통장", 0)
        val day = LocalDate.of(2026, 10, 9)
        transactions.add(TransactionDraft(TransactionType.INCOME, day, 500, old, memo = "이자"))
        transactions.add(TransactionDraft(TransactionType.TRANSFER, day, 500, old, toAssetId = kb, memo = "옮기기"))
        assets.deleteAsset(old)

        val store = LocalBackupStore(source)
        val json = store.encode(store.export(exportedAt = 42))
        val decoded = LocalBackupStore(target).decode(json)
        assertEquals(42L, decoded.exportedAt)
        LocalBackupStore(target).import(decoded)

        val restoredTree = AssetRepository(target).observeAssetTree().first()
        assertEquals(listOf("국민"), restoredTree.groups.single().assets.map { it.name })
        assertEquals(10_500L, restoredTree.total)

        val history = TransactionRepository(target).observeMonth(YearMonth.of(2026, 10)).first()
        assertEquals(listOf("옮기기", "이자"), history.map { it.transaction.memo })
        assertTrue(history.all { it.assetDeleted && it.assetName == "옛날통장" })
        assertEquals("국민", history.first().toAssetName)
    }

    @Test
    fun import_replacesExistingData() = runTest {
        AssetRepository(source).addGroup("드라이브에 있던 그룹")
        val backup = LocalBackupStore(source).export(exportedAt = 1)

        AssetRepository(target).addGroup("기기에만 있던 그룹")
        LocalBackupStore(target).import(backup)

        val names = AssetRepository(target).observeAssetTree().first().groups.map { it.group.name }
        assertEquals(listOf("드라이브에 있던 그룹"), names)
    }

    @Test
    fun import_rejectsNewerSchemaAndKeepsData() = runTest {
        AssetRepository(target).addGroup("그대로")
        val future = BackupFile(BackupFile.SCHEMA_VERSION + 1, 0, emptyList(), emptyList(), emptyList())
        try {
            LocalBackupStore(target).import(future)
            fail("새 버전 백업은 거부해야 한다")
        } catch (e: UnsupportedBackupException) {
            // 예상한 오류
        }
        assertEquals(listOf("그대로"), AssetRepository(target).observeAssetTree().first().groups.map { it.group.name })
    }

    @Test
    fun decode_ignoresUnknownFields() {
        val json = """{"schemaVersion":1,"exportedAt":5,"groups":[],"assets":[],"transactions":[],"future":"field"}"""
        assertEquals(5L, LocalBackupStore(target).decode(json).exportedAt)
    }
}
