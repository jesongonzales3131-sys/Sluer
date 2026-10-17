package com.sluer.app.core

import android.content.Context

/**
 * Persistent on-device state: the force-stop kill list for Boost and the
 * list of packages uninstalled for user 0 (so they can be restored later).
 */
object SluerPrefs {

    private const val FILE = "sluer_state"
    private const val KEY_KILL = "kill_list"
    private const val KEY_UNINSTALLED = "uninstalled_log"

    private val DEFAULT_KILL_LIST = listOf(
        "com.facebook.katana",
        "com.facebook.orca",
        "com.instagram.android",
        "com.zhiliaoapp.musically",
        "com.snapchat.android",
        "com.discord",
        "com.spotify.music",
        "com.netflix.mediaclient",
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun killList(context: Context): List<String> {
        val p = prefs(context)
        if (!p.contains(KEY_KILL)) {
            p.edit().putStringSet(KEY_KILL, DEFAULT_KILL_LIST.toSet()).apply()
            return DEFAULT_KILL_LIST.sorted()
        }
        return (p.getStringSet(KEY_KILL, emptySet()) ?: emptySet()).sorted()
    }

    fun saveKillList(context: Context, list: List<String>) {
        prefs(context).edit().putStringSet(KEY_KILL, list.toSet()).apply()
    }

    fun uninstalled(context: Context): List<String> =
        (prefs(context).getStringSet(KEY_UNINSTALLED, emptySet()) ?: emptySet()).sorted()

    fun addUninstalled(context: Context, pkg: String) {
        val updated = uninstalled(context).toMutableSet().apply { add(pkg) }
        prefs(context).edit().putStringSet(KEY_UNINSTALLED, updated).apply()
    }

    fun removeUninstalled(context: Context, pkg: String) {
        val updated = uninstalled(context).toMutableSet().apply { remove(pkg) }
        prefs(context).edit().putStringSet(KEY_UNINSTALLED, updated).apply()
    }
}
