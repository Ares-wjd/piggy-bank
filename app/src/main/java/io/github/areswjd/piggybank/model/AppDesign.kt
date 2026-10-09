package io.github.areswjd.piggybank.model

/** 화면 디자인. 설정에서 고르며, 고르기 전에는 [DEFAULT]. [key]는 저장용이라 바꾸지 않는다. */
enum class AppDesign(val key: String) {
    STRAWBERRY("strawberry"),
    MINT("mint"),
    BUTTER("butter"),
    ;

    companion object {
        val DEFAULT = STRAWBERRY

        fun fromKey(key: String?): AppDesign = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}
