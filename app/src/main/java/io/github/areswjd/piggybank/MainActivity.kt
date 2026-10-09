package io.github.areswjd.piggybank

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.areswjd.piggybank.ui.PiggyBankRoot
import io.github.areswjd.piggybank.ui.common.LocalAppIcon
import io.github.areswjd.piggybank.ui.theme.PiggyBankTheme
import io.github.areswjd.piggybank.ui.theme.isAlwaysDark
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as PiggyBankApplication).container
        val preferences = container.preferences
        val iconManager = container.appIconManager
        // 고른 디자인·아이콘(앱 속 캐릭터)으로 바로 그리도록 처음 한 번은 기다려서 읽는다(아주 짧음). 이후 바뀌면 즉시 반영된다.
        val initialDesign = runBlocking {
            iconManager.refresh()
            preferences.appDesign.first()
        }
        setContent {
            val design by preferences.appDesign.collectAsStateWithLifecycle(initialValue = initialDesign)
            // 항상 어두운 테마(H. 별밤)는 폰이 밝은 모드여도 상태 표시줄 글자를 밝게 한다.
            val alwaysDark = design.isAlwaysDark()
            LaunchedEffect(alwaysDark) {
                if (alwaysDark) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                        navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                    )
                } else {
                    enableEdgeToEdge()
                }
            }
            val appIcon by iconManager.icon.collectAsStateWithLifecycle()
            PiggyBankTheme(design = design) {
                CompositionLocalProvider(LocalAppIcon provides appIcon) {
                    PiggyBankRoot()
                }
            }
        }
    }
}
