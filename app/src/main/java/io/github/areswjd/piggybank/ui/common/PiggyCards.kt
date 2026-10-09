package io.github.areswjd.piggybank.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.areswjd.piggybank.ui.theme.HighlightStyle
import io.github.areswjd.piggybank.ui.theme.PiggyTheme

/** 테두리. [dashed]이면 점선으로 그린다. */
fun Modifier.piggyBorder(width: Dp, color: Color, shape: Shape, dashed: Boolean): Modifier =
    if (!dashed) {
        border(width, color, shape)
    } else {
        drawWithContent {
            drawContent()
            val outline = shape.createOutline(size, layoutDirection, this)
            val dash = 6.dp.toPx()
            drawOutline(
                outline,
                color,
                style = Stroke(width = width.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash * 0.7f))),
            )
        }
    }

/** 화면 디자인에 맞춘 기본 카드(그림자 / 테두리 / 점선 테두리). */
@Composable
fun PiggyCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val style = PiggyTheme.style
    val shape = MaterialTheme.shapes.large
    val border = style.cardBorder
    val borderModifier = if (border != null) {
        Modifier.piggyBorder(border.width, if (PiggyTheme.isDark) border.dark else border.light, shape, border.dashed)
    } else {
        Modifier
    }
    Card(
        modifier = modifier.fillMaxWidth().then(borderModifier),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = style.cardElevation),
        content = content,
    )
}

/** 강조 카드(총자산, 현재 잔액)의 배경색과 글자색. */
data class HighlightColors(val container: Color, val content: Color, val subContent: Color)

@Composable
fun highlightColors(): HighlightColors {
    val scheme = MaterialTheme.colorScheme
    return when (PiggyTheme.style.highlightCard) {
        HighlightStyle.CONTAINER -> HighlightColors(scheme.primaryContainer, scheme.onPrimaryContainer, scheme.onPrimaryContainer)
        HighlightStyle.FILLED -> HighlightColors(scheme.primary, scheme.onPrimary, scheme.onPrimary.copy(alpha = 0.85f))
        HighlightStyle.OUTLINED -> HighlightColors(scheme.surfaceContainerLowest, scheme.onSurface, scheme.onSurfaceVariant)
    }
}

/** 총자산·현재 잔액처럼 눈에 띄게 보여주는 카드. */
@Composable
fun HighlightCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = MaterialTheme.shapes.large
    val outlined = PiggyTheme.style.highlightCard == HighlightStyle.OUTLINED
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (outlined) Modifier.border(2.dp, MaterialTheme.colorScheme.outline, shape) else Modifier),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = highlightColors().container),
        content = content,
    )
}
