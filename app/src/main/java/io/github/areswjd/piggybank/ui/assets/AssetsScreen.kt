package io.github.areswjd.piggybank.ui.assets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.areswjd.piggybank.R
import io.github.areswjd.piggybank.data.local.dao.AssetBalance
import io.github.areswjd.piggybank.data.repository.AssetGroupWithAssets
import io.github.areswjd.piggybank.ui.AppViewModelProvider
import io.github.areswjd.piggybank.ui.common.ConfirmDialog
import io.github.areswjd.piggybank.ui.common.HighlightCard
import io.github.areswjd.piggybank.ui.common.PigMascot
import io.github.areswjd.piggybank.ui.common.PiggyCard
import io.github.areswjd.piggybank.ui.common.highlightColors
import io.github.areswjd.piggybank.ui.theme.HighlightStyle
import io.github.areswjd.piggybank.ui.theme.PiggyTheme
import io.github.areswjd.piggybank.util.formatMoney
import kotlinx.coroutines.launch

/** 지금 열려 있는 팝업. */
private sealed interface AssetsDialog {
    data object AddGroup : AssetsDialog
    data class RenameGroup(val id: Long, val name: String) : AssetsDialog
    data class DeleteGroup(val id: Long, val name: String) : AssetsDialog
    data class AddAsset(val groupId: Long) : AssetsDialog
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsScreen(
    onAssetClick: (Long) -> Unit,
    viewModel: AssetsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val tree by viewModel.tree.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var dialog by remember { mutableStateOf<AssetsDialog?>(null) }
    var dialogError by rememberSaveable { mutableStateOf<Int?>(null) }

    fun close() {
        dialog = null
        dialogError = null
    }

    // 팝업 안 입력 작업: 실패하면 팝업에 오류를 띄우고, 성공하면 닫는다.
    val formResult: (Int?) -> Unit = { error ->
        if (error == null) {
            close()
        } else {
            dialogError = error
        }
    }
    // 확인 팝업 작업: 팝업은 바로 닫고, 실패하면 아래 안내줄로 알린다.
    val snackbarResult: (Int?) -> Unit = { error ->
        if (error != null) {
            scope.launch { snackbarHostState.showSnackbar(context.getString(error)) }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tab_assets)) }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val current = tree ?: return@Scaffold
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { TotalCard(current.total) }
            if (current.isEmpty) {
                item {
                    Text(
                        stringResource(R.string.assets_empty),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    )
                }
            }
            items(current.groups, key = { it.group.id }) { group ->
                GroupCard(
                    group = group,
                    onAddAsset = { dialog = AssetsDialog.AddAsset(group.group.id) },
                    onRename = { dialog = AssetsDialog.RenameGroup(group.group.id, group.group.name) },
                    onDelete = { dialog = AssetsDialog.DeleteGroup(group.group.id, group.group.name) },
                    onAssetClick = onAssetClick,
                )
            }
            item {
                OutlinedButton(
                    onClick = { dialog = AssetsDialog.AddGroup },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.add_group), style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }

    when (val d = dialog) {
        AssetsDialog.AddGroup -> GroupNameDialog(
            title = stringResource(R.string.add_group),
            initialName = "",
            errorRes = dialogError,
            onConfirm = { viewModel.addGroup(it, formResult) },
            onDismiss = ::close,
        )
        is AssetsDialog.RenameGroup -> GroupNameDialog(
            title = stringResource(R.string.rename_group),
            initialName = d.name,
            errorRes = dialogError,
            onConfirm = { viewModel.renameGroup(d.id, it, formResult) },
            onDismiss = ::close,
        )
        is AssetsDialog.DeleteGroup -> ConfirmDialog(
            title = stringResource(R.string.delete_group_title, d.name),
            message = null,
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = {
                close()
                viewModel.deleteGroup(d.id, snackbarResult)
            },
            onDismiss = ::close,
        )
        is AssetsDialog.AddAsset -> AssetFormDialog(
            title = stringResource(R.string.add_asset),
            initialName = "",
            initialBalance = 0,
            initialGroupId = d.groupId,
            groups = null,
            errorRes = dialogError,
            onConfirm = { name, groupId, balance -> viewModel.addAsset(groupId, name, balance, formResult) },
            onDismiss = ::close,
        )
        null -> Unit
    }
}

/** 총자산 카드. 진한 색 카드(FILLED) 테마는 캐릭터를 오른쪽에, 나머지는 왼쪽에 둔다. */
@Composable
private fun TotalCard(total: Long) {
    val colors = highlightColors()
    val mascotOnRight = PiggyTheme.style.highlightCard == HighlightStyle.FILLED
    HighlightCard {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!mascotOnRight) {
                PigMascot(56.dp)
                Spacer(Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.total_assets),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.subContent,
                )
                Text(
                    formatMoney(total),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (total < 0) PiggyTheme.colors.expense else colors.content,
                )
            }
            if (mascotOnRight) {
                Spacer(Modifier.width(16.dp))
                PigMascot(56.dp)
            }
        }
    }
}

@Composable
private fun GroupCard(
    group: AssetGroupWithAssets,
    onAddAsset: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAssetClick: (Long) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    PiggyCard {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (PiggyTheme.style.dayHeaderSticker) {
                Text(
                    group.group.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                )
                Spacer(Modifier.weight(1f))
            } else {
                Text(group.group.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            }
            Text(
                formatMoney(group.subtotal),
                style = MaterialTheme.typography.titleSmall,
                color = if (group.subtotal < 0) PiggyTheme.colors.expense else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = onAddAsset) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_asset))
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.action_more))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.rename_group)) },
                        onClick = {
                            menuOpen = false
                            onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete_group)) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        if (group.assets.isEmpty()) {
            Text(
                stringResource(R.string.group_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
        group.assets.forEach { asset -> AssetRow(asset, onClick = { onAssetClick(asset.id) }) }
    }
}

@Composable
private fun AssetRow(asset: AssetBalance, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(asset.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(
            formatMoney(asset.balance),
            style = MaterialTheme.typography.titleSmall,
            color = if (asset.balance < 0) PiggyTheme.colors.expense else MaterialTheme.colorScheme.onSurface,
        )
    }
}
