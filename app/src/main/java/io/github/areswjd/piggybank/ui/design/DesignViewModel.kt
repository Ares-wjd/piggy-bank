package io.github.areswjd.piggybank.ui.design

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.areswjd.piggybank.R
import io.github.areswjd.piggybank.data.icon.AppIconManager
import io.github.areswjd.piggybank.data.preferences.UserPreferences
import io.github.areswjd.piggybank.model.AppDesign
import io.github.areswjd.piggybank.model.AppIcon
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DesignViewModel(
    private val preferences: UserPreferences,
    private val iconManager: AppIconManager,
) : ViewModel() {

    /** 지금 쓰는 테마. */
    val design: StateFlow<AppDesign> =
        preferences.appDesign.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppDesign.DEFAULT)

    /** 지금 홈 화면 아이콘. */
    val icon: StateFlow<AppIcon> = iconManager.icon

    private val _messages = Channel<Int>(Channel.BUFFERED)
    val messages: Flow<Int> = _messages.receiveAsFlow()

    init {
        viewModelScope.launch { iconManager.refresh() }
    }

    fun applyDesign(design: AppDesign) {
        viewModelScope.launch { preferences.setAppDesign(design) }
    }

    fun applyIcon(icon: AppIcon) {
        viewModelScope.launch {
            val message = try {
                iconManager.set(icon)
                R.string.design_icon_changed
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                R.string.design_icon_failed
            }
            _messages.send(message)
        }
    }
}
