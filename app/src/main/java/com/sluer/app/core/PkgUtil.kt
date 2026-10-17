package com.sluer.app.core

import android.content.Context

data class PkgInfo(
    val pkg: String,
    val label: String,
    val isSystem: Boolean,
    val disabled: Boolean,
)

/**
 * Packages that must not be disabled/uninstalled: doing so can make the
 * device unusable (boot loops, no launcher, broken settings).
 */
private val CRITICAL_PACKAGES = setOf(
    "android",
    "com.android.systemui",
    "com.android.settings",
    "com.android.phone",
    "com.android.shell",
    "com.android.providers.settings",
    "com.android.providers.telephony",
    "com.android.providers.contacts",
    "com.android.providers.media",
    "com.android.providers.downloads",
    "com.android.packageinstaller",
    "com.google.android.packageinstaller",
    "com.android.permissioncontroller",
    "com.google.android.permissioncontroller",
    "com.google.android.gms",
    "com.google.android.gsf",
    "com.android.launcher3",
    "com.google.android.apps.nexuslauncher",
)

fun isCriticalPackage(pkg: String): Boolean = pkg in CRITICAL_PACKAGES

private fun parsePkgLines(output: String): Set<String> =
    output.lineSequence()
        .map { it.trim() }
        .filter { it.startsWith("package:") }
        .map { it.removePrefix("package:").trim() }
        .filter { it.isNotEmpty() }
        .toSet()

/**
 * Loads every installed package with a human-readable label.
 * Runs shell commands, so call from a coroutine (IO dispatcher).
 */
suspend fun loadPackages(context: Context): List<PkgInfo> {
    val system = parsePkgLines(ShizukuShell.exec("pm list packages -s"))
    val user = parsePkgLines(ShizukuShell.exec("pm list packages -3"))
    val disabled = parsePkgLines(ShizukuShell.exec("pm list packages -d"))
    val pm = context.packageManager
    return (system + user).map { pkg ->
        val label = try {
            pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString()
        } catch (t: Throwable) {
            pkg
        }
        PkgInfo(
            pkg = pkg,
            label = label,
            isSystem = pkg in system,
            disabled = pkg in disabled,
        )
    }.sortedBy { it.label.lowercase() }
}
