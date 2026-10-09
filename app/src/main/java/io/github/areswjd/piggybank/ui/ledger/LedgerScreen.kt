package io.github.areswjd.piggybank.ui.ledger

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.areswjd.piggybank.R
import io.github.areswjd.piggybank.data.repository.MonthlySummary
import io.github.areswjd.piggybank.ui.AppViewModelProvider
import io.github.areswjd.piggybank.ui.common.DayCard
import io.github.areswjd.piggybank.ui.common.EmptyState
import io.github.areswjd.piggybank.ui.common.MonthSelector
import io.github.areswjd.piggybank.ui.theme.PiggyTheme
import io.github.areswjd.piggybank.util.formatMoney

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    onAddTransaction: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    viewModel: LedgerViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { MonthSelector(state.month, viewModel::previousMonth, viewModel::nextMonth) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_transaction))
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            SummaryBar(
                summary = state.summary,
                hidden = state.summaryHidden,
                onToggleHidden = viewModel::toggleSummaryHidden,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            if (!state.loading && state.days.isEmpty()) {
                EmptyState(stringResource(R.string.ledger_empty))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.days, key = { it.date.toEpochDay() }) { section ->
                        DayCard(section, onTransactionClick)
                    }
                }
            }
        }
    }
}

/** "수입 3,000,000 · 지출 452,000 · 합계 2,548,000 [눈]" 한 줄. 길면 글자를 줄여 한 줄에 맞춘다. */
@Composable
private fun SummaryBar(
    summary: MonthlySummary,
    hidden: Boolean,
    onToggleHidden: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hiddenText = stringResource(R.string.hidden_amount)
    fun amount(value: Long) = if (hidden) hiddenText else formatMoney(value)

    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val incomeLabel = stringResource(R.string.summary_income)
    val expenseLabel = stringResource(R.string.summary_expense)
    val netLabel = stringResource(R.string.summary_net)
    val colors = PiggyTheme.colors
    val netColor = MaterialTheme.colorScheme.onSurface
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = labelColor)) { append("$incomeLabel ") }
        withStyle(SpanStyle(color = colors.income)) { append(amount(summary.income)) }
        withStyle(SpanStyle(color = labelColor)) { append("  ·  $expenseLabel ") }
        withStyle(SpanStyle(color = colors.expense)) { append(amount(summary.expense)) }
        withStyle(SpanStyle(color = labelColor)) { append("  ·  $netLabel ") }
        withStyle(SpanStyle(color = netColor)) { append(amount(summary.net)) }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 15.sp),
            )
            IconButton(onClick = onToggleHidden) {
                Icon(
                    imageVector = if (hidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = stringResource(if (hidden) R.string.summary_show else R.string.summary_hide),
                    tint = labelColor,
                )
            }
        }
    }
}
