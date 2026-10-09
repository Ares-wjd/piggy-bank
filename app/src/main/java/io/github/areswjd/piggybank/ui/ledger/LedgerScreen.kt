package io.github.areswjd.piggybank.ui.ledger

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.areswjd.piggybank.R
import io.github.areswjd.piggybank.ui.components.PlaceholderContent
import io.github.areswjd.piggybank.ui.theme.PiggyBankTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_ledger)) }) },
    ) { padding ->
        PlaceholderContent(Modifier.padding(padding))
    }
}

@Preview
@Composable
private fun LedgerScreenPreview() {
    PiggyBankTheme { LedgerScreen() }
}
