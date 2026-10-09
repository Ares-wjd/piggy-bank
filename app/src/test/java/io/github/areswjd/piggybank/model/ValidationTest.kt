package io.github.areswjd.piggybank.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ValidationTest {

    private fun draft(
        type: TransactionType = TransactionType.EXPENSE,
        amount: Long = 1_000,
        assetId: Long = 1,
        toAssetId: Long? = null,
        memo: String = "",
    ) = TransactionDraft(type, LocalDate.of(2026, 10, 9), amount, assetId, toAssetId, memo)

    @Test
    fun name_mustBeOneToTwentyChars() {
        assertEquals(ValidationError.NAME_EMPTY, validateName(""))
        assertNull(validateName("국"))
        assertNull(validateName("가".repeat(20)))
        assertEquals(ValidationError.NAME_TOO_LONG, validateName("가".repeat(21)))
    }

    @Test
    fun initialBalance_allowsNegativeWithinLimit() {
        assertNull(validateInitialBalance(0))
        assertNull(validateInitialBalance(-Limits.AMOUNT_MAX))
        assertNull(validateInitialBalance(Limits.AMOUNT_MAX))
        assertEquals(ValidationError.INITIAL_BALANCE_OUT_OF_RANGE, validateInitialBalance(Limits.AMOUNT_MAX + 1))
    }

    @Test
    fun amount_mustBeBetweenOneAndMax() {
        assertEquals(ValidationError.AMOUNT_OUT_OF_RANGE, validateTransaction(draft(amount = 0)))
        assertEquals(ValidationError.AMOUNT_OUT_OF_RANGE, validateTransaction(draft(amount = -5)))
        assertNull(validateTransaction(draft(amount = 1)))
        assertNull(validateTransaction(draft(amount = Limits.AMOUNT_MAX)))
        assertEquals(ValidationError.AMOUNT_OUT_OF_RANGE, validateTransaction(draft(amount = Limits.AMOUNT_MAX + 1)))
    }

    @Test
    fun transfer_needsDifferentTargetAsset() {
        val transfer = TransactionType.TRANSFER
        assertEquals(ValidationError.TRANSFER_TARGET_REQUIRED, validateTransaction(draft(type = transfer)))
        assertEquals(
            ValidationError.TRANSFER_SAME_ASSET,
            validateTransaction(draft(type = transfer, assetId = 1, toAssetId = 1)),
        )
        assertNull(validateTransaction(draft(type = transfer, assetId = 1, toAssetId = 2)))
    }

    @Test
    fun memo_isOptionalUpToFiftyChars() {
        assertNull(validateTransaction(draft(memo = "")))
        assertNull(validateTransaction(draft(memo = "a".repeat(50))))
        assertEquals(ValidationError.MEMO_TOO_LONG, validateTransaction(draft(memo = "a".repeat(51))))
    }
}
