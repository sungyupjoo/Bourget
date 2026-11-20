package com.example.bourget.detection

/**
 * Pure helpers for website rules (no Android types, unit tested).
 *
 * Only the host is ever used; paths and queries are discarded immediately.
 */

private val HOST_PATTERN = Regex("^[a-z0-9-]+(\\.[a-z0-9-]+)+$")

/**
 * Host shown in a browser address bar, e.g. "namu.wiki/w/..." or "https://m.namu.wiki/x".
 * Returns null whenever the text is not clearly a single URL (empty, contains spaces,
 * non-ASCII host, no dot, ...), so callers treat it as "unknown" rather than guessing.
 */
fun hostFromAddressBar(text: CharSequence?): String? {
    // Browsers may prepend invisible format characters (Samsung Internet adds U+200E).
    val cleaned = text?.filterNot { Character.getType(it) == Character.FORMAT.toInt() }
        ?.toString()?.trim()
    if (cleaned.isNullOrEmpty() || cleaned.any { it.isWhitespace() }) return null
    val afterScheme = if ("://" in cleaned) cleaned.substringAfter("://") else cleaned
    val host = afterScheme
        .substringBefore('/').substringBefore('?').substringBefore('#')
        .substringAfterLast('@')
        .substringBefore(':')
        .lowercase()
    return host.takeIf { HOST_PATTERN.matches(it) }
}

/** Domain typed by the user, normalized to a bare host ("https://www.Namu.wiki/x" -> "namu.wiki"). */
fun normalizeDomain(input: String): String? {
    val host = hostFromAddressBar(input) ?: return null
    val withoutWww = host.removePrefix("www.")
    return if ('.' in withoutWww) withoutWww else host
}

/** The rule domain that [host] belongs to (itself or any subdomain), most specific first. */
fun matchingRule(host: String, domains: Set<String>): String? =
    domains.filter { host == it || host.endsWith(".$it") }.maxByOrNull { it.length }
