package com.example.bourget.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

data class WebsiteRule(val domain: String, val enabled: Boolean)

/**
 * Stores only the user's rules: selected app package names and website rule domains.
 * Visited URLs or anything read from the screen are never persisted.
 */
class TargetAppsStore(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    private val prefs: Flow<Preferences> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }

    val targetPackages: Flow<Set<String>> = prefs.map { it[TARGET_PACKAGES] ?: emptySet() }

    val websiteRules: Flow<List<WebsiteRule>> = prefs.map { p ->
        val disabled = p[WEBSITE_DISABLED] ?: emptySet()
        (p[WEBSITE_DOMAINS] ?: emptySet()).sorted().map { WebsiteRule(it, it !in disabled) }
    }

    suspend fun setSelected(packageName: String, selected: Boolean) {
        dataStore.edit { prefs ->
            val current = prefs[TARGET_PACKAGES] ?: emptySet()
            prefs[TARGET_PACKAGES] = if (selected) current + packageName else current - packageName
        }
    }

    /** Drops selected packages that are no longer in [installed] (uninstalled or disabled apps). */
    suspend fun retainOnly(installed: Set<String>) {
        dataStore.edit { prefs ->
            val current = prefs[TARGET_PACKAGES] ?: return@edit
            val kept = current intersect installed
            if (kept != current) prefs[TARGET_PACKAGES] = kept
        }
    }

    /** Adds an enabled rule for [domain] (already normalized). */
    suspend fun addWebsite(domain: String) {
        dataStore.edit { prefs ->
            prefs[WEBSITE_DOMAINS] = (prefs[WEBSITE_DOMAINS] ?: emptySet()) + domain
            prefs[WEBSITE_DISABLED] = (prefs[WEBSITE_DISABLED] ?: emptySet()) - domain
        }
    }

    suspend fun removeWebsite(domain: String) {
        dataStore.edit { prefs ->
            prefs[WEBSITE_DOMAINS] = (prefs[WEBSITE_DOMAINS] ?: emptySet()) - domain
            prefs[WEBSITE_DISABLED] = (prefs[WEBSITE_DISABLED] ?: emptySet()) - domain
        }
    }

    suspend fun setWebsiteEnabled(domain: String, enabled: Boolean) {
        dataStore.edit { prefs ->
            val disabled = prefs[WEBSITE_DISABLED] ?: emptySet()
            prefs[WEBSITE_DISABLED] = if (enabled) disabled - domain else disabled + domain
        }
    }

    private companion object {
        val TARGET_PACKAGES = stringSetPreferencesKey("target_packages")
        val WEBSITE_DOMAINS = stringSetPreferencesKey("website_domains")
        val WEBSITE_DISABLED = stringSetPreferencesKey("website_disabled")
    }
}
