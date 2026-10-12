package com.winlator.star.store

import android.content.Context
import androidx.preference.PreferenceManager

/**
 * "Show mature content" for the storefronts — the store's own 17+/18+ ratings and adult tags,
 * hidden from the Deck rails unless the user turns this on. Off by default; read live by the
 * storefront filter, so a toggle in the Downloads pane applies the moment it flips. Mirrors
 * DroidDeck's `SessionPrefs.storesShowMature`.
 */
object StoreMatureSetting {

    /** Default-SharedPreferences key. */
    const val PREF_KEY = "storesShowMature"

    @JvmStatic
    fun get(ctx: Context): Boolean =
        try {
            PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(PREF_KEY, false)
        } catch (_: Throwable) {
            false
        }

    @JvmStatic
    fun set(ctx: Context, show: Boolean) {
        try {
            PreferenceManager.getDefaultSharedPreferences(ctx).edit().putBoolean(PREF_KEY, show).apply()
        } catch (_: Throwable) {
        }
    }
}
