package io.github.areswjd.piggybank.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AppDesignTest {
    @Test
    fun `저장된 값이 없거나 모르는 값이면 기본 테마(딸기우유)`() {
        assertEquals(AppDesign.STRAWBERRY, AppDesign.fromKey(null))
        assertEquals(AppDesign.STRAWBERRY, AppDesign.fromKey("unknown"))
    }

    @Test
    fun `모든 테마는 자기 key로 되찾을 수 있고 key는 서로 다르다`() {
        AppDesign.entries.forEach { assertEquals(it, AppDesign.fromKey(it.key)) }
        assertEquals(AppDesign.entries.size, AppDesign.entries.map { it.key }.toSet().size)
        assertEquals(8, AppDesign.entries.size)
    }
}
