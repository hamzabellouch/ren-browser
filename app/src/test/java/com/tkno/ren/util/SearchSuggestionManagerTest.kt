package com.tkno.ren.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSuggestionManagerTest {

    @Test
    fun testBuildSuggestionUrl() {
        val googleUrl = SearchSuggestionManager.buildSuggestionUrl("google", "kotlin coroutines")
        assertNotNull(googleUrl)
        assertTrue(googleUrl!!.contains("suggestqueries.google.com"))
        assertTrue(googleUrl.contains("kotlin+coroutines") || googleUrl.contains("kotlin%20coroutines"))

        val duckUrl = SearchSuggestionManager.buildSuggestionUrl("duckduckgo", "android compose")
        assertNotNull(duckUrl)
        assertTrue(duckUrl!!.contains("duckduckgo.com/ac/"))

        val braveUrl = SearchSuggestionManager.buildSuggestionUrl("brave", "tor browser")
        assertNotNull(braveUrl)
        assertTrue(braveUrl!!.contains("search.brave.com/api/suggest"))

        val qwantUrl = SearchSuggestionManager.buildSuggestionUrl("qwant", "privacy")
        assertNotNull(qwantUrl)
        assertTrue(qwantUrl!!.contains("api.qwant.com/v3/suggest"))
    }

    @Test
    fun testParseGoogleSuggestions() {
        val googleJson = """["ren browser", ["ren browser android", "ren browser github", "ren browser apk"]]"""
        val results = SearchSuggestionManager.parseSuggestions("google", googleJson)

        assertEquals(3, results.size)
        assertEquals("ren browser android", results[0])
        assertEquals("ren browser github", results[1])
        assertEquals("ren browser apk", results[2])
    }

    @Test
    fun testParseDuckDuckGoSuggestions() {
        val ddgJson = """[{"phrase": "duckduckgo search"}, {"phrase": "duckduckgo privacy"}, {"phrase": "duckduckgo bang"}]"""
        val results = SearchSuggestionManager.parseSuggestions("duckduckgo", ddgJson)

        assertEquals(3, results.size)
        assertEquals("duckduckgo search", results[0])
        assertEquals("duckduckgo privacy", results[1])
        assertEquals("duckduckgo bang", results[2])
    }

    @Test
    fun testParseBraveSuggestions() {
        val braveJsonArray = """["brave", ["brave browser", "brave search"]]"""
        val resultsArray = SearchSuggestionManager.parseSuggestions("brave", braveJsonArray)
        assertEquals(2, resultsArray.size)
        assertEquals("brave browser", resultsArray[0])
        assertEquals("brave search", resultsArray[1])

        val braveJsonObject = """{"query": "brave", "results": [{"title": "brave shield"}, {"title": "brave wallet"}]}"""
        val resultsObj = SearchSuggestionManager.parseSuggestions("brave", braveJsonObject)
        assertEquals(2, resultsObj.size)
        assertEquals("brave shield", resultsObj[0])
        assertEquals("brave wallet", resultsObj[1])
    }

    @Test
    fun testParseQwantSuggestions() {
        val qwantJson = """{"status": "success", "data": {"items": [{"value": "qwant maps"}, {"value": "qwant junior"}]}}"""
        val results = SearchSuggestionManager.parseSuggestions("qwant", qwantJson)

        assertEquals(2, results.size)
        assertEquals("qwant maps", results[0])
        assertEquals("qwant junior", results[1])
    }

    @Test
    fun testMalformedJsonDoesNotCrash() {
        val results = SearchSuggestionManager.parseSuggestions("google", "invalid json string")
        assertTrue(results.isEmpty())
    }
}
