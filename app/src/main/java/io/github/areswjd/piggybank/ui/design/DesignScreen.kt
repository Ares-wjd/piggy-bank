package io.github.areswjd.piggybank.ui.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.areswjd.piggybank.R
import io.github.areswjd.piggybank.data.local.dao.TransactionDetail
import io.github.areswjd.piggybank.data.local.entity.TransactionEntity
import io.github.areswjd.piggybank.data.repository.MonthlySummary
import io.github.areswjd.piggybank.model.AppDesign
import io.github.areswjd.piggybank.model.AppIcon
import io.github.areswjd.piggybank.model.TransactionType
import io.github.areswjd.piggybank.ui.AppViewModelProvider
import io.github.areswjd.piggybank.ui.common.DayCard
import io.github.areswjd.piggybank.ui.common.groupByDay
import io.github.areswjd.piggybank.ui.common.ledgerDelta
import io.github.areswjd.piggybank.ui.ledger.AddTransactionButton
import io.github.areswjd.piggybank.ui.ledger.SummaryBar
import io.github.areswjd.piggybank.ui.theme.PiggyBankTheme
import java.time.LocalDate

private enum class DesignTab { THEME, ICON }

/**
 * 설정 → 디자인. [테마 | 앱 아이콘] 탭에서 하나를 고르고 아래 버튼을 눌러야 적용된다.
 * 테마는 위 미리보기에서 먼저 볼 수 있다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignScreen(
    onBack: () -> Unit,
    viewModel: DesignViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val currentDesign by viewModel.design.collectAsStateWithLifecycle()
    val currentIcon by viewModel.icon.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(DesignTab.THEME) }
    // 고르기만 하고 아직 적용하지 않은 것. null이면 지금 쓰는 것.
    var pendingDesignKey by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingIconName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedDesign = pendingDesignKey?.let(AppDesign::fromKey) ?: currentDesign
    val selectedIcon = pendingIconName?.let(AppIcon::valueOf) ?: currentIcon

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(context.getString(it)) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.design_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val isCurrent = when (tab) {
                DesignTab.THEME -> selectedDesign == currentDesign
                DesignTab.ICON -> selectedIcon == currentIcon
            }
            val label = when {
                tab == DesignTab.THEME && isCurrent -> R.string.design_current_theme
                tab == DesignTab.THEME -> R.string.design_apply_theme
                isCurrent -> R.string.design_current_icon
                else -> R.string.design_apply_icon
            }
            Button(
                onClick = {
                    when (tab) {
                        DesignTab.THEME -> viewModel.applyDesign(selectedDesign)
                        DesignTab.ICON -> viewModel.applyIcon(selectedIcon)
                    }
                },
                enabled = !isCurrent,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(52.dp),
            ) {
                Text(stringResource(label), style = MaterialTheme.typography.titleMedium)
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            DesignTabs(tab, onSelect = { tab = it }, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            Spacer(Modifier.height(16.dp))
            when (tab) {
                DesignTab.THEME -> ThemeTab(
                    selected = selectedDesign,
                    icon = currentIcon,
                    onSelect = { pendingDesignKey = it.key },
                )
                DesignTab.ICON -> IconTab(
                    selected = selectedIcon,
                    current = currentIcon,
                    onSelect = { pendingIconName = it.name },
                )
            }
        }
    }
}

/** 알약 모양 두 칸 탭. */
@Composable
private fun DesignTabs(selected: DesignTab, onSelect: (DesignTab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
    ) {
        DesignTab.entries.forEach { tab ->
            val isSelected = tab == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(if (tab == DesignTab.THEME) R.string.design_tab_theme else R.string.design_tab_icon),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── 테마 탭 ─────────────────────────────────────────────────────────────────

@Composable
private fun ThemeTab(selected: AppDesign, icon: AppIcon, onSelect: (AppDesign) -> Unit) {
    ThemePreview(selected, icon, modifier = Modifier.padding(horizontal = 16.dp))
    Spacer(Modifier.height(16.dp))
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(AppDesign.entries) { design ->
            ThemeChip(design, selected = design == selected, onClick = { onSelect(design) })
        }
    }
    Text(
        stringResource(R.string.design_theme_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

/** 고른 테마의 색·글꼴로 가계부 일부를 그린다. 가계부 화면과 같은 부품을 쓴다. */
@Composable
private fun ThemePreview(design: AppDesign, icon: AppIcon, modifier: Modifier = Modifier) {
    val sample = previewSection()
    PiggyBankTheme(design = design) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(modifier = Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIconImage(icon, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(stringResource(design.labelRes()), style = MaterialTheme.typography.titleLarge)
                        Text(
                            stringResource(R.string.design_preview),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                SummaryBar(
                    summary = MonthlySummary(income = 3_000_000, expense = 452_000),
                    hidden = false,
                    onToggleHidden = {},
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                DayCard(sample, onTransactionClick = {}, modifier = Modifier.padding(horizontal = 16.dp))
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.CenterEnd) {
                    AddTransactionButton(onClick = {})
                }
            }
        }
    }
}

/** 미리보기용 거래 두 개(지출 점심, 수입 월급). 저장하지 않는 보기용 데이터다. */
@Composable
private fun previewSection() = groupByDay(
    listOf(
        previewDetail(
            id = 1, type = TransactionType.EXPENSE, amount = 12_000, memo = stringResource(R.string.design_preview_memo_lunch),
            group = stringResource(R.string.design_preview_group_cash), asset = stringResource(R.string.design_preview_asset_wallet),
        ),
        previewDetail(
            id = 2, type = TransactionType.INCOME, amount = 3_000_000, memo = stringResource(R.string.design_preview_memo_salary),
            group = stringResource(R.string.design_preview_group_bank), asset = stringResource(R.string.design_preview_asset_bank),
        ),
    ),
    ::ledgerDelta,
).first()

private fun previewDetail(id: Long, type: TransactionType, amount: Long, memo: String, group: String, asset: String) =
    TransactionDetail(
        transaction = TransactionEntity(
            id = id,
            type = type,
            date = LocalDate.now(),
            amount = amount,
            assetId = id,
            toAssetId = null,
            memo = memo,
            createdAt = 0,
            updatedAt = 0,
        ),
        assetName = asset,
        assetGroupName = group,
        assetDeleted = false,
        toAssetName = null,
        toAssetGroupName = null,
        toAssetDeleted = false,
    )

/** 테마 동그라미 + 이름. A에는 "기본"을 붙인다. */
@Composable
private fun ThemeChip(design: AppDesign, selected: Boolean, onClick: () -> Unit) {
    val name = stringResource(design.labelRes())
    Column(
        modifier = Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SelectionRing(selected, shape = CircleShape) {
            ThemeSwatch(design, 56.dp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            name,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        if (design == AppDesign.DEFAULT) {
            Text(
                stringResource(R.string.design_default),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 고른 것에 바깥 고리와 체크 표시를 그린다. */
@Composable
private fun SelectionRing(selected: Boolean, shape: Shape, content: @Composable () -> Unit) {
    Box {
        Box(
            modifier = Modifier
                .border(
                    width = 2.5.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = shape,
                )
                .padding(4.dp),
        ) {
            content()
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .padding(2.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

// ── 앱 아이콘 탭 ─────────────────────────────────────────────────────────────

@Composable
private fun IconTab(selected: AppIcon, current: AppIcon, onSelect: (AppIcon) -> Unit) {
    HomeScreenPreview(selected, modifier = Modifier.padding(horizontal = 16.dp))
    Text(
        stringResource(R.string.design_icon_home_preview),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
    Spacer(Modifier.height(12.dp))
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            AppIcon.entries.chunked(3).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEach { icon ->
                        IconCell(
                            icon = icon,
                            selected = icon == selected,
                            isCurrent = icon == current,
                            onClick = { onSelect(icon) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
    Text(
        stringResource(R.string.design_icon_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
    )
}

/** 배경화면 위에 다른 앱 자리 셋과 고른 아이콘을 나란히 보여준다. */
@Composable
private fun HomeScreenPreview(icon: AppIcon, modifier: Modifier = Modifier) {
    val wallpaper = Brush.linearGradient(listOf(Color(0xFF8EC5FC), Color(0xFFE0C3FC)))
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(wallpaper)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(4) { index ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
                if (index == 1) {
                    AppIconImage(icon, 56.dp)
                } else {
                    Box(Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.35f)))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (index == 1) stringResource(R.string.app_name) else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun IconCell(icon: AppIcon, selected: Boolean, isCurrent: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SelectionRing(selected, shape = CircleShape) {
            AppIconImage(icon, 60.dp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(icon.labelRes()),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (isCurrent) {
            Text(
                stringResource(R.string.design_in_use),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
