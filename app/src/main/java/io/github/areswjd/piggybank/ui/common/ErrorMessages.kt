package io.github.areswjd.piggybank.ui.common

import androidx.annotation.StringRes
import io.github.areswjd.piggybank.R
import io.github.areswjd.piggybank.model.ValidationError
import io.github.areswjd.piggybank.model.ValidationException

@StringRes
fun ValidationError.messageRes(): Int = when (this) {
    ValidationError.NAME_EMPTY -> R.string.error_name_empty
    ValidationError.NAME_TOO_LONG -> R.string.error_name_too_long
    ValidationError.NAME_DUPLICATE -> R.string.error_name_duplicate
    ValidationError.GROUP_NOT_FOUND -> R.string.error_group_not_found
    ValidationError.GROUP_NOT_EMPTY -> R.string.error_group_not_empty
    ValidationError.ASSET_NOT_FOUND -> R.string.error_asset_not_found
    ValidationError.ASSET_DELETED -> R.string.error_asset_deleted
    ValidationError.BALANCE_NOT_ZERO -> R.string.error_balance_not_zero
    ValidationError.INITIAL_BALANCE_OUT_OF_RANGE -> R.string.error_initial_balance
    ValidationError.AMOUNT_OUT_OF_RANGE -> R.string.error_amount
    ValidationError.TRANSFER_TARGET_REQUIRED -> R.string.error_transfer_target
    ValidationError.TRANSFER_SAME_ASSET -> R.string.error_transfer_same
    ValidationError.MEMO_TOO_LONG -> R.string.error_memo_too_long
    ValidationError.TRANSACTION_NOT_FOUND -> R.string.error_transaction_not_found
}

/** 저장소 작업 실패를 안내 문구로 바꾼다. 규칙 위반이 아닌 오류는 일반 문구로. */
@StringRes
fun Throwable.messageRes(): Int = (this as? ValidationException)?.error?.messageRes() ?: R.string.error_unknown
