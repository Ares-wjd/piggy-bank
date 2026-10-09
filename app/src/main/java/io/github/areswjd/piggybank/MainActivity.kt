package io.github.areswjd.piggybank

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.areswjd.piggybank.ui.PiggyBankRoot
import io.github.areswjd.piggybank.ui.theme.PiggyBankTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferences = (application as PiggyBankApplication).container.preferences
        // 고른 디자인으로 바로 그리도록 처음 한 번은 기다려서 읽는다(아주 짧음). 이후 바뀌면 즉시 반영된다.
        val initialDesign = runBlocking { preferences.appDesign.first() }
        setContent {
            val design by preferences.appDesign.collectAsStateWithLifecycle(initialValue = initialDesign)
            PiggyBankTheme(design = design) {
                PiggyBankRoot()
            }
        }
    }
}
