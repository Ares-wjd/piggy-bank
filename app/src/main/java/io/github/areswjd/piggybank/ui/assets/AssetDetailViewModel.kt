package io.github.areswjd.piggybank.ui.assets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.areswjd.piggybank.data.local.dao.AssetBalance
import io.github.areswjd.piggybank.data.local.entity.AssetGroupEntity
import io.github.areswjd.piggybank.data.repository.AssetRepository
import io.github.areswjd.piggybank.data.repository.TransactionRepository
import io.github.areswjd.piggybank.ui.common.DaySection
import io.github.areswjd.piggybank.ui.common.assetDelta
import io.github.areswjd.piggybank.ui.common.groupByDay
import io.github.areswjd.piggybank.ui.common.messageRes
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

data class AssetDetailUiState(
    val month: YearMonth,
    /** null이면 자산이 없거나(삭제됨) 아직 불러오는 중. */
    val asset: AssetBalance? = null,
    val group: AssetGroupEntity? = null,
    val groups: List<AssetGroupEntity> = emptyList(),
    val days: List<DaySection> = emptyList(),
    val loading: Boolean = true,
)

class AssetDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val assetRepository: AssetRepository,
    transactionRepository: TransactionRepository,
) : ViewModel() {

    val assetId: Long = checkNotNull(savedStateHandle.get<Long>(ARG_ASSET_ID))
    private val month = MutableStateFlow(YearMonth.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AssetDetailUiState> = combine(
        assetRepository.observeAssetTree(),
        month.flatMapLatest { m -> transactionRepository.observeMonthForAsset(assetId, m).map { m to it } },
    ) { tree, (m, items) ->
        val owner = tree.groups.firstOrNull { g -> g.assets.any { it.id == assetId } }
        AssetDetailUiState(
            month = m,
            asset = owner?.assets?.first { it.id == assetId },
            group = owner?.group,
            groups = tree.groups.map { it.group },
            days = groupByDay(items) { assetDelta(it, assetId) },
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AssetDetailUiState(month.value))

    fun previousMonth() = month.update { it.minusMonths(1) }

    fun nextMonth() = month.update { it.plusMonths(1) }

    fun updateAsset(groupId: Long, name: String, initialBalance: Long, onResult: ResultCallback) {
        viewModelScope.launch {
            onResult(runCatching { assetRepository.updateAsset(assetId, groupId, name, initialBalance) }.exceptionOrNull()?.messageRes())
        }
    }

    fun deleteAsset(onResult: ResultCallback) {
        viewModelScope.launch {
            onResult(runCatching { assetRepository.deleteAsset(assetId) }.exceptionOrNull()?.messageRes())
        }
    }

    companion object {
        const val ARG_ASSET_ID = "assetId"
    }
}
