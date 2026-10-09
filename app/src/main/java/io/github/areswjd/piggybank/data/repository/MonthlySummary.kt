package io.github.areswjd.piggybank.data.repository

import io.github.areswjd.piggybank.data.local.entity.TransactionEntity
import io.github.areswjd.piggybank.model.TransactionType

/** 월 요약. 이체는 자산 사이의 이동이라 수입·지출에 넣지 않는다. */
data class MonthlySummary(val income: Long, val expense: Long) {
    val net: Long get() = income - expense

    companion object {
        val EMPTY = MonthlySummary(0, 0)

        fun of(transactions: List<TransactionEntity>): MonthlySummary = MonthlySummary(
            income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
            expense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
        )
    }
}
