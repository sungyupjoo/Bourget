package com.example.bourget.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * Version of the in-app disclosure (what the service reads and stores). Bump it whenever that
 * changes: every user must then agree again before the service processes anything.
 * 1: first release (app package names, the browser address bar, optional backup).
 */
const val DISCLOSURE_VERSION = 1

/**
 * Stores only what the user made: app and website rules (target, on/off, action, app to launch),
 * the confirm popup template, whether the intro was seen, whether backup is on, and which
 * disclosure version the user agreed to. Detected apps,
 * visited URLs or anything read from the screen are never persisted.
 */
class TargetAppsStore(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    private val prefs: Flow<Preferences> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }

    /** Rules in screen order (newest first). */
    fun rules(kind: RuleKind): Flow<List<Rule>> = prefs.map { it.rules(kind) }

    val appRules: Flow<List<Rule>> = rules(RuleKind.APP)
    val websiteRules: Flow<List<Rule>> = rules(RuleKind.WEBSITE)

    /** The confirm popup title template the user saved; null when unset or no longer valid. */
    val savedTemplate: Flow<String?> =
        prefs.map { p -> p[CONFIRM_TEMPLATE]?.takeIf { templateError(it) == null } }

    /** [savedTemplate], or [default] (the default text in the current language). */
    fun confirmTemplate(default: String): Flow<String> = savedTemplate.map { it ?: default }

    suspend fun setConfirmTemplate(template: String) {
        dataStore.edit { it[CONFIRM_TEMPLATE] = template }
    }

    /** The disclosure version the user last agreed to; 0 if never. */
    val consentVersion: Flow<Int> = prefs.map { it[CONSENT_VERSION] ?: 0 }

    /** True once the user agreed to the current [DISCLOSURE_VERSION] in the app. */
    val consentedToCurrentDisclosure: Flow<Boolean> =
        prefs.map { (it[CONSENT_VERSION] ?: 0) >= DISCLOSURE_VERSION }

    /** True once the intro and recommended apps were seen. */
    val introDone: Flow<Boolean> = prefs.map { it[INTRO_DONE] ?: false }

    suspend fun recordConsent() {
        dataStore.edit { it[CONSENT_VERSION] = DISCLOSURE_VERSION }
    }

    /** Forgets consent (after a restore to a new device, where the user must agree again). */
    suspend fun clearConsent() {
        dataStore.edit { it.remove(CONSENT_VERSION) }
    }

    /** Whether Android backup may include Bourget's data. Off unless the user turns it on. */
    val backupEnabled: Flow<Boolean> = prefs.map { it[BACKUP_ENABLED] ?: false }

    suspend fun setBackupEnabled(enabled: Boolean) {
        dataStore.edit { it[BACKUP_ENABLED] = enabled }
    }

    suspend fun setIntroDone() {
        dataStore.edit { it[INTRO_DONE] = true }
    }

    suspend fun editRules(kind: RuleKind, transform: (List<Rule>) -> List<Rule>) {
        dataStore.edit { prefs -> prefs[kind.key] = encodeRules(transform(prefs.rules(kind))) }
    }

    /** Adds an enabled "confirm" rule for [target] at the top, unless it already has one. */
    suspend fun addRule(kind: RuleKind, target: String) = editRules(kind) { it.withAdded(Rule(target)) }

    suspend fun removeRule(kind: RuleKind, target: String) = editRules(kind) { it.without(target) }

    /** Saves the order the user dragged the rules into. */
    suspend fun reorderRules(kind: RuleKind, targets: List<String>) = editRules(kind) { it.reordered(targets) }

    suspend fun updateRule(kind: RuleKind, target: String, transform: (Rule) -> Rule) =
        editRules(kind) { it.updated(target, transform) }

    /**
     * Forgets app rules for apps that are gone and launch targets that are gone (see
     * [retainInstalled]). Callers pass an empty set only if the app query really returned nothing.
     */
    suspend fun retainOnly(installed: Set<String>) {
        dataStore.edit { prefs ->
            for (kind in RuleKind.entries) {
                val current = prefs.rules(kind)
                val kept = current.retainInstalled(kind, installed)
                if (kept != current) prefs[kind.key] = encodeRules(kept)
            }
        }
    }

    private val RuleKind.key
        get() = if (this == RuleKind.APP) APP_RULES else WEBSITE_RULES

    private fun Preferences.rules(kind: RuleKind): List<Rule> = this[kind.key]?.let(::decodeRules).orEmpty()

    private companion object {
        val APP_RULES = stringPreferencesKey("app_rules")
        val WEBSITE_RULES = stringPreferencesKey("website_rules")
        val CONFIRM_TEMPLATE = stringPreferencesKey("confirm_template")
        val INTRO_DONE = booleanPreferencesKey("intro_done")
        val BACKUP_ENABLED = booleanPreferencesKey("backup_enabled")
        val CONSENT_VERSION = intPreferencesKey("consent_version")
    }
}
