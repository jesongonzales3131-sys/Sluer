package com.sluer.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sluer.app.core.PkgInfo
import com.sluer.app.core.ShellStatus
import com.sluer.app.core.ShizukuShell
import com.sluer.app.core.SluerPrefs
import com.sluer.app.core.isCriticalPackage
import com.sluer.app.core.loadPackages
import kotlinx.coroutines.launch

/** Popular social/media packages offered as a one-tap freeze preset. */
private val FREEZE_PRESET = listOf(
    "com.facebook.katana",
    "com.facebook.orca",
    "com.instagram.android",
    "com.zhiliaoapp.musically",
    "com.snapchat.android",
    "com.discord",
    "com.spotify.music",
    "com.netflix.mediaclient",
    "com.twitter.android",
)

@Composable
fun FreezeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val shellStatus by ShizukuShell.status.collectAsState()
    val ready = shellStatus == ShellStatus.READY

    var apps by remember { mutableStateOf<List<PkgInfo>?>(null) }
    var frozen by remember { mutableStateOf(SluerPrefs.frozen(context).toSet()) }
    var query by remember { mutableStateOf("") }
    var showFrozenOnly by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var log by remember { mutableStateOf(listOf<String>()) }

    LaunchedEffect(Unit) { apps = loadPackages(context) }

    val installed = apps.orEmpty()

    fun presetTargets(): List<String> =
        installed
            .filter { it.pkg in FREEZE_PRESET && !it.isSystem && it.pkg !in frozen }
            .map { it.pkg }

    fun applyFreeze(pkgs: List<String>, freeze: Boolean, tag: String) {
        if (busy || pkgs.isEmpty()) return
        busy = true
        scope.launch {
            val lines = mutableListOf<String>()
            var ok = 0
            pkgs.forEach { pkg ->
                val cmd = if (freeze) "pm suspend $pkg" else "pm unsuspend $pkg"
                val out = ShizukuShell.exec(cmd).trim()
                val gone = out.contains("Unknown package", ignoreCase = true)
                val failed = !gone && (
                    out.startsWith("!") ||
                        out.contains("Failure", ignoreCase = true) ||
                        out.contains("Error", ignoreCase = true)
                    )
                if (!failed) {
                    ok++
                    lines += "$ $cmd"
                    if (freeze) {
                        if (!gone) SluerPrefs.addFrozen(context, pkg)
                        val kill = ShizukuShell.exec("am force-stop $pkg").trim()
                        if (kill.isNotBlank() && !kill.startsWith("!")) lines += kill
                    } else {
                        SluerPrefs.removeFrozen(context, pkg)
                    }
                } else {
                    lines += "$ $cmd"
                    if (out.isNotBlank()) lines += out
                }
            }
            frozen = SluerPrefs.frozen(context).toSet()
            log = (listOf("$tag: $ok/${pkgs.size} ok") + lines + log).take(20)
            busy = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Freeze", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Suspended apps are fully stopped in the background but keep their data and logins. Unfreeze anytime.",
            style = MaterialTheme.typography.bodySmall,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Presets", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { applyFreeze(presetTargets(), true, "Freeze preset") },
                        enabled = ready && !busy && presetTargets().isNotEmpty(),
                    ) {
                        Text("Freeze social & media")
                    }
                    OutlinedButton(
                        onClick = { applyFreeze(frozen.sorted(), false, "Unfreeze all") },
                        enabled = ready && !busy && frozen.isNotEmpty(),
                    ) {
                        Text("Unfreeze all")
                    }
                }
                Text("${frozen.size} apps frozen", style = MaterialTheme.typography.labelMedium)
                if (!ready) {
                    Text(
                        "Waiting for shell connection...",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search apps") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        FilterChip(
            selected = showFrozenOnly,
            onClick = { showFrozenOnly = !showFrozenOnly },
            label = { Text("Frozen only") },
        )

        when (val list = apps) {
            null -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                val filtered = list
                    .filter {
                        query.isBlank() ||
                            it.label.contains(query, ignoreCase = true) ||
                            it.pkg.contains(query, ignoreCase = true)
                    }
                    .filter { !showFrozenOnly || it.pkg in frozen }
                if (filtered.isEmpty()) {
                    Text("No apps match.", style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filtered, key = { it.pkg }) { p ->
                            FreezeRow(
                                info = p,
                                isFrozen = p.pkg in frozen,
                                enabled = ready && !busy,
                                onToggle = { desired -> applyFreeze(listOf(p.pkg), desired, p.label) },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }

        if (log.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Last actions", style = MaterialTheme.typography.titleMedium)
                    Text(
                        log.joinToString("\n"),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        maxLines = 14,
                    )
                }
            }
        }
    }
}

@Composable
private fun FreezeRow(
    info: PkgInfo,
    isFrozen: Boolean,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val blocked = info.isSystem || isCriticalPackage(info.pkg)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(info.label, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                if (isFrozen) {
                    Text(
                        "frozen",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
            Text(
                info.pkg,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            if (blocked) {
                Text(
                    "system app - freezing disabled",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        TextButton(onClick = { onToggle(!isFrozen) }, enabled = enabled && !blocked) {
            Text(if (isFrozen) "Unfreeze" else "Freeze")
        }
    }
}
