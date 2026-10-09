package io.github.areswjd.piggybank.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.areswjd.piggybank.R
import io.github.areswjd.piggybank.data.repository.AssetTree
import io.github.areswjd.piggybank.ui.theme.PiggyTheme
import io.github.areswjd.piggybank.util.formatMoney

/** 그룹별로 묶인 활성 자산 중 하나를 고른다. 각 자산의 현재 잔액도 보여준다. */
@Composable
fun AssetPickerDialog(
    title: String,
    tree: AssetTree,
    selectedId: Long?,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            val groups = tree.groups.filter { it.assets.isNotEmpty() }
            if (groups.isEmpty()) {
                Text(stringResource(R.string.no_assets_yet))
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    groups.forEach { group ->
                        item(key = "g${group.group.id}") {
                            Text(
                                group.group.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 8.dp),
                            )
                        }
                        items(group.assets, key = { "a${it.id}" }) { asset ->
                            val selected = asset.id == selectedId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.small)
                                    .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .clickable { onSelect(asset.id) }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(asset.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Text(
                                    formatMoney(asset.balance),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (asset.balance < 0) PiggyTheme.colors.expense else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
