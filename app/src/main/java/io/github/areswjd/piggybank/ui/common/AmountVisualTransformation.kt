package io.github.areswjd.piggybank.ui.common

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/** 입력 중인 숫자에 천 단위 쉼표를 보여준다("-1234567" → "-1,234,567"). 실제 값은 숫자만 남는다. */
object AmountVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val digitsStart = if (raw.startsWith("-")) 1 else 0
        val digitCount = raw.length - digitsStart

        val out = StringBuilder()
        val originalToTransformed = IntArray(raw.length + 1)
        raw.forEachIndexed { i, c ->
            val pos = i - digitsStart
            if (pos > 0 && (digitCount - pos) % 3 == 0) out.append(',')
            originalToTransformed[i] = out.length
            out.append(c)
        }
        originalToTransformed[raw.length] = out.length

        val transformedToOriginal = IntArray(out.length + 1)
        var original = 0
        for (t in 0..out.length) {
            transformedToOriginal[t] = original
            if (t < out.length && out[t] != ',') original++
        }

        return TransformedText(
            AnnotatedString(out.toString()),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = originalToTransformed[offset.coerceIn(0, raw.length)]
                override fun transformedToOriginal(offset: Int) = transformedToOriginal[offset.coerceIn(0, out.length)]
            },
        )
    }
}

/** [base]로 바꾼 글자 뒤에 [suffix](예: " 원")를 붙인다. 커서는 숫자 부분 안에서만 움직인다. */
class SuffixTransformation(private val base: VisualTransformation, private val suffix: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (text.isEmpty()) return base.filter(text)
        val inner = base.filter(text)
        val innerLength = inner.text.length
        return TransformedText(
            AnnotatedString(inner.text.text + suffix),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = inner.offsetMapping.originalToTransformed(offset)
                override fun transformedToOriginal(offset: Int) =
                    inner.offsetMapping.transformedToOriginal(offset.coerceAtMost(innerLength))
            },
        )
    }
}
