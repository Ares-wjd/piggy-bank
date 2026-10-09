package io.github.areswjd.piggybank.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val moneyFormat = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US))
private val fullDateFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd (E)", Locale.KOREAN)
private val shortDateFormat = DateTimeFormatter.ofPattern("MM.dd (E)", Locale.KOREAN)
private val monthFormat = DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN)
private val dateTimeFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREAN)

/** 12000 → "12,000", -500 → "-500" */
fun formatMoney(amount: Long): String = moneyFormat.format(amount)

/** 부호를 붙인다. 12000 → "+12,000", -500 → "-500", 0 → "0" */
fun formatSignedMoney(amount: Long): String = if (amount > 0) "+${formatMoney(amount)}" else formatMoney(amount)

/** "2026.10.09 (금)" */
fun formatFullDate(date: LocalDate): String = fullDateFormat.format(date)

/** "10.09 (금)" */
fun formatShortDate(date: LocalDate): String = shortDateFormat.format(date)

/** "2026년 10월" */
fun formatMonth(month: YearMonth): String = monthFormat.format(month)

/** epoch 밀리초 → "2026.10.09 14:30" (기기 시간대) */
fun formatDateTime(epochMillis: Long): String =
    dateTimeFormat.format(java.time.Instant.ofEpochMilli(epochMillis).atZone(java.time.ZoneId.systemDefault()))

/**
 * 금액 입력칸의 글자를 정리한다. 숫자만 남기고(앞의 0 제거), [allowNegative]이면 맨 앞의 '-'를 허용한다.
 * 12자리(999,999,999,999)를 넘으면 null을 돌려 입력을 무시하게 한다.
 */
fun sanitizeAmountInput(input: String, allowNegative: Boolean = false): String? {
    val negative = allowNegative && input.startsWith("-")
    val digits = input.filter { it.isDigit() }.trimStart('0')
    if (digits.length > 12) return null
    return if (negative) "-$digits" else digits
}

/** 정리된 금액 문자열 → 숫자. 비었거나 '-'뿐이면 null. */
fun parseAmountInput(text: String): Long? = text.takeIf { it.isNotEmpty() && it != "-" }?.toLongOrNull()
