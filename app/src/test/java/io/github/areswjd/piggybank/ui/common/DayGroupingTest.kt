package io.github.areswjd.piggybank.ui.common

import io.github.areswjd.piggybank.data.local.dao.TransactionDetail
import io.github.areswjd.piggybank.data.local.entity.TransactionEntity
import io.github.areswjd.piggybank.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DayGroupingTest {

    private fun detail(day: Int, type: TransactionType, amount: Long, assetId: Long = 1, toAssetId: Long? = null) =
        TransactionDetail(
            transaction = TransactionEntity(
                type = type,
                date = LocalDate.of(2026, 10, day),
                amount = amount,
                assetId = assetId,
                toAssetId = toAssetId,
                memo = "",
                createdAt = 0,
                updatedAt = 0,
            ),
            assetName = "국민",
            assetGroupName = "은행",
            assetDeleted = false,
            toAssetName = null,
            toAssetGroupName = null,
            toAssetDeleted = false,
        )

    private val items = listOf(
        detail(9, TransactionType.EXPENSE, 12_000),
        detail(9, TransactionType.TRANSFER, 500_000, assetId = 1, toAssetId = 2),
        detail(9, TransactionType.INCOME, 2_000),
        detail(8, TransactionType.INCOME, 3_000_000, assetId = 2),
    )

    @Test
    fun ledgerDays_excludeTransfersFromDailyNet() {
        val days = groupByDay(items, ::ledgerDelta)
        assertEquals(listOf(LocalDate.of(2026, 10, 9), LocalDate.of(2026, 10, 8)), days.map { it.date })
        assertEquals(-10_000L, days[0].net)
        assertEquals(3, days[0].items.size)
        assertEquals(3_000_000L, days[1].net)
    }

    @Test
    fun assetDays_countTransfersByDirection() {
        val fromAsset = groupByDay(items.filter { it.transaction.assetId == 1L }) { assetDelta(it, 1) }
        assertEquals(-12_000L - 500_000L + 2_000L, fromAsset[0].net)

        val toAsset = groupByDay(items.filter { it.transaction.assetId == 2L || it.transaction.toAssetId == 2L }) { assetDelta(it, 2) }
        assertEquals(500_000L, toAsset[0].net)
        assertEquals(3_000_000L, toAsset[1].net)
    }
}
