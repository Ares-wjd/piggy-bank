package io.github.areswjd.piggybank.data.repository

import io.github.areswjd.piggybank.model.TransactionDraft
import io.github.areswjd.piggybank.model.TransactionType
import io.github.areswjd.piggybank.model.ValidationError
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // SDK 35 이상은 Robolectric에서 JDK 21이 필요해서 34로 고정
class AssetRepositoryTest : RepositoryTestBase() {

    private val day = LocalDate.of(2026, 10, 9)

    private suspend fun balances(): Map<String, Long> =
        assets.observeAssetTree().first().groups.flatMap { it.assets }.associate { it.name to it.balance }

    @Test
    fun balance_reflectsIncomeExpenseAndTransfers() = runTest {
        val bank = assets.addGroup("은행")
        val kb = assets.addAsset(bank, "국민", initialBalance = 10_000)
        val shinhan = assets.addAsset(bank, "신한", initialBalance = 0)

        transactions.add(TransactionDraft(TransactionType.INCOME, day, 5_000, kb))
        transactions.add(TransactionDraft(TransactionType.EXPENSE, day, 3_000, kb))
        transactions.add(TransactionDraft(TransactionType.TRANSFER, day, 2_000, kb, toAssetId = shinhan))

        assertEquals(mapOf("국민" to 10_000L, "신한" to 2_000L), balances())
        val tree = assets.observeAssetTree().first()
        assertEquals(12_000L, tree.groups.single().subtotal)
        assertEquals(12_000L, tree.total)
    }

    @Test
    fun balance_canGoNegative() = runTest {
        val cash = assets.addGroup("현금")
        val wallet = assets.addAsset(cash, "지갑", initialBalance = 1_000)
        transactions.add(TransactionDraft(TransactionType.EXPENSE, day, 3_000, wallet))
        assertEquals(-2_000L, balances()["지갑"])
    }

    @Test
    fun deleteAsset_blockedUnlessBalanceIsZero() = runTest {
        val cash = assets.addGroup("현금")
        val wallet = assets.addAsset(cash, "지갑", initialBalance = 1_000)
        assertFails(ValidationError.BALANCE_NOT_ZERO) { assets.deleteAsset(wallet) }

        transactions.add(TransactionDraft(TransactionType.EXPENSE, day, 1_000, wallet))
        assets.deleteAsset(wallet)
        assertTrue(assets.observeAssetTree().first().groups.single().assets.isEmpty())
    }

    @Test
    fun deleteAsset_keepsTransactionsWithDeletedMark() = runTest {
        val cash = assets.addGroup("현금")
        val wallet = assets.addAsset(cash, "지갑", initialBalance = 0)
        transactions.add(TransactionDraft(TransactionType.INCOME, day, 1_000, wallet, memo = "용돈"))
        transactions.add(TransactionDraft(TransactionType.EXPENSE, day, 1_000, wallet, memo = "간식"))

        assets.deleteAsset(wallet)

        val history = transactions.observeMonth(YearMonth.of(2026, 10)).first()
        assertEquals(listOf("간식", "용돈"), history.map { it.transaction.memo })
        assertTrue(history.all { it.assetDeleted && it.assetName == "지갑" && it.assetGroupName == "현금" })
    }

    @Test
    fun deleteGroup_onlyWhenNoActiveAssets() = runTest {
        val cash = assets.addGroup("현금")
        val wallet = assets.addAsset(cash, "지갑", initialBalance = 0)
        assertFails(ValidationError.GROUP_NOT_EMPTY) { assets.deleteGroup(cash) }

        assets.deleteAsset(wallet)
        assets.deleteGroup(cash)
        assertTrue(assets.observeAssetTree().first().isEmpty)
        assertFails(ValidationError.GROUP_NOT_FOUND) { assets.addAsset(cash, "새 지갑", 0) }
    }

    @Test
    fun names_areTrimmedAndUniqueAmongActiveItems() = runTest {
        val bank = assets.addGroup("  은행 ")
        assertEquals("은행", assets.observeAssetTree().first().groups.single().group.name)
        assertFails(ValidationError.NAME_DUPLICATE) { assets.addGroup("은행") }
        assertFails(ValidationError.NAME_EMPTY) { assets.addGroup("   ") }
        assertFails(ValidationError.NAME_TOO_LONG) { assets.addGroup("가".repeat(21)) }

        val kb = assets.addAsset(bank, "국민", 0)
        assertFails(ValidationError.NAME_DUPLICATE) { assets.addAsset(bank, "국민", 0) }
        val card = assets.addGroup("증권")
        assets.addAsset(card, "국민", 0) // 다른 그룹이면 같은 이름 가능

        assets.deleteAsset(kb)
        assets.addAsset(bank, "국민", 0) // 삭제된 자산과는 이름이 겹쳐도 된다
    }

    @Test
    fun updateAsset_movesToOtherGroup() = runTest {
        val bank = assets.addGroup("은행")
        val invest = assets.addGroup("투자")
        assets.addAsset(invest, "증권", 0)
        val kb = assets.addAsset(bank, "국민", 100)

        assets.updateAsset(kb, groupId = invest, name = "국민 ISA", initialBalance = 200)

        val tree = assets.observeAssetTree().first()
        assertTrue(tree.groups.first { it.group.id == bank }.assets.isEmpty())
        assertEquals(listOf("증권", "국민 ISA"), tree.groups.first { it.group.id == invest }.assets.map { it.name })
        assertEquals(200L, balances()["국민 ISA"])
    }

    @Test
    fun seedDefaults_createsCashWalletOnlyOnce() = runTest {
        assets.seedDefaultsIfEmpty()
        assets.seedDefaultsIfEmpty()
        val tree = assets.observeAssetTree().first()
        assertEquals(listOf("현금"), tree.groups.map { it.group.name })
        assertEquals(listOf("지갑"), tree.groups.single().assets.map { it.name })
    }
}
