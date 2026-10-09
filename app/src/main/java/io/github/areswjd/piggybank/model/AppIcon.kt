package io.github.areswjd.piggybank.model

/**
 * 홈 화면 앱 아이콘. 아이콘마다 매니페스트에 activity-alias가 하나씩 있다.
 * [aliasName]은 매니페스트의 alias 이름(패키지 뒤 부분)과 같아야 한다.
 */
enum class AppIcon(val aliasName: String) {
    COIN(".IconCoin"),
    PEEK(".IconPeek"),
    BIG_SNOUT(".IconBigSnout"),
    WINK(".IconWink"),
    SLEEPY(".IconSleepy"),
    BANK(".IconBank"),
    HEART(".IconHeart"),
    BALL(".IconBall"),
    GOLD_COIN(".IconGoldCoin"),
    ;

    companion object {
        /** 매니페스트에서 처음부터 켜져 있는 아이콘. */
        val DEFAULT = COIN
    }
}
