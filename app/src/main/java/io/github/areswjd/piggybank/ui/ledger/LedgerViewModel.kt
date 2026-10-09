package io.github.areswjd.piggybank.ui.ledger

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.areswjd.piggybank.data.preferences.UserPreferences
import io.github.areswjd.piggybank.data.repository.MonthlySummary
import io.github.areswjd.piggybank.data.repository.TransactionRepository
import io.github.areswjd.piggybank.ui.common.DaySection
import io.github.areswjd.piggybank.ui.common.groupByDay
import io.github.areswjd.piggybank.ui.common.ledgerDelta
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth

data class LedgerUiState(
    val month: YearMonth,
    val summary: MonthlySummary = MonthlySummary.EMPTY,
    val summaryHidden: Boolean = false,
    val days: List<DaySection> = emptyList(),
    val loading: Boolean = true,
)

class LedgerViewModel(
    private val transactionRepository: TransactionRepository,
    private val preferences: UserPreferences,
) : ViewModel() {

    private val month = MutableStateFlow(YearMonth.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<LedgerUiState> = combine(
        month.flatMapLatest { m -> transactionRepository.observeMonth(m).map { m to it } },
        preferences.summaryHidden,
    ) { (m, items), hidden ->
        LedgerUiState(
            month = m,
            summary = MonthlySummary.of(items.map { it.transaction }),
            summaryHidden = hidden,
            days = groupByDay(items, ::ledgerDelta),
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LedgerUiState(month.value))

    fun previousMonth() = month.update { it.minusMonths(1) }

    fun nextMonth() = month.update { it.plusMonths(1) }

    fun toggleSummaryHidden() {
        viewModelScope.launch { preferences.setSummaryHidden(!uiState.value.summaryHidden) }
    }
}
