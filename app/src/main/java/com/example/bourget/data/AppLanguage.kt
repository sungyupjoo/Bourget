package com.example.bourget.data

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/** Bourget's own language: the system's, Korean or English. */
enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    KOREAN("ko"),
    ENGLISH("en");

    companion object {
        private const val PREFS = "language"
        private const val KEY_TAG = "tag"

        /**
         * Android 13+: the system keeps the choice per app (it also appears in the system's app
         * language settings). Older versions: a small preferences file of Bourget's own.
         */
        fun current(context: Context): AppLanguage {
            val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.getSystemService(LocaleManager::class.java).applicationLocales.toLanguageTags()
            } else {
                prefs(context).getString(KEY_TAG, "").orEmpty()
            }
            return entries.firstOrNull { it.tag.isNotEmpty() && tag.startsWith(it.tag) } ?: SYSTEM
        }

        /** Applies [language] and redraws [activity] in it. */
        fun set(activity: Activity, language: AppLanguage) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // The system recreates the activity itself.
                activity.getSystemService(LocaleManager::class.java).applicationLocales =
                    LocaleList.forLanguageTags(language.tag)
            } else {
                prefs(activity).edit().putString(KEY_TAG, language.tag).apply()
                activity.recreate()
            }
        }

        /**
         * Before Android 13: [base] with the chosen language applied, for activities
         * (attachBaseContext) and for the accessibility service's popup. From 13 the system
         * applies it to the whole app, so [base] is returned as is.
         */
        fun wrap(base: Context): Context {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
            val tag = prefs(base).getString(KEY_TAG, "").orEmpty()
            if (tag.isEmpty()) return base
            val config = Configuration(base.resources.configuration)
            config.setLocale(Locale.forLanguageTag(tag))
            return base.createConfigurationContext(config)
        }

        /**
         * For the accessibility service: [base] in the current language, read again on every call.
         * A running service keeps the resources it started with even after the app language
         * changes on Android 13+, so the language is applied here explicitly instead.
         */
        fun localized(base: Context): Context {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return wrap(base)
            val localeManager = base.getSystemService(LocaleManager::class.java)
            val locales = localeManager.applicationLocales.takeIf { !it.isEmpty } ?: localeManager.systemLocales
            val config = Configuration(base.resources.configuration)
            config.setLocales(locales)
            return base.createConfigurationContext(config)
        }

        private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }
}
