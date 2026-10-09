package io.github.areswjd.piggybank

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.areswjd.piggybank.ui.PiggyBankRoot
import io.github.areswjd.piggybank.ui.theme.PiggyBankTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PiggyBankTheme {
                PiggyBankRoot()
            }
        }
    }
}
