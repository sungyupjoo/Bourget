package com.example.bourget.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** Stores only the package names the user selected. Nothing else is persisted. */
class TargetAppsStore(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    val targetPackages: Flow<Set<String>> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[TARGET_PACKAGES] ?: emptySet() }

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

    private companion object {
        val TARGET_PACKAGES = stringSetPreferencesKey("target_packages")
    }
}
