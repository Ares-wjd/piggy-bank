package io.github.areswjd.piggybank.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.github.areswjd.piggybank.R

/** 동글동글한 한글 폰트 Jua (SIL OFL 1.1, docs/licenses/Jua-OFL.txt). 굵기는 Regular 하나뿐이다. */
val Jua = FontFamily(Font(R.font.jua_regular))

private val Default = Typography()

// 제목·라벨·버튼은 Jua로 아기자기하게, 긴 본문은 읽기 쉬운 기본 폰트로 둔다.
private fun TextStyle.jua() = copy(fontFamily = Jua, fontWeight = FontWeight.Normal)

val PiggyTypography = Typography(
    displayLarge = Default.displayLarge.jua(),
    displayMedium = Default.displayMedium.jua(),
    displaySmall = Default.displaySmall.jua(),
    headlineLarge = Default.headlineLarge.jua(),
    headlineMedium = Default.headlineMedium.jua(),
    headlineSmall = Default.headlineSmall.jua(),
    titleLarge = Default.titleLarge.jua(),
    titleMedium = Default.titleMedium.jua(),
    titleSmall = Default.titleSmall.jua(),
    labelLarge = Default.labelLarge.jua(),
    labelMedium = Default.labelMedium.jua(),
    labelSmall = Default.labelSmall.jua(),
)
