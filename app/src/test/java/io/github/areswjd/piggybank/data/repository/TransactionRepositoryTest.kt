package io.github.areswjd.piggybank.data.repository

import io.github.areswjd.piggybank.model.TransactionDraft
import io.github.areswjd.piggybank.model.TransactionType
import io.github.areswjd.piggybank.model.ValidationError
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // SDK 35 이상은 Robolectric에서 JDK 21이 필요해서 34로 고정
class TransactionRepositoryTest : RepositoryTestBase() {

    private val october = YearMonth.of(2026, 10)

    private suspend fun setUpBank(): Pair<Long, Long> {
        val bank = assets.addGroup("은행")
        return assets.addAsset(bank, "국민", 0) to assets.addAsset(bank, "신한", 0)
    }

    @Test
    fun observeMonth_onlyThatMonth_newestDateFirst_thenNewestEntry() = runTest {
        val (kb, _) = setUpBank()
        transactions.add(TransactionDraft(TransactionType.EXPENSE, LocalDate.of(2026, 9, 30), 1, kb, memo = "9월"))
        transactions.add(TransactionDraft(TransactionType.EXPENSE, LocalDate.of(2026, 10, 1), 1, kb, memo = "1일"))
        transactions.add(TransactionDraft(TransactionType.EXPENSE, LocalDate.of(2026, 10, 9), 1, kb, memo = "9일-먼저"))
        transactions.add(TransactionDraft(TransactionType.EXPENSE, LocalDate.of(2026, 10, 9), 1, kb, memo = "9일-나중"))
        transactions.add(TransactionDraft(TransactionType.EXPENSE, LocalDate.of(2026, 10, 31), 1, kb, memo = "31일"))
        transactions.add(TransactionDraft(TransactionType.EXPENSE, LocalDate.of(2026, 11, 1), 1, kb, memo = "11월"))

        val memos = transactions.observeMonth(october).first().map { it.transaction.memo }
        assertEquals(listOf("31일", "9일-나중", "9일-먼저", "1일"), memos)
    }

    @Test
    fun transferDetail_hasBothAssetNames() = runTest {
        val (kb, shinhan) = setUpBank()
        transactions.add(TransactionDraft(TransactionType.TRANSFER, LocalDate.of(2026, 10, 9), 500, kb, shinhan))

        val detail = transactions.observeMonth(october).first().single()
        assertEquals("국민", detail.assetName)
        assertEquals("신한", detail.toAssetName)
        assertEquals("은행", detail.toAssetGroupName)
        assertEquals(false, detail.toAssetDeleted)
    }

    @Test
    fun observeMonthForAsset_includesIncomingTransfers() = runTest {
        val (kb, shinhan) = setUpBank()
        val day = LocalDate.of(2026, 10, 9)
        transactions.add(TransactionDraft(TransactionType.EXPENSE, day, 100, kb))
        transactions.add(TransactionDraft(TransactionType.TRANSFER, day, 200, kb, shinhan))
        transactions.add(TransactionDraft(TransactionType.INCOME, day, 300, shinhan))

        assertEquals(2, transactions.observeMonthForAsset(kb, october).first().size)
        assertEquals(2, transactions.observeMonthForAsset(shinhan, october).first().size)
    }

    @Test
    fun add_rejectsInvalidDraftsAndDeletedAssets() = runTest {
        val (kb, shinhan) = setUpBank()
        val day = LocalDate.of(2026, 10, 9)
        assertFails(ValidationError.AMOUNT_OUT_OF_RANGE) {
            transactions.add(TransactionDraft(TransactionType.EXPENSE, day, 0, kb))
        }
        assertFails(ValidationError.TRANSFER_SAME_ASSET) {
            transactions.add(TransactionDraft(TransactionType.TRANSFER, day, 10, kb, kb))
        }
        assertFails(ValidationError.ASSET_NOT_FOUND) {
            transactions.add(TransactionDraft(TransactionType.EXPENSE, day, 10, 999))
        }
        assets.deleteAsset(shinhan)
        assertFails(ValidationError.ASSET_DELETED) {
            transactions.add(TransactionDraft(TransactionType.INCOME, day, 10, shinhan))
        }
        assertFails(ValidationError.ASSET_DELETED) {
            transactions.add(TransactionDraft(TransactionType.TRANSFER, day, 10, kb, shinhan))
        }
    }

    @Test
    fun update_canKeepItsOwnDeletedAssetButNotSwitchToAnother() = runTest {
        val (kb, shinhan) = setUpBank()
        val cash = assets.addGroup("현금")
        val wallet = assets.addAsset(cash, "지갑", 0)
        val day = LocalDate.of(2026, 10, 9)
        val id = transactions.add(TransactionDraft(TransactionType.INCOME, day, 1_000, kb))
        transactions.add(TransactionDraft(TransactionType.EXPENSE, day, 1_000, kb)) // 국민 잔액 0
        assets.deleteAsset(kb)
        assets.deleteAsset(wallet)

        transactions.update(id, TransactionDraft(TransactionType.INCOME, day, 2_000, kb, memo = " 수정 "))
        val updated = transactions.get(id)!!
        assertEquals(2_000L, updated.amount)
        assertEquals("수정", updated.memo)
        assertTrue(updated.updatedAt > updated.createdAt)

        assertFails(ValidationError.ASSET_DELETED) {
            transactions.update(id, TransactionDraft(TransactionType.INCOME, day, 2_000, wallet))
        }
        transactions.update(id, TransactionDraft(TransactionType.INCOME, day, 2_000, shinhan))
    }

    @Test
    fun update_changingTransferToExpenseClearsTarget() = runTest {
        val (kb, shinhan) = setUpBank()
        val day = LocalDate.of(2026, 10, 9)
        val id = transactions.add(TransactionDraft(TransactionType.TRANSFER, day, 500, kb, shinhan))

        transactions.update(id, TransactionDraft(TransactionType.EXPENSE, day, 500, kb, toAssetId = shinhan))

        assertNull(transactions.get(id)!!.toAssetId)
    }

    @Test
    fun delete_removesTransaction() = runTest {
        val (kb, _) = setUpBank()
        val id = transactions.add(TransactionDraft(TransactionType.EXPENSE, LocalDate.of(2026, 10, 9), 500, kb))
        transactions.delete(id)
        assertNull(transactions.get(id))
        assertFails(ValidationError.TRANSACTION_NOT_FOUND) { transactions.delete(id) }
    }
}
