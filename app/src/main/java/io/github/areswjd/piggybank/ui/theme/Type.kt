package io.github.areswjd.piggybank.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.github.areswjd.piggybank.R

// 글꼴은 모두 SIL OFL 1.1 (docs/licenses). 고운돋움·개구는 자주 쓰는 한글 2,350자만 담았다. 없는 글자는 폰 기본 글꼴로 보인다.

/** 동글동글한 제목용 글꼴 (A·D·F·H). 굵기는 Regular 하나. */
val Jua = FontFamily(Font(R.font.jua_regular))

/** 부드러운 본문 글꼴 (B·C·D·E·G·H). */
val GowunDodum = FontFamily(Font(R.font.gowun_dodum_regular))

/** 손글씨 제목 글꼴 (C·G). */
val Gaegu = FontFamily(Font(R.font.gaegu_bold, FontWeight.Bold))

private val Default = Typography()

/** 숫자 자릿수를 맞춘다(금액이 세로로 가지런하게). */
private fun TextStyle.tnum() = copy(fontFeatureSettings = "tnum")

/** 제목·라벨 스타일과 본문 스타일을 따로 정해 Typography를 만든다. */
private fun typography(title: (TextStyle) -> TextStyle, body: (TextStyle) -> TextStyle) = Typography(
    displayLarge = title(Default.displayLarge).tnum(),
    displayMedium = title(Default.displayMedium).tnum(),
    displaySmall = title(Default.displaySmall).tnum(),
    headlineLarge = title(Default.headlineLarge).tnum(),
    headlineMedium = title(Default.headlineMedium).tnum(),
    headlineSmall = title(Default.headlineSmall).tnum(),
    titleLarge = title(Default.titleLarge).tnum(),
    titleMedium = title(Default.titleMedium).tnum(),
    titleSmall = title(Default.titleSmall).tnum(),
    labelLarge = title(Default.labelLarge).tnum(),
    labelMedium = title(Default.labelMedium).tnum(),
    labelSmall = title(Default.labelSmall).tnum(),
    bodyLarge = body(Default.bodyLarge).tnum(),
    bodyMedium = body(Default.bodyMedium).tnum(),
    bodySmall = body(Default.bodySmall).tnum(),
)

/** A·F: 제목·숫자·버튼은 Jua, 본문은 폰 기본 글꼴. */
internal val StrawberryTypography = typography(
    title = { it.copy(fontFamily = Jua, fontWeight = FontWeight.Normal) },
    body = { it },
)

/** B·E: 전부 고운돋움. 제목은 굵게. */
internal val MintTypography = typography(
    title = { it.copy(fontFamily = GowunDodum, fontWeight = FontWeight.Bold) },
    body = { it.copy(fontFamily = GowunDodum) },
)

/** C·G: 제목·숫자·버튼은 개구(손글씨, 같은 크기에서 작아 보여 조금 키움), 본문은 고운돋움. */
internal val ButterTypography = typography(
    title = { it.copy(fontFamily = Gaegu, fontWeight = FontWeight.Bold, fontSize = it.fontSize * 1.18f) },
    body = { it.copy(fontFamily = GowunDodum) },
)

/** D·H: 제목·숫자·버튼은 Jua, 본문은 고운돋움. */
internal val JuaGowunTypography = typography(
    title = { it.copy(fontFamily = Jua, fontWeight = FontWeight.Normal) },
    body = { it.copy(fontFamily = GowunDodum) },
)
