package io.github.areswjd.piggybank.ui.assets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.areswjd.piggybank.R
import io.github.areswjd.piggybank.ui.AppViewModelProvider
import io.github.areswjd.piggybank.ui.common.ConfirmDialog
import io.github.areswjd.piggybank.ui.common.DayCard
import io.github.areswjd.piggybank.ui.common.MonthSelector
import io.github.areswjd.piggybank.ui.theme.PiggyTheme
import io.github.areswjd.piggybank.util.formatMoney
import kotlinx.coroutines.launch

/** 자산 하나의 현재 잔액과 월별 거래 내역. 위쪽 버튼으로 자산을 수정·삭제한다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailScreen(
    onBack: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    viewModel: AssetDetailViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var editing by rememberSaveable { mutableStateOf(false) }
    var editError by rememberSaveable { mutableStateOf<Int?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    // 삭제되면(또는 없는 자산이면) 이전 화면으로 돌아간다.
    LaunchedEffect(state.loading, state.asset) {
        if (!state.loading && state.asset == null) onBack()
    }

    val asset = state.asset
    val group = state.group

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(asset?.name.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (asset != null) {
                        IconButton(onClick = { editing = true }) {
                            Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.edit_asset))
                        }
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = stringResource(R.string.delete_asset))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (asset == null || group == null) return@Scaffold
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "${group.name} > ${asset.name}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            stringResource(R.string.current_balance),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Text(
                            formatMoney(asset.balance),
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (asset.balance < 0) PiggyTheme.colors.expense else MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            item {
                MonthSelector(
                    state.month,
                    viewModel::previousMonth,
                    viewModel::nextMonth,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                )
            }
            if (state.days.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.asset_history_empty),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    )
                }
            }
            items(state.days, key = { it.date.toEpochDay() }) { section ->
                DayCard(section, onTransactionClick, perspectiveAssetId = viewModel.assetId)
            }
        }
    }

    if (editing && asset != null && group != null) {
        AssetFormDialog(
            title = stringResource(R.string.edit_asset),
            initialName = asset.name,
            initialBalance = asset.initialBalance,
            initialGroupId = group.id,
            groups = state.groups,
            errorRes = editError,
            onConfirm = { name, groupId, balance ->
                viewModel.updateAsset(groupId, name, balance) { error ->
                    if (error == null) {
                        editing = false
                        editError = null
                    } else {
                        editError = error
                    }
                }
            },
            onDismiss = {
                editing = false
                editError = null
            },
        )
    }

    if (confirmDelete && asset != null) {
        ConfirmDialog(
            title = stringResource(R.string.delete_asset_title, asset.name),
            message = stringResource(R.string.delete_asset_message),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = {
                confirmDelete = false
                viewModel.deleteAsset { error ->
                    if (error != null) scope.launch { snackbarHostState.showSnackbar(context.getString(error)) }
                }
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
