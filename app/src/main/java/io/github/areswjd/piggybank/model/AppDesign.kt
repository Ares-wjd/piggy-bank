package io.github.areswjd.piggybank.model

/** 화면 테마. [key]는 기기 설정에 저장하는 값이라 바꾸면 안 된다. */
enum class AppDesign(val key: String) {
    STRAWBERRY("strawberry"),
    MINT("mint"),
    BUTTER("butter"),
    LAVENDER("lavender"),
    SKY("sky"),
    PEACH("peach"),
    MATCHA("matcha"),
    NIGHT("night"),
    ;

    companion object {
        val DEFAULT = STRAWBERRY

        fun fromKey(key: String?): AppDesign = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}
