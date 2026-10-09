package io.github.areswjd.piggybank.data.icon

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import io.github.areswjd.piggybank.MainActivity
import io.github.areswjd.piggybank.model.AppIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * 홈 화면 아이콘을 바꾼다. 아이콘마다 매니페스트에 activity-alias가 있고, 고른 것만 켜 둔다.
 * 지금 아이콘은 따로 저장하지 않고 폰에 켜져 있는 alias로 판단한다([refresh]로 읽는다).
 */
class AppIconManager(context: Context) {
    private val packageName = context.packageName
    private val packageManager = context.packageManager

    // alias 이름은 매니페스트 기준(".IconCoin")이라 앱 코드의 패키지 이름을 앞에 붙인다.
    private val codePackage = MainActivity::class.java.name.substringBeforeLast('.')

    private val _icon = MutableStateFlow(AppIcon.DEFAULT)

    /** 지금 홈 화면 아이콘. 처음에는 기본값이고 [refresh] 뒤에 실제 값이 된다. */
    val icon: StateFlow<AppIcon> = _icon.asStateFlow()

    suspend fun refresh() {
        _icon.value = withContext(Dispatchers.IO) { readCurrent() }
    }

    suspend fun set(icon: AppIcon) {
        withContext(Dispatchers.IO) {
            // 새 아이콘을 먼저 켜고 나머지를 끈다. 홈 화면에 아이콘이 하나도 없는 순간이 생기지 않게.
            setEnabled(icon, true)
            AppIcon.entries.filter { it != icon }.forEach { setEnabled(it, false) }
        }
        _icon.value = icon
    }

    private fun readCurrent(): AppIcon =
        AppIcon.entries.firstOrNull { state(it) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED }
            ?: AppIcon.DEFAULT

    /** 매니페스트 기본값(DEFAULT)은 처음 켜져 있는 [AppIcon.DEFAULT]만 켜진 것으로 본다. */
    private fun state(icon: AppIcon): Int {
        val state = packageManager.getComponentEnabledSetting(component(icon))
        if (state != PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) return state
        return if (icon == AppIcon.DEFAULT) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
    }

    private fun setEnabled(icon: AppIcon, enabled: Boolean) {
        val newState = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        packageManager.setComponentEnabledSetting(component(icon), newState, PackageManager.DONT_KILL_APP)
    }

    private fun component(icon: AppIcon) = ComponentName(packageName, codePackage + icon.aliasName)
}
