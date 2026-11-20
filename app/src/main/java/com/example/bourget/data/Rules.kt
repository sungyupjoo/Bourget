package com.example.bourget.data

/**
 * Rules as the user made them. Pure Kotlin (no Android types) so storage and migration can be
 * unit tested. Only these rules, the confirm template and consent are ever stored; nothing
 * detected on screen is.
 */

enum class RuleKind { APP, WEBSITE }

enum class RuleAction { CONFIRM, LAUNCH }

/**
 * One rule. [target] is a package name for [RuleKind.APP] and a normalized domain for
 * [RuleKind.WEBSITE]. [launchPackage] is set only when [action] is [RuleAction.LAUNCH].
 */
data class Rule(
    val target: String,
    val enabled: Boolean = true,
    val action: RuleAction = RuleAction.CONFIRM,
    val launchPackage: String? = null,
)

/** New rules go first, matching the order shown on screen. An existing target is left as is. */
fun List<Rule>.withAdded(rule: Rule, index: Int = 0): List<Rule> =
    if (any { it.target == rule.target }) this
    else toMutableList().apply { add(index.coerceIn(0, size), rule) }

fun List<Rule>.without(target: String): List<Rule> = filterNot { it.target == target }

fun List<Rule>.updated(target: String, transform: (Rule) -> Rule): List<Rule> =
    map { if (it.target == target) transform(it) else it }

/** Sets the action to "launch [launchPackage]", or back to "confirm" when it is null. */
fun Rule.withLaunch(launchPackage: String?): Rule =
    if (launchPackage == null) copy(action = RuleAction.CONFIRM, launchPackage = null)
    else copy(action = RuleAction.LAUNCH, launchPackage = launchPackage)

/**
 * Drops rules for apps that are no longer installed ([kind] APP only) and turns "launch" back
 * into "confirm" when the app to launch is gone.
 */
fun List<Rule>.retainInstalled(kind: RuleKind, installed: Set<String>): List<Rule> =
    filter { kind != RuleKind.APP || it.target in installed }
        .map { if (it.launchPackage != null && it.launchPackage !in installed) it.withLaunch(null) else it }

// One rule per line, fields separated by tabs. Package names and domains never contain either.
private const val FIELD = '\t'

fun encodeRules(rules: List<Rule>): String = rules.joinToString("\n") { rule ->
    listOf(rule.target, if (rule.enabled) "1" else "0", rule.action.name, rule.launchPackage.orEmpty())
        .joinToString(FIELD.toString())
}

/** Lines that cannot be read are skipped rather than failing the whole list. */
fun decodeRules(text: String): List<Rule> = text.lineSequence().mapNotNull { line ->
    val fields = line.split(FIELD)
    val target = fields.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
    val action = RuleAction.entries.firstOrNull { it.name == fields.getOrNull(2) } ?: RuleAction.CONFIRM
    val launch = fields.getOrNull(3)?.takeIf { it.isNotEmpty() }
    Rule(target, enabled = fields.getOrNull(1) != "0").withLaunch(if (action == RuleAction.LAUNCH) launch else null)
}.distinctBy { it.target }.toList()

/** Rules from the version 1 keys: every selected app, enabled, showing the confirm popup. */
fun legacyAppRules(targetPackages: Set<String>?): List<Rule> =
    targetPackages.orEmpty().sorted().map { Rule(it) }

/** Rules from the version 1 keys: a domain in [disabled] becomes a disabled rule. */
fun legacyWebsiteRules(domains: Set<String>?, disabled: Set<String>?): List<Rule> =
    domains.orEmpty().sorted().map { Rule(it, enabled = it !in disabled.orEmpty()) }
