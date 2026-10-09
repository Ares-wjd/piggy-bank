package io.github.areswjd.piggybank.model

import java.time.LocalDate

object Limits {
    const val NAME_MAX_LENGTH = 20
    const val MEMO_MAX_LENGTH = 50
    const val AMOUNT_MAX = 999_999_999_999L
}

/** 저장·삭제 규칙을 어겼을 때의 이유. 화면에서 안내 문구로 바꿔 보여준다. */
enum class ValidationError {
    NAME_EMPTY,
    NAME_TOO_LONG,
    NAME_DUPLICATE,
    GROUP_NOT_FOUND,
    GROUP_NOT_EMPTY,
    ASSET_NOT_FOUND,
    ASSET_DELETED,
    BALANCE_NOT_ZERO,
    INITIAL_BALANCE_OUT_OF_RANGE,
    AMOUNT_OUT_OF_RANGE,
    TRANSFER_TARGET_REQUIRED,
    TRANSFER_SAME_ASSET,
    MEMO_TOO_LONG,
    TRANSACTION_NOT_FOUND,
}

class ValidationException(val error: ValidationError) : Exception(error.name)

/** 입력 화면에서 넘어오는 거래 내용. [toAssetId]는 이체일 때만 쓴다. */
data class TransactionDraft(
    val type: TransactionType,
    val date: LocalDate,
    val amount: Long,
    val assetId: Long,
    val toAssetId: Long? = null,
    val memo: String = "",
)

/** 자산 그룹·자산 이름 검사. 앞뒤 공백을 뺀 이름을 넘긴다. */
fun validateName(name: String): ValidationError? = when {
    name.isEmpty() -> ValidationError.NAME_EMPTY
    name.length > Limits.NAME_MAX_LENGTH -> ValidationError.NAME_TOO_LONG
    else -> null
}

fun validateInitialBalance(balance: Long): ValidationError? =
    if (balance in -Limits.AMOUNT_MAX..Limits.AMOUNT_MAX) null else ValidationError.INITIAL_BALANCE_OUT_OF_RANGE

/** 거래 내용 자체의 검사. 자산이 존재하는지·삭제됐는지는 저장소에서 따로 확인한다. */
fun validateTransaction(draft: TransactionDraft): ValidationError? = when {
    draft.amount !in 1..Limits.AMOUNT_MAX -> ValidationError.AMOUNT_OUT_OF_RANGE
    draft.type == TransactionType.TRANSFER && draft.toAssetId == null -> ValidationError.TRANSFER_TARGET_REQUIRED
    draft.type == TransactionType.TRANSFER && draft.toAssetId == draft.assetId -> ValidationError.TRANSFER_SAME_ASSET
    draft.memo.length > Limits.MEMO_MAX_LENGTH -> ValidationError.MEMO_TOO_LONG
    else -> null
}
