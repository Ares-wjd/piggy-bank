package io.github.areswjd.piggybank.ui.assets

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.areswjd.piggybank.R
import io.github.areswjd.piggybank.data.local.entity.AssetGroupEntity
import io.github.areswjd.piggybank.model.Limits
import io.github.areswjd.piggybank.ui.common.AmountVisualTransformation
import io.github.areswjd.piggybank.util.parseAmountInput
import io.github.areswjd.piggybank.util.sanitizeAmountInput

/** 자산 그룹 이름 입력 (추가/이름 변경). 저장에 실패하면 [errorRes]를 칸 아래에 보여준다. */
@Composable
fun GroupNameDialog(
    title: String,
    initialName: String,
    @StringRes errorRes: Int?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            NameField(
                value = name,
                onValueChange = { name = it },
                labelRes = R.string.group_name,
                placeholderRes = R.string.group_name_hint,
                errorRes = errorRes,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

/**
 * 세부 자산 추가/수정. [groups]를 주면 소속 그룹을 바꿀 수 있다(수정할 때).
 */
@Composable
fun AssetFormDialog(
    title: String,
    initialName: String,
    initialBalance: Long,
    initialGroupId: Long,
    groups: List<AssetGroupEntity>?,
    @StringRes errorRes: Int?,
    onConfirm: (name: String, groupId: Long, initialBalance: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var balanceText by rememberSaveable { mutableStateOf(if (initialBalance == 0L) "" else initialBalance.toString()) }
    var groupId by rememberSaveable { mutableStateOf(initialGroupId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (groups != null) {
                    GroupDropdown(groups, groupId, onSelect = { groupId = it })
                }
                NameField(
                    value = name,
                    onValueChange = { name = it },
                    labelRes = R.string.asset_name,
                    placeholderRes = R.string.asset_name_hint,
                    errorRes = errorRes,
                )
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { input -> sanitizeAmountInput(input, allowNegative = true)?.let { balanceText = it } },
                    label = { Text(stringResource(R.string.initial_balance)) },
                    prefix = { Text(stringResource(R.string.won_symbol) + " ") },
                    placeholder = { Text("0") },
                    supportingText = { Text(stringResource(R.string.initial_balance_help)) },
                    singleLine = true,
                    visualTransformation = AmountVisualTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    shape = MaterialTheme.shapes.medium,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, groupId, parseAmountInput(balanceText) ?: 0L) },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun NameField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes labelRes: Int,
    @StringRes placeholderRes: Int,
    @StringRes errorRes: Int?,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= Limits.NAME_MAX_LENGTH) onValueChange(it) },
        label = { Text(stringResource(labelRes)) },
        placeholder = { Text(stringResource(placeholderRes)) },
        isError = errorRes != null,
        supportingText = if (errorRes != null) {
            { Text(stringResource(errorRes)) }
        } else {
            null
        },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
    )
}

@Composable
private fun GroupDropdown(groups: List<AssetGroupEntity>, selectedId: Long, onSelect: (Long) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { expanded = true },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.asset_group),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(groups.firstOrNull { it.id == selectedId }?.name.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                }
                Icon(Icons.Rounded.ExpandMore, contentDescription = null)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            groups.forEach { group ->
                DropdownMenuItem(
                    text = { Text(group.name) },
                    onClick = {
                        onSelect(group.id)
                        expanded = false
                    },
                )
            }
        }
    }
}
