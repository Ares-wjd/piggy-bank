package io.github.areswjd.piggybank.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** AppIcon과 매니페스트의 activity-alias가 서로 맞는지 확인한다(이름이 어긋나면 아이콘을 바꿀 때 앱이 사라질 수 있다). */
class AppIconAliasTest {
    private val android = "http://schemas.android.com/apk/res/android"

    private fun aliases(): Map<String, Boolean> {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val doc = factory.newDocumentBuilder().parse(File("src/main/AndroidManifest.xml"))
        val nodes = doc.getElementsByTagName("activity-alias")
        return (0 until nodes.length).associate { i ->
            val el = nodes.item(i) as org.w3c.dom.Element
            el.getAttributeNS(android, "name") to (el.getAttributeNS(android, "enabled") == "true")
        }
    }

    @Test
    fun `아이콘마다 alias가 하나씩 있다`() {
        assertEquals(AppIcon.entries.map { it.aliasName }.toSet(), aliases().keys)
    }

    @Test
    fun `처음에는 기본 아이콘 alias만 켜져 있다`() {
        assertEquals(setOf(AppIcon.DEFAULT.aliasName), aliases().filterValues { it }.keys)
    }
}
