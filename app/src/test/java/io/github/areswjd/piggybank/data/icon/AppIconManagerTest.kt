package io.github.areswjd.piggybank.data.icon

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import io.github.areswjd.piggybank.model.AppIcon
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppIconManagerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val manager = AppIconManager(context)

    private fun state(icon: AppIcon): Int = context.packageManager.getComponentEnabledSetting(
        ComponentName(context.packageName, "io.github.areswjd.piggybank" + icon.aliasName),
    )

    @Test
    fun `처음에는 기본 아이콘(동전 돼지)`() = runTest {
        manager.refresh()
        assertEquals(AppIcon.COIN, manager.icon.value)
    }

    @Test
    fun `바꾸면 고른 아이콘만 켜진다`() = runTest {
        manager.set(AppIcon.WINK)

        assertEquals(AppIcon.WINK, manager.icon.value)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, state(AppIcon.WINK))
        AppIcon.entries.filter { it != AppIcon.WINK }.forEach {
            assertEquals(it.name, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, state(it))
        }

        // 폰에 켜져 있는 alias를 다시 읽어도 같다.
        AppIconManager(context).run {
            refresh()
            assertEquals(AppIcon.WINK, icon.value)
        }
    }

    @Test
    fun `기본 아이콘으로 되돌릴 수 있다`() = runTest {
        manager.set(AppIcon.GOLD_COIN)
        manager.set(AppIcon.COIN)

        manager.refresh()
        assertEquals(AppIcon.COIN, manager.icon.value)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DISABLED, state(AppIcon.GOLD_COIN))
    }
}
