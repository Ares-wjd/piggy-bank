package io.github.areswjd.piggybank.ui.assets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.areswjd.piggybank.data.repository.AssetRepository
import io.github.areswjd.piggybank.data.repository.AssetTree
import io.github.areswjd.piggybank.ui.common.messageRes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 작업 결과를 돌려받는 콜백. 성공하면 null, 실패하면 안내 문구의 문자열 리소스 ID. */
typealias ResultCallback = (Int?) -> Unit

class AssetsViewModel(private val assetRepository: AssetRepository) : ViewModel() {

    /** null이면 아직 불러오는 중. */
    val tree: StateFlow<AssetTree?> =
        assetRepository.observeAssetTree().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun addGroup(name: String, onResult: ResultCallback) = launchWithResult(onResult) { assetRepository.addGroup(name) }

    fun renameGroup(id: Long, name: String, onResult: ResultCallback) =
        launchWithResult(onResult) { assetRepository.renameGroup(id, name) }

    fun deleteGroup(id: Long, onResult: ResultCallback) = launchWithResult(onResult) { assetRepository.deleteGroup(id) }

    fun addAsset(groupId: Long, name: String, initialBalance: Long, onResult: ResultCallback) =
        launchWithResult(onResult) { assetRepository.addAsset(groupId, name, initialBalance) }

    private fun launchWithResult(onResult: ResultCallback, block: suspend () -> Unit) {
        viewModelScope.launch {
            onResult(runCatching { block() }.exceptionOrNull()?.messageRes())
        }
    }
}
