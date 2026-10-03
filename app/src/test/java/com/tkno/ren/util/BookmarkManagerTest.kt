package com.tkno.ren.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class BookmarkManagerTest {

    @Test
    fun testBookmarkFolderCreation() {
        val folder = BookmarkFolder(
            id = "f-1",
            title = "Development",
            parentId = null,
            timestamp = 1700000000000L
        )

        assertEquals("f-1", folder.id)
        assertEquals("Development", folder.title)
        assertNull(folder.parentId)
        assertEquals(1700000000000L, folder.timestamp)

        val subFolder = BookmarkFolder(
            id = "f-2",
            title = "Kotlin",
            parentId = "f-1",
            timestamp = 1700000001000L
        )
        assertEquals("f-1", subFolder.parentId)
    }

    @Test
    fun testBookmarkItemCreation() {
        val item = BookmarkItem(
            id = "b-1",
            title = "Google Search",
            url = "https://www.google.com",
            favicon = "data:image/png;base64,mock",
            folderId = "f-1",
            timestamp = 1700000000000L
        )

        assertEquals("b-1", item.id)
        assertEquals("Google Search", item.title)
        assertEquals("https://www.google.com", item.url)
        assertEquals("data:image/png;base64,mock", item.favicon)
        assertEquals("f-1", item.folderId)
        assertEquals(1700000000000L, item.timestamp)
    }

    @Test
    fun testNetscapeHtmlParsingStructure() {
        val sampleNetscapeHtml = """
            <!DOCTYPE NETSCAPE-Bookmark-file-1>
            <!-- This is an automatically generated file. -->
            <META HTTP-EQUIV="Content-Type" CONTENT="text/html; charset=UTF-8">
            <TITLE>Bookmarks</TITLE>
            <H1>Bookmarks</H1>
            <DL><p>
                <DT><A HREF="https://duckduckgo.com" ADD_DATE="1600000000">DuckDuckGo</A>
                <DT><H3 ADD_DATE="1600000010">Search Engines</H3>
                <DL><p>
                    <DT><A HREF="https://google.com" ADD_DATE="1600000020">Google &amp; Search</A>
                    <DT><A HREF="https://bing.com" ADD_DATE="1600000030" ICON="data:image/png;base64,xyz">Bing &quot;Engine&quot;</A>
                </DL><p>
                <DT><A HREF="https://github.com" ADD_DATE="1600000040">GitHub</A>
            </DL><p>
        """.trimIndent()

        // Test regex pattern extraction on this HTML
        val tagPattern = java.util.regex.Pattern.compile(
            "(?i)(<h3\\b([^>]*)>(.*?)(?:</h3>|$))|(<a\\b([^>]*?)>(.*?)(?:</a>|$))|(</dl>)",
            java.util.regex.Pattern.DOTALL
        )

        val matcher = tagPattern.matcher(sampleNetscapeHtml)
        var h3Count = 0
        var aCount = 0
        var dlCloseCount = 0

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                h3Count++
                val folderTitle = matcher.group(3)
                assertEquals("Search Engines", folderTitle?.trim())
            } else if (matcher.group(4) != null) {
                aCount++
            } else if (matcher.group(7) != null) {
                dlCloseCount++
            }
        }

        assertEquals(1, h3Count)
        assertEquals(4, aCount)
        assertEquals(2, dlCloseCount)
    }

    @Test
    fun testAttributeExtraction() {
        val attrString = "HREF=\"https://github.com/hamzabellouch/ren-browser\" ADD_DATE=\"1690000000\" ICON=\"data:image/png;base64,abc\""
        
        fun extract(attr: String): String? {
            val pattern = java.util.regex.Pattern.compile(
                "(?i)\\b$attr\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))"
            )
            val m = pattern.matcher(attrString)
            if (m.find()) {
                return m.group(1) ?: m.group(2) ?: m.group(3)
            }
            return null
        }

        assertEquals("https://github.com/hamzabellouch/ren-browser", extract("HREF"))
        assertEquals("1690000000", extract("ADD_DATE"))
        assertEquals("data:image/png;base64,abc", extract("ICON"))
        assertNull(extract("NON_EXISTENT"))
    }

    @Test
    fun testHtmlEntityEscaping() {
        fun escapeHtml(text: String): String {
            return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
        }

        fun unescapeHtml(text: String): String {
            return text
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&#x2F;", "/")
        }

        val original = "Ren Browser <Fast & \"Secure\"> 'Beta'"
        val escaped = escapeHtml(original)
        assertEquals("Ren Browser &lt;Fast &amp; &quot;Secure&quot;&gt; &#39;Beta&#39;", escaped)

        val unescaped = unescapeHtml(escaped)
        assertEquals(original, unescaped)
    }
}
