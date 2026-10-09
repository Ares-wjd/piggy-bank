package io.github.areswjd.piggybank.ui.transaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.areswjd.piggybank.R
import io.github.areswjd.piggybank.data.repository.AssetTree
import io.github.areswjd.piggybank.model.Limits
import io.github.areswjd.piggybank.model.TransactionType
import io.github.areswjd.piggybank.ui.AppViewModelProvider
import io.github.areswjd.piggybank.ui.common.AmountVisualTransformation
import io.github.areswjd.piggybank.ui.common.ConfirmDialog
import io.github.areswjd.piggybank.ui.common.SuffixTransformation
import io.github.areswjd.piggybank.ui.common.assetLabel
import io.github.areswjd.piggybank.ui.common.color
import io.github.areswjd.piggybank.ui.common.label
import io.github.areswjd.piggybank.ui.theme.PiggyTheme
import io.github.areswjd.piggybank.util.formatFullDate
import java.time.LocalDate

private const val MILLIS_PER_DAY = 86_400_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditScreen(
    onDone: () -> Unit,
    viewModel: TransactionEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val form = viewModel.form
    val tree by viewModel.assetTree.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var pickingFor by rememberSaveable { mutableStateOf<AssetSlot?>(null) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                EditEvent.Done -> onDone()
                is EditEvent.Error -> snackbarHostState.showSnackbar(context.getString(event.messageRes))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (viewModel.isEditing) R.string.transaction_edit_title else R.string.transaction_new_title))
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (viewModel.isEditing) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TypeSelector(selected = form.type, onSelect = viewModel::onTypeChange)

            FormRow(
                label = stringResource(R.string.field_date),
                value = formatFullDate(form.date),
                icon = Icons.Rounded.CalendarMonth,
                onClick = { showDatePicker = true },
            )

            if (PiggyTheme.style.amountHero) {
                HeroAmountField(form.amountText, viewModel::onAmountChange)
            } else {
                OutlinedTextField(
                    value = form.amountText,
                    onValueChange = viewModel::onAmountChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.field_amount)) },
                    prefix = { Text(stringResource(R.string.won_symbol) + " ") },
                    textStyle = MaterialTheme.typography.titleLarge,
                    singleLine = true,
                    visualTransformation = AmountVisualTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    shape = MaterialTheme.shapes.medium,
                )
            }

            val noAssets = tree?.isEmpty == true || tree?.groups?.all { it.assets.isEmpty() } == true
            if (form.type == TransactionType.TRANSFER) {
                AssetRow(R.string.field_from_asset, form.assetId, tree, viewModel.originalAssetNames) { pickingFor = AssetSlot.FROM }
                AssetRow(R.string.field_to_asset, form.toAssetId, tree, viewModel.originalAssetNames) { pickingFor = AssetSlot.TO }
            } else {
                AssetRow(R.string.field_asset, form.assetId, tree, viewModel.originalAssetNames) { pickingFor = AssetSlot.FROM }
            }
            if (noAssets && viewModel.loaded) {
                Text(
                    stringResource(R.string.no_assets_yet),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            OutlinedTextField(
                value = form.memo,
                onValueChange = viewModel::onMemoChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.field_memo_hint)) },
                singleLine = true,
                supportingText = { Text("${form.memo.length} / ${Limits.MEMO_MAX_LENGTH}") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = MaterialTheme.shapes.medium,
            )

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = viewModel::save,
                enabled = viewModel.loaded && form.canSave,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Text(stringResource(R.string.action_save), style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = form.date.toEpochDay() * MILLIS_PER_DAY)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { viewModel.onDateChange(LocalDate.ofEpochDay(it / MILLIS_PER_DAY)) }
                    showDatePicker = false
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) {
            DatePicker(state = state)
        }
    }

    pickingFor?.let { slot ->
        val currentTree = tree
        if (currentTree != null) {
            AssetPickerDialog(
                title = stringResource(
                    when {
                        form.type != TransactionType.TRANSFER -> R.string.field_asset
                        slot == AssetSlot.FROM -> R.string.field_from_asset
                        else -> R.string.field_to_asset
                    },
                ),
                tree = currentTree,
                selectedId = if (slot == AssetSlot.FROM) form.assetId else form.toAssetId,
                onSelect = { id ->
                    if (slot == AssetSlot.FROM) viewModel.onAssetChange(id) else viewModel.onToAssetChange(id)
                    pickingFor = null
                },
                onDismiss = { pickingFor = null },
            )
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_transaction_title),
            message = stringResource(R.string.delete_transaction_message),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = {
                confirmDelete = false
                viewModel.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

private enum class AssetSlot { FROM, TO }

/** 금액을 화면 가운데에 아주 크게 보여주는 입력칸 (B. 민트 사탕). */
@Composable
private fun HeroAmountField(value: String, onValueChange: (String) -> Unit) {
    val won = stringResource(R.string.won_unit)
    val textStyle = MaterialTheme.typography.displaySmall.copy(
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.field_amount),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = textStyle,
            singleLine = true,
            visualTransformation = SuffixTransformation(AmountVisualTransformation, " $won"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.Center) {
                    if (value.isEmpty()) {
                        Text("0 $won", style = textStyle.copy(color = MaterialTheme.colorScheme.outline))
                    }
                    inner()
                }
            },
        )
    }
}

/** 상단의 [수입][지출][이체] 선택. 고른 유형의 색으로 칠한다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeSelector(selected: TransactionType, onSelect: (TransactionType) -> Unit) {
    val types = listOf(TransactionType.INCOME, TransactionType.EXPENSE, TransactionType.TRANSFER)
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        types.forEachIndexed { index, type ->
            val color = type.color()
            SegmentedButton(
                selected = type == selected,
                onClick = { onSelect(type) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = color.copy(alpha = 0.16f),
                    activeContentColor = color,
                    activeBorderColor = color,
                ),
                icon = {},
                modifier = Modifier.height(48.dp),
            ) {
                Text(type.label(), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** "라벨  값  아이콘" 형태의 눌러서 고르는 칸. */
@Composable
private fun FormRow(
    label: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(72.dp),
            )
            Text(value, style = MaterialTheme.typography.bodyLarge, color = valueColor, modifier = Modifier.weight(1f))
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AssetRow(
    labelRes: Int,
    assetId: Long?,
    tree: AssetTree?,
    originalNames: Map<Long, AssetName>,
    onClick: () -> Unit,
) {
    val active = tree?.groups?.firstNotNullOfOrNull { group ->
        group.assets.firstOrNull { it.id == assetId }?.let { group.group.name to it.name }
    }
    val original = assetId?.let { originalNames[it] }
    val text = when {
        active != null -> assetLabel(active.first, active.second, deleted = false)
        original != null -> assetLabel(original.groupName, original.name, original.deleted)
        else -> null
    }
    FormRow(
        label = stringResource(labelRes),
        value = text ?: stringResource(R.string.select_asset),
        icon = Icons.Rounded.ExpandMore,
        onClick = onClick,
        valueColor = if (text == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
    )
}
