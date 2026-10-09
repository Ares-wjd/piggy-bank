package io.github.areswjd.piggybank.ui.design

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.areswjd.piggybank.R
import io.github.areswjd.piggybank.model.AppDesign
import io.github.areswjd.piggybank.model.AppIcon
import io.github.areswjd.piggybank.ui.theme.spec

@StringRes
fun AppDesign.labelRes(): Int = when (this) {
    AppDesign.STRAWBERRY -> R.string.design_strawberry
    AppDesign.MINT -> R.string.design_mint
    AppDesign.BUTTER -> R.string.design_butter
    AppDesign.LAVENDER -> R.string.design_lavender
    AppDesign.SKY -> R.string.design_sky
    AppDesign.PEACH -> R.string.design_peach
    AppDesign.MATCHA -> R.string.design_matcha
    AppDesign.NIGHT -> R.string.design_night
}

@StringRes
fun AppIcon.labelRes(): Int = when (this) {
    AppIcon.COIN -> R.string.icon_coin
    AppIcon.PEEK -> R.string.icon_peek
    AppIcon.BIG_SNOUT -> R.string.icon_big_snout
    AppIcon.WINK -> R.string.icon_wink
    AppIcon.SLEEPY -> R.string.icon_sleepy
    AppIcon.BANK -> R.string.icon_bank
    AppIcon.HEART -> R.string.icon_heart
    AppIcon.BALL -> R.string.icon_ball
    AppIcon.GOLD_COIN -> R.string.icon_gold_coin
}

/** 적응형 아이콘의 그림 층(108dp 중 가운데 72dp가 보이는 그림). */
@DrawableRes
private fun AppIcon.foregroundRes(): Int = when (this) {
    AppIcon.COIN -> R.drawable.ic_launcher_fg_coin
    AppIcon.PEEK -> R.drawable.ic_launcher_fg_peek
    AppIcon.BIG_SNOUT -> R.drawable.ic_launcher_fg_bigsnout
    AppIcon.WINK -> R.drawable.ic_launcher_fg_wink
    AppIcon.SLEEPY -> R.drawable.ic_launcher_fg_sleepy
    AppIcon.BANK -> R.drawable.ic_launcher_fg_bank
    AppIcon.HEART -> R.drawable.ic_launcher_fg_heart
    AppIcon.BALL -> R.drawable.ic_launcher_fg_ball
    AppIcon.GOLD_COIN -> R.drawable.ic_launcher_fg_coinface
}

@ColorRes
private fun AppIcon.backgroundRes(): Int = when (this) {
    AppIcon.COIN -> R.color.icon_bg_coin
    AppIcon.PEEK -> R.color.icon_bg_peek
    AppIcon.BIG_SNOUT -> R.color.icon_bg_bigsnout
    AppIcon.WINK -> R.color.icon_bg_wink
    AppIcon.SLEEPY -> R.color.icon_bg_sleepy
    AppIcon.BANK -> R.color.icon_bg_bank
    AppIcon.HEART -> R.color.icon_bg_heart
    AppIcon.BALL -> R.color.icon_bg_ball
    AppIcon.GOLD_COIN -> R.color.icon_bg_coinface
}

/** 홈 화면에서 동그랗게 잘린 아이콘처럼 그린다. */
@Composable
fun AppIconImage(icon: AppIcon, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(colorResource(icon.backgroundRes())),
        contentAlignment = Alignment.Center,
    ) {
        // 그림 층은 108dp 중 가운데 72dp만 보이므로 1.5배로 그려 가운데를 맞춘다.
        Image(
            painter = painterResource(icon.foregroundRes()),
            contentDescription = null,
            modifier = Modifier.requiredSize(size * 1.5f),
        )
    }
}

/** 테마 견본: 동그라미를 대각선으로 나눠 배경색과 주 색을 반씩 칠한다. */
@Composable
fun ThemeSwatch(design: AppDesign, diameter: Dp, modifier: Modifier = Modifier) {
    val scheme = design.spec().light
    Box(
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .drawBehind {
                drawRect(scheme.background)
                val half = Path().apply {
                    moveTo(size.width, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(half, scheme.primary)
                drawCircle(scheme.outlineVariant, style = Stroke(width = 1.dp.toPx()))
            },
    )
}
