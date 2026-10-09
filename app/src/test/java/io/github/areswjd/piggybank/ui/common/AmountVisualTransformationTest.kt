package io.github.areswjd.piggybank.ui.common

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class AmountVisualTransformationTest {

    private fun transform(raw: String) = AmountVisualTransformation.filter(AnnotatedString(raw))

    @Test
    fun insertsThousandsSeparators() {
        assertEquals("", transform("").text.text)
        assertEquals("123", transform("123").text.text)
        assertEquals("1,234", transform("1234").text.text)
        assertEquals("1,234,567", transform("1234567").text.text)
        assertEquals("-1,234", transform("-1234").text.text)
        assertEquals("-", transform("-").text.text)
    }

    @Test
    fun cursorMappingRoundTrips() {
        val raw = "-1234567"
        val mapping = transform(raw).offsetMapping
        assertEquals(0, mapping.originalToTransformed(0))
        assertEquals(raw.length + 2, mapping.originalToTransformed(raw.length)) // 쉼표 2개
        for (offset in 0..raw.length) {
            assertEquals(offset, mapping.transformedToOriginal(mapping.originalToTransformed(offset)))
        }
        // "1,234" 에서 쉼표 바로 뒤(3) → 원래 위치 1
        val simple = transform("1234").offsetMapping
        assertEquals(1, simple.transformedToOriginal(2))
        assertEquals(1, simple.transformedToOriginal(1))
    }
}
