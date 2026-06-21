package com.desn1k.vlessapp.prefs

import android.content.Context

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Small SharedPreferences wrapper for the handful of user settings that don't need Room. */
object AppPreferences {

    private const val PREFS_NAME = "vlessapp_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_SUBSCRIPTIONS = "subscriptions"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getThemeMode(context: Context): ThemeMode =
        runCatching { ThemeMode.valueOf(prefs(context).getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)!!) }
            .getOrDefault(ThemeMode.SYSTEM)

    fun setThemeMode(context: Context, mode: ThemeMode) {
        prefs(context).edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun getSubscriptions(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_SUBSCRIPTIONS, emptySet())?.toSet() ?: emptySet()

    fun addSubscription(context: Context, url: String) {
        val current = getSubscriptions(context).toMutableSet()
        current.add(url)
        prefs(context).edit().putStringSet(KEY_SUBSCRIPTIONS, current).apply()
    }

    fun removeSubscription(context: Context, url: String) {
        val current = getSubscriptions(context).toMutableSet()
        current.remove(url)
        prefs(context).edit().putStringSet(KEY_SUBSCRIPTIONS, current).apply()
    }
}
