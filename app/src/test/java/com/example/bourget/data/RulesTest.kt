package com.example.bourget.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RulesTest {

    private val yt = "com.google.android.youtube"
    private val ig = "com.instagram.android"
    private val anki = "com.ichi2.anki"

    @Test
    fun encodeDecodeRoundTrip() {
        val rules = listOf(
            Rule(yt, enabled = true, action = RuleAction.LAUNCH, launchPackage = anki),
            Rule(ig, enabled = false),
            Rule("namu.wiki"),
        )
        assertEquals(rules, decodeRules(encodeRules(rules)))
    }

    @Test
    fun emptyTextDecodesToNoRules() {
        assertEquals(emptyList<Rule>(), decodeRules(""))
        assertEquals("", encodeRules(emptyList()))
    }

    @Test
    fun malformedLinesAreSkippedAndLaunchWithoutTargetBecomesConfirm() {
        val decoded = decodeRules("\n$yt\t1\tLAUNCH\t\n\t1\tCONFIRM\t\n$ig\t0\tUNKNOWN\t$anki")
        assertEquals(listOf(Rule(yt), Rule(ig, enabled = false)), decoded)
    }

    @Test
    fun newRulesGoFirstAndExistingOnesAreKept() {
        val rules = listOf(Rule(yt, enabled = false))
        assertEquals(listOf(Rule(ig), Rule(yt, enabled = false)), rules.withAdded(Rule(ig)))
        assertEquals(rules, rules.withAdded(Rule(yt)))
    }

    @Test
    fun withAddedRestoresAtIndex() {
        val rules = listOf(Rule(yt), Rule(ig))
        assertEquals(listOf(Rule(yt), Rule(anki), Rule(ig)), rules.withAdded(Rule(anki), index = 1))
    }

    @Test
    fun retainInstalledDropsGoneAppsAndResetsGoneLaunchTargets() {
        val apps = listOf(Rule(yt).withLaunch(anki), Rule(ig))
        assertEquals(listOf(Rule(yt)), apps.retainInstalled(RuleKind.APP, setOf(yt)))

        val sites = listOf(Rule("namu.wiki").withLaunch(anki))
        assertEquals(listOf(Rule("namu.wiki")), sites.retainInstalled(RuleKind.WEBSITE, setOf(yt)))
    }

    @Test
    fun legacyKeysMigrate() {
        assertEquals(listOf(Rule(yt), Rule(ig)).sortedBy { it.target }, legacyAppRules(setOf(yt, ig)))
        assertEquals(emptyList<Rule>(), legacyAppRules(null))
        assertEquals(
            listOf(Rule("mania.kr", enabled = false), Rule("namu.wiki")),
            legacyWebsiteRules(setOf("namu.wiki", "mania.kr"), setOf("mania.kr")),
        )
    }
}
