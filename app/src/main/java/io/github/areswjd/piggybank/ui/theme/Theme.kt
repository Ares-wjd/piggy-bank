package io.github.areswjd.piggybank.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import io.github.areswjd.piggybank.model.AppDesign

/** Material 색 체계에 없는 앱 전용 색. */
@Immutable
data class PiggyColors(
    val income: Color,
    val expense: Color,
    val transfer: Color,
)

/** 카드 테두리. [dashed]이면 점선. */
@Immutable
data class CardBorder(val width: Dp, val light: Color, val dark: Color, val dashed: Boolean)

/** 거래 유형 표시: 동그라미 안 화살표 / 둥근 네모 안 화살표 / 도장 글자(수·지·이). */
enum class BadgeStyle { CIRCLE_ICON, ROUNDED_ICON, STAMP }

/** 기록(+) 버튼 모양. */
enum class FabStyle { SQUARE, EXTENDED, CIRCLE_PENCIL }

/** 총자산·현재 잔액처럼 강조하는 카드: 연한 색 / 진한 주 색 / 테두리. */
enum class HighlightStyle { CONTAINER, FILLED, OUTLINED }

/** 디자인마다 다른 화면 장식. */
@Immutable
data class PiggyStyle(
    val cardBorder: CardBorder?,
    val cardElevation: Dp,
    /** 월 요약 줄을 글자 없이 색 점과 숫자만으로 짧게 보여줄지. */
    val summaryCompact: Boolean,
    val badge: BadgeStyle,
    /** 날짜 머리글을 견출지(색 배경)처럼 보여줄지. */
    val dayHeaderSticker: Boolean,
    val fab: FabStyle,
    /** 기록하기 화면에서 금액을 화면 가운데에 아주 크게 보여줄지. */
    val amountHero: Boolean,
    val highlightCard: HighlightStyle,
)

private val LocalPiggyColors = staticCompositionLocalOf { AppDesign.DEFAULT.spec().lightExtras }
private val LocalPiggyStyle = staticCompositionLocalOf { AppDesign.DEFAULT.spec().style }
private val LocalDarkTheme = staticCompositionLocalOf { false }

object PiggyTheme {
    val colors: PiggyColors
        @Composable
        @ReadOnlyComposable
        get() = LocalPiggyColors.current

    val style: PiggyStyle
        @Composable
        @ReadOnlyComposable
        get() = LocalPiggyStyle.current

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalDarkTheme.current
}

@Composable
fun PiggyBankTheme(
    design: AppDesign = AppDesign.DEFAULT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val spec = design.spec()
    CompositionLocalProvider(
        LocalPiggyColors provides if (darkTheme) spec.darkExtras else spec.lightExtras,
        LocalPiggyStyle provides spec.style,
        LocalDarkTheme provides darkTheme,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) spec.dark else spec.light,
            typography = spec.typography,
            shapes = spec.shapes,
            content = content,
        )
    }
}
