package com.example.bourget.detection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WebsiteRulesTest {

    @Test
    fun chromeAddressBarWithoutScheme() {
        // As shown by Chrome 158 on a Galaxy S22 Ultra.
        assertEquals("namu.wiki", hostFromAddressBar("namu.wiki/w/나무위키:대문"))
    }

    @Test
    fun samsungInternetLeadingFormatCharacter() {
        // Samsung Internet 31 prefixes the host with U+200E (left-to-right mark).
        assertEquals("namu.wiki", hostFromAddressBar("‎namu.wiki"))
    }

    @Test
    fun searchResultsUseSearchEngineHost() {
        assertEquals("google.com", hostFromAddressBar("google.com/search?q=namu.wiki"))
    }

    @Test
    fun schemePortAndUserInfoAreIgnored() {
        assertEquals("m.namu.wiki", hostFromAddressBar("https://M.Namu.Wiki/w/a"))
        assertEquals("example.com", hostFromAddressBar("http://user@example.com:8080/x"))
    }

    @Test
    fun anythingNotClearlyAUrlIsUnknown() {
        assertNull(hostFromAddressBar(null))
        assertNull(hostFromAddressBar(""))
        assertNull(hostFromAddressBar("namu wiki"))
        assertNull(hostFromAddressBar("chrome://newtab"))
        assertNull(hostFromAddressBar("나무위키.한국"))
    }

    @Test
    fun normalizeUserInput() {
        assertEquals("namu.wiki", normalizeDomain("https://www.Namu.Wiki/w/a"))
        assertEquals("namu.wiki", normalizeDomain(" namu.wiki "))
        assertEquals("www.com", normalizeDomain("www.com"))
        assertNull(normalizeDomain("namu"))
        assertNull(normalizeDomain("namu wiki"))
    }

    @Test
    fun ruleMatchesDomainAndSubdomains() {
        val rules = setOf("namu.wiki")
        assertEquals("namu.wiki", matchingRule("namu.wiki", rules))
        assertEquals("namu.wiki", matchingRule("m.namu.wiki", rules))
        assertNull(matchingRule("notnamu.wiki", rules))
        assertNull(matchingRule("namu.wiki.example.com", rules))
    }

    @Test
    fun mostSpecificRuleWins() {
        assertEquals("m.namu.wiki", matchingRule("a.m.namu.wiki", setOf("namu.wiki", "m.namu.wiki")))
    }
}
