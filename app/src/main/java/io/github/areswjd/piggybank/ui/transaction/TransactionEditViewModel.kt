package io.github.areswjd.piggybank.ui.transaction

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.areswjd.piggybank.data.preferences.UserPreferences
import io.github.areswjd.piggybank.data.repository.AssetRepository
import io.github.areswjd.piggybank.data.repository.AssetTree
import io.github.areswjd.piggybank.data.repository.TransactionRepository
import io.github.areswjd.piggybank.model.Limits
import io.github.areswjd.piggybank.model.TransactionDraft
import io.github.areswjd.piggybank.model.TransactionType
import io.github.areswjd.piggybank.ui.common.messageRes
import io.github.areswjd.piggybank.util.parseAmountInput
import io.github.areswjd.piggybank.util.sanitizeAmountInput
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TransactionForm(
    val type: TransactionType = TransactionType.EXPENSE,
    val date: LocalDate = LocalDate.now(),
    val amountText: String = "",
    val assetId: Long? = null,
    val toAssetId: Long? = null,
    val memo: String = "",
) {
    val amount: Long? get() = parseAmountInput(amountText)

    val canSave: Boolean
        get() {
            val value = amount ?: return false
            if (value !in 1..Limits.AMOUNT_MAX || assetId == null || memo.length > Limits.MEMO_MAX_LENGTH) return false
            return type != TransactionType.TRANSFER || (toAssetId != null && toAssetId != assetId)
        }
}

/** 수정 중인 거래가 원래 쓰던 자산의 이름. 그 자산이 삭제됐어도 화면에 보여줘야 한다. */
data class AssetName(val groupName: String, val name: String, val deleted: Boolean)

sealed interface EditEvent {
    data object Done : EditEvent
    data class Error(@StringRes val messageRes: Int) : EditEvent
}

class TransactionEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val transactionRepository: TransactionRepository,
    private val assetRepository: AssetRepository,
    private val preferences: UserPreferences,
) : ViewModel() {

    private val transactionId: Long? = savedStateHandle.get<Long>(ARG_ID)?.takeIf { it > 0 }
    val isEditing: Boolean = transactionId != null

    var form by mutableStateOf(TransactionForm())
        private set

    var loaded by mutableStateOf(false)
        private set

    /** 원래 거래의 자산 이름 (id → 이름). 삭제된 자산 표시용. */
    var originalAssetNames by mutableStateOf<Map<Long, AssetName>>(emptyMap())
        private set

    val assetTree: StateFlow<AssetTree?> =
        assetRepository.observeAssetTree().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _events = Channel<EditEvent>(Channel.BUFFERED)
    val events: Flow<EditEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            if (transactionId != null) loadExisting(transactionId) else loadDefaults()
            loaded = true
        }
    }

    private suspend fun loadExisting(id: Long) {
        val detail = transactionRepository.getDetail(id)
        if (detail == null) {
            _events.send(EditEvent.Done)
            return
        }
        val tx = detail.transaction
        form = TransactionForm(
            type = tx.type,
            date = tx.date,
            amountText = tx.amount.toString(),
            assetId = tx.assetId,
            toAssetId = tx.toAssetId,
            memo = tx.memo,
        )
        originalAssetNames = buildMap {
            put(tx.assetId, AssetName(detail.assetGroupName, detail.assetName, detail.assetDeleted))
            val toId = tx.toAssetId
            if (toId != null && detail.toAssetName != null && detail.toAssetGroupName != null) {
                put(toId, AssetName(detail.toAssetGroupName, detail.toAssetName, detail.toAssetDeleted))
            }
        }
    }

    /** 새 기록: 마지막으로 쓴 자산이 살아 있으면 그것을, 아니면 첫 자산을 고른다. */
    private suspend fun loadDefaults() {
        val activeIds = assetRepository.observeAssetTree().first().groups.flatMap { g -> g.assets.map { it.id } }
        val last = preferences.lastAssetId()
        form = form.copy(assetId = last?.takeIf { it in activeIds } ?: activeIds.firstOrNull())
    }

    fun onTypeChange(type: TransactionType) {
        form = form.copy(type = type, toAssetId = if (type == TransactionType.TRANSFER) form.toAssetId else null)
    }

    fun onDateChange(date: LocalDate) {
        form = form.copy(date = date)
    }

    fun onAmountChange(text: String) {
        sanitizeAmountInput(text)?.let { form = form.copy(amountText = it) }
    }

    fun onAssetChange(id: Long) {
        form = form.copy(assetId = id)
    }

    fun onToAssetChange(id: Long) {
        form = form.copy(toAssetId = id)
    }

    fun onMemoChange(memo: String) {
        if (memo.length <= Limits.MEMO_MAX_LENGTH) form = form.copy(memo = memo)
    }

    fun save() {
        val current = form
        val amount = current.amount ?: return
        val assetId = current.assetId ?: return
        val draft = TransactionDraft(current.type, current.date, amount, assetId, current.toAssetId, current.memo)
        viewModelScope.launch {
            runCatching {
                if (transactionId == null) transactionRepository.add(draft) else transactionRepository.update(transactionId, draft)
            }.onSuccess {
                preferences.setLastAssetId(assetId)
                _events.send(EditEvent.Done)
            }.onFailure {
                _events.send(EditEvent.Error(it.messageRes()))
            }
        }
    }

    fun delete() {
        val id = transactionId ?: return
        viewModelScope.launch {
            runCatching { transactionRepository.delete(id) }
                .onSuccess { _events.send(EditEvent.Done) }
                .onFailure { _events.send(EditEvent.Error(it.messageRes())) }
        }
    }

    companion object {
        const val ARG_ID = "id"
    }
}
