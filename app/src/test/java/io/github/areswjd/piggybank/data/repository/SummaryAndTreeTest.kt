package io.github.areswjd.piggybank.data.repository

import io.github.areswjd.piggybank.data.local.dao.AssetBalance
import io.github.areswjd.piggybank.data.local.entity.AssetGroupEntity
import io.github.areswjd.piggybank.data.local.entity.TransactionEntity
import io.github.areswjd.piggybank.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class SummaryAndTreeTest {

    private fun tx(type: TransactionType, amount: Long) = TransactionEntity(
        type = type,
        date = LocalDate.of(2026, 10, 1),
        amount = amount,
        assetId = 1,
        toAssetId = if (type == TransactionType.TRANSFER) 2 else null,
        memo = "",
        createdAt = 0,
        updatedAt = 0,
    )

    @Test
    fun monthlySummary_excludesTransfers() {
        val summary = MonthlySummary.of(
            listOf(
                tx(TransactionType.INCOME, 3_000_000),
                tx(TransactionType.EXPENSE, 12_000),
                tx(TransactionType.EXPENSE, 440_000),
                tx(TransactionType.TRANSFER, 500_000),
            ),
        )
        assertEquals(3_000_000L, summary.income)
        assertEquals(452_000L, summary.expense)
        assertEquals(2_548_000L, summary.net)
    }

    @Test
    fun assetTree_groupsAssetsInGroupOrderAndSums() {
        val bank = AssetGroupEntity(id = 1, name = "은행", sortOrder = 0)
        val cash = AssetGroupEntity(id = 2, name = "현금", sortOrder = 1)
        val tree = buildAssetTree(
            groups = listOf(bank, cash),
            assets = listOf(
                AssetBalance(id = 10, groupId = 1, name = "국민", initialBalance = 0, sortOrder = 0, balance = 10_000_000),
                AssetBalance(id = 11, groupId = 1, name = "신한", initialBalance = 0, sortOrder = 1, balance = 4_800_000),
                AssetBalance(id = 20, groupId = 2, name = "지갑", initialBalance = 0, sortOrder = 0, balance = 520_000),
                AssetBalance(id = 99, groupId = 3, name = "고아", initialBalance = 0, sortOrder = 0, balance = 1),
            ),
        )
        assertEquals(listOf("은행", "현금"), tree.groups.map { it.group.name })
        assertEquals(listOf("국민", "신한"), tree.groups[0].assets.map { it.name })
        assertEquals(14_800_000L, tree.groups[0].subtotal)
        assertEquals(520_000L, tree.groups[1].subtotal)
        assertEquals(15_320_000L, tree.total)
    }
}
