package com.example.bourget.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * Version of the in-app disclosure (what the service reads and stores). Bump it whenever that
 * changes: every user must then agree again before the service processes anything.
 * 1: app package names only (never recorded). 2: adds the browser address bar.
 */
const val DISCLOSURE_VERSION = 2

/**
 * Stores only what the user made: app and website rules (target, on/off, action, app to launch),
 * whether the intro was seen, and which disclosure version the user agreed to. Detected apps,
 * visited URLs or anything read from the screen are never persisted.
 */
class TargetAppsStore(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    private val prefs: Flow<Preferences> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }

    /** Rules in screen order (newest first). Reads the version 1 keys until they are migrated. */
    fun rules(kind: RuleKind): Flow<List<Rule>> = prefs.map { it.rules(kind) }

    val appRules: Flow<List<Rule>> = rules(RuleKind.APP)
    val websiteRules: Flow<List<Rule>> = rules(RuleKind.WEBSITE)

    /** True once the user agreed to the current [DISCLOSURE_VERSION] in the app. */
    val consentedToCurrentDisclosure: Flow<Boolean> =
        prefs.map { (it[CONSENT_VERSION] ?: 0) >= DISCLOSURE_VERSION }

    /** True once the intro and recommended apps were seen, or the user already had rules. */
    val introDone: Flow<Boolean> = prefs.map { p ->
        p[INTRO_DONE] ?: (p.rules(RuleKind.APP).isNotEmpty() || p.rules(RuleKind.WEBSITE).isNotEmpty() ||
            (p[CONSENT_VERSION] ?: 0) > 0)
    }

    suspend fun recordConsent() {
        dataStore.edit { it[CONSENT_VERSION] = DISCLOSURE_VERSION }
    }

    suspend fun setIntroDone() {
        dataStore.edit { it[INTRO_DONE] = true }
    }

    /** Moves rules from the version 1 keys to the new ones and deletes the old keys. */
    suspend fun migrate() {
        dataStore.edit { it.migrate() }
    }

    suspend fun editRules(kind: RuleKind, transform: (List<Rule>) -> List<Rule>) {
        dataStore.edit { prefs ->
            prefs.migrate()
            prefs[kind.key] = encodeRules(transform(prefs.rules(kind)))
        }
    }

    /** Adds an enabled "confirm" rule for [target] at the top, unless it already has one. */
    suspend fun addRule(kind: RuleKind, target: String) = editRules(kind) { it.withAdded(Rule(target)) }

    suspend fun removeRule(kind: RuleKind, target: String) = editRules(kind) { it.without(target) }

    suspend fun updateRule(kind: RuleKind, target: String, transform: (Rule) -> Rule) =
        editRules(kind) { it.updated(target, transform) }

    /**
     * Forgets app rules for apps that are gone and launch targets that are gone (see
     * [retainInstalled]). Callers pass an empty set only if the app query really returned nothing.
     */
    suspend fun retainOnly(installed: Set<String>) {
        dataStore.edit { prefs ->
            prefs.migrate()
            for (kind in RuleKind.entries) {
                val current = prefs.rules(kind)
                val kept = current.retainInstalled(kind, installed)
                if (kept != current) prefs[kind.key] = encodeRules(kept)
            }
        }
    }

    private val RuleKind.key
        get() = if (this == RuleKind.APP) APP_RULES else WEBSITE_RULES

    private fun Preferences.rules(kind: RuleKind): List<Rule> = this[kind.key]?.let(::decodeRules)
        ?: when (kind) {
            RuleKind.APP -> legacyAppRules(this[TARGET_PACKAGES])
            RuleKind.WEBSITE -> legacyWebsiteRules(this[WEBSITE_DOMAINS], this[WEBSITE_DISABLED])
        }

    private fun MutablePreferences.migrate() {
        if (TARGET_PACKAGES in this || WEBSITE_DOMAINS in this || WEBSITE_DISABLED in this) {
            // Users from version 1 already set up their rules, so they skip the intro.
            if (this[INTRO_DONE] == null) this[INTRO_DONE] = true
        }
        if (this[APP_RULES] == null) this[APP_RULES] = encodeRules(rules(RuleKind.APP))
        if (this[WEBSITE_RULES] == null) this[WEBSITE_RULES] = encodeRules(rules(RuleKind.WEBSITE))
        remove(TARGET_PACKAGES)
        remove(WEBSITE_DOMAINS)
        remove(WEBSITE_DISABLED)
    }

    private companion object {
        val APP_RULES = stringPreferencesKey("app_rules")
        val WEBSITE_RULES = stringPreferencesKey("website_rules")
        val INTRO_DONE = booleanPreferencesKey("intro_done")
        val CONSENT_VERSION = intPreferencesKey("consent_version")

        // Version 1 keys, read only to migrate.
        val TARGET_PACKAGES = stringSetPreferencesKey("target_packages")
        val WEBSITE_DOMAINS = stringSetPreferencesKey("website_domains")
        val WEBSITE_DISABLED = stringSetPreferencesKey("website_disabled")
    }
}
