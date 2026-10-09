package io.github.areswjd.piggybank.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.areswjd.piggybank.data.update.UpdateManager
import io.github.areswjd.piggybank.data.update.UpdateState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class UpdateViewModel(val manager: UpdateManager) : ViewModel() {
    val state: StateFlow<UpdateState> = manager.state

    private var checkedOnLaunch = false

    /** 앱을 켤 때마다 한 번 확인한다(화면 회전 등으로 다시 그려질 때는 건너뜀). */
    fun checkOnLaunch() {
        if (checkedOnLaunch) return
        checkedOnLaunch = true
        viewModelScope.launch { manager.check() }
    }

    fun dismiss() = manager.dismiss()

    fun download() {
        viewModelScope.launch { manager.download() }
    }

    fun onInstallerLaunched() = manager.onInstallerLaunched()
}
