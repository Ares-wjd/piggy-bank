package io.github.areswjd.piggybank.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.areswjd.piggybank.R
import io.github.areswjd.piggybank.data.local.dao.TransactionDetail
import io.github.areswjd.piggybank.model.TransactionType
import io.github.areswjd.piggybank.ui.theme.BadgeStyle
import io.github.areswjd.piggybank.ui.theme.PiggyTheme
import io.github.areswjd.piggybank.util.formatMoney
import io.github.areswjd.piggybank.util.formatShortDate
import io.github.areswjd.piggybank.util.formatSignedMoney
import java.time.LocalDate

/** 날짜 하나에 묶인 거래들. [net]은 그날의 합계(관점에 따라 계산 방식이 다르다). */
data class DaySection(val date: LocalDate, val net: Long, val items: List<TransactionDetail>)

/** 가계부 기준 하루 합계: 수입 − 지출 (이체 제외). */
fun ledgerDelta(detail: TransactionDetail): Long = with(detail.transaction) {
    when (type) {
        TransactionType.INCOME -> amount
        TransactionType.EXPENSE -> -amount
        TransactionType.TRANSFER -> 0
    }
}

/** 자산 하나의 관점에서 본 잔액 변화. 이체는 들어오면 +, 나가면 −. */
fun assetDelta(detail: TransactionDetail, assetId: Long): Long = with(detail.transaction) {
    when {
        type == TransactionType.INCOME -> amount
        type == TransactionType.EXPENSE -> -amount
        toAssetId == assetId -> amount
        else -> -amount
    }
}

/** 날짜 순서(이미 최신순)를 유지하며 날짜별로 묶는다. */
fun groupByDay(items: List<TransactionDetail>, delta: (TransactionDetail) -> Long): List<DaySection> =
    items.groupBy { it.transaction.date }
        .map { (date, dayItems) -> DaySection(date, dayItems.sumOf(delta), dayItems) }

@Composable
fun TransactionType.label(): String = stringResource(
    when (this) {
        TransactionType.INCOME -> R.string.type_income
        TransactionType.EXPENSE -> R.string.type_expense
        TransactionType.TRANSFER -> R.string.type_transfer
    },
)

@Composable
fun TransactionType.color(): Color = when (this) {
    TransactionType.INCOME -> PiggyTheme.colors.income
    TransactionType.EXPENSE -> PiggyTheme.colors.expense
    TransactionType.TRANSFER -> PiggyTheme.colors.transfer
}

private fun TransactionType.icon(): ImageVector = when (this) {
    TransactionType.INCOME -> Icons.Rounded.ArrowDownward
    TransactionType.EXPENSE -> Icons.Rounded.ArrowUpward
    TransactionType.TRANSFER -> Icons.Rounded.SwapHoriz
}

/** "그룹 > 자산", 삭제된 자산이면 뒤에 "(삭제됨)". */
@Composable
fun assetLabel(groupName: String, assetName: String, deleted: Boolean): String {
    val base = "$groupName > $assetName"
    return if (deleted) "$base ${stringResource(R.string.deleted_suffix)}" else base
}

@Composable
private fun TransactionDetail.assetText(): String {
    val from = assetLabel(assetGroupName, assetName, assetDeleted)
    if (transaction.type != TransactionType.TRANSFER || toAssetName == null) return from
    val deleted = stringResource(R.string.deleted_suffix)
    val fromShort = if (assetDeleted) "$assetName $deleted" else assetName
    val toShort = if (toAssetDeleted) "$toAssetName $deleted" else toAssetName
    return "$fromShort → $toShort"
}

/** 거래 유형 표시. 화면 디자인에 따라 동그라미·둥근 네모 안 화살표, 또는 도장 글자(수/지/이). */
@Composable
fun TypeBadge(type: TransactionType, modifier: Modifier = Modifier) {
    val color = type.color()
    when (PiggyTheme.style.badge) {
        BadgeStyle.STAMP -> Box(
            modifier = modifier
                .size(36.dp)
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(type.stampLetter(), style = MaterialTheme.typography.titleSmall, color = color)
        }
        BadgeStyle.CIRCLE_ICON, BadgeStyle.ROUNDED_ICON -> {
            val shape = if (PiggyTheme.style.badge == BadgeStyle.CIRCLE_ICON) CircleShape else RoundedCornerShape(12.dp)
            Box(
                modifier = modifier
                    .size(38.dp)
                    .clip(shape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(type.icon(), contentDescription = type.label(), tint = color, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** 도장에 쓰는 한 글자: 수입 → 수, 지출 → 지, 이체 → 이. */
@Composable
private fun TransactionType.stampLetter(): String = label().take(1)

/**
 * 거래 한 줄. [perspectiveAssetId]를 주면 그 자산 기준으로 이체 금액에 +/−를 붙인다.
 */
@Composable
fun TransactionRow(
    detail: TransactionDetail,
    onClick: () -> Unit,
    perspectiveAssetId: Long? = null,
) {
    val tx = detail.transaction
    val amountText = when {
        tx.type == TransactionType.INCOME -> "+${formatMoney(tx.amount)}"
        tx.type == TransactionType.EXPENSE -> "-${formatMoney(tx.amount)}"
        perspectiveAssetId != null -> formatSignedMoney(assetDelta(detail, perspectiveAssetId))
        else -> formatMoney(tx.amount)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TypeBadge(tx.type)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = tx.memo.ifEmpty { tx.type.label() },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = detail.assetText(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(amountText, style = MaterialTheme.typography.titleMedium, color = tx.type.color())
    }
}

/** 날짜 머리글 + 그날의 거래들을 담은 카드. */
@Composable
fun DayCard(
    section: DaySection,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    perspectiveAssetId: Long? = null,
) {
    PiggyCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val dateText = formatShortDate(section.date)
            if (PiggyTheme.style.dayHeaderSticker) {
                Text(
                    dateText,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.weight(1f))
            } else {
                Text(
                    dateText,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                formatSignedMoney(section.net),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        section.items.forEachIndexed { index, detail ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 66.dp, end = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            TransactionRow(detail, onClick = { onTransactionClick(detail.transaction.id) }, perspectiveAssetId)
        }
    }
}
