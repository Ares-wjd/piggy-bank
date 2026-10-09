package io.github.areswjd.piggybank.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class FormattersTest {

    @Test
    fun money() {
        assertEquals("0", formatMoney(0))
        assertEquals("12,000", formatMoney(12_000))
        assertEquals("-1,234,567", formatMoney(-1_234_567))
        assertEquals("999,999,999,999", formatMoney(999_999_999_999))
        assertEquals("+12,000", formatSignedMoney(12_000))
        assertEquals("-500", formatSignedMoney(-500))
        assertEquals("0", formatSignedMoney(0))
    }

    @Test
    fun dates() {
        assertEquals("2026.10.09 (목)", formatFullDate(LocalDate.of(2026, 10, 9)))
        assertEquals("10.09 (목)", formatShortDate(LocalDate.of(2026, 10, 9)))
        assertEquals("2026년 10월", formatMonth(YearMonth.of(2026, 10)))
    }

    @Test
    fun amountInput_keepsDigitsOnly() {
        assertEquals("12000", sanitizeAmountInput("12,000"))
        assertEquals("5", sanitizeAmountInput("005"))
        assertEquals("", sanitizeAmountInput("0"))
        assertEquals("123", sanitizeAmountInput("-123"))
        assertEquals("999999999999", sanitizeAmountInput("999999999999"))
        assertNull(sanitizeAmountInput("1000000000000"))
    }

    @Test
    fun amountInput_negativeWhenAllowed() {
        assertEquals("-123", sanitizeAmountInput("-123", allowNegative = true))
        assertEquals("-", sanitizeAmountInput("-", allowNegative = true))
        assertEquals("123", sanitizeAmountInput("1-23", allowNegative = true))
    }

    @Test
    fun parseAmount() {
        assertEquals(12_000L, parseAmountInput("12000"))
        assertEquals(-5L, parseAmountInput("-5"))
        assertNull(parseAmountInput(""))
        assertNull(parseAmountInput("-"))
    }
}
