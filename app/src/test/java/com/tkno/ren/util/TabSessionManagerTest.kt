package com.tkno.ren.util

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class TabSessionManagerTest {

    @Test
    fun testSavedTabSerialization() {
        val tab = SavedTab(
            id = "test-tab-123",
            title = "Example Domain",
            url = "https://example.com",
            isIncognito = false,
            isDesktopSite = true,
            isReaderMode = false,
            originalUrlBeforeReader = null,
            timestamp = 1700000000000L
        )

        val json = tab.toJson()
        assertEquals("test-tab-123", json.getString("id"))
        assertEquals("Example Domain", json.getString("title"))
        assertEquals("https://example.com", json.getString("url"))
        assertFalse(json.getBoolean("isIncognito"))
        assertTrue(json.getBoolean("isDesktopSite"))
        assertFalse(json.getBoolean("isReaderMode"))
        assertEquals(1700000000000L, json.getLong("timestamp"))

        val restored = SavedTab.fromJson(json)
        assertEquals(tab.id, restored.id)
        assertEquals(tab.title, restored.title)
        assertEquals(tab.url, restored.url)
        assertEquals(tab.isIncognito, restored.isIncognito)
        assertEquals(tab.isDesktopSite, restored.isDesktopSite)
        assertEquals(tab.isReaderMode, restored.isReaderMode)
        assertEquals(tab.originalUrlBeforeReader, restored.originalUrlBeforeReader)
        assertEquals(tab.timestamp, restored.timestamp)
    }

    @Test
    fun testSavedTabListSerialization() {
        val tabs = listOf(
            SavedTab(id = "1", title = "Page 1", url = "https://page1.com"),
            SavedTab(id = "2", title = "Page 2", url = "https://page2.com", isDesktopSite = true)
        )

        val jsonStr = SavedTab.listToJson(tabs)
        val restoredList = SavedTab.listFromJson(jsonStr)

        assertEquals(2, restoredList.size)
        assertEquals("1", restoredList[0].id)
        assertEquals("Page 1", restoredList[0].title)
        assertEquals("https://page1.com", restoredList[0].url)
        assertFalse(restoredList[0].isDesktopSite)

        assertEquals("2", restoredList[1].id)
        assertEquals("Page 2", restoredList[1].title)
        assertEquals("https://page2.com", restoredList[1].url)
        assertTrue(restoredList[1].isDesktopSite)
    }

    @Test
    fun testMalformedJsonHandling() {
        val emptyList = SavedTab.listFromJson("malformed { json")
        assertTrue(emptyList.isEmpty())

        val emptyObj = SavedTab.fromJson(JSONObject("{}"))
        assertNotNull(emptyObj.id)
        assertEquals("Blank page", emptyObj.title)
        assertEquals("about:blank", emptyObj.url)
        assertFalse(emptyObj.isIncognito)
    }
}
