package io.github.areswjd.piggybank.model

/** 거래 유형. DB에는 이름(INCOME 등) 그대로 저장한다 — 이름을 바꾸면 마이그레이션이 필요하다. */
enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER,
}
