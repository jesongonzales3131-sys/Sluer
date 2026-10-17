package com.sluer.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.sluer.app.core.ShizukuShell
import com.sluer.app.core.SluerPrefs
import com.sluer.app.core.isCriticalPackage
import com.sluer.app.core.loadPackages
import kotlinx.coroutines.launch

private enum class DebloatFilter(val label: String) {
    ALL("All"),
    USER("User"),
    SYSTEM("System"),
    DISABLED("Disabled"),
}

private enum class ActionKind(val title: String, val confirm: String) {
    DISABLE("Disable package", "Disable"),
    ENABLE("Enable package", "Enable"),
    UNINSTALL("Uninstall for user 0", "Uninstall"),
}

private data class PendingAction(val info: PkgInfo, val kind: ActionKind)

private fun commandFor(action: PendingAction): String = when (action.kind) {
    ActionKind.DISABLE -> "pm disable-user --user 0 ${action.info.pkg}"
    ActionKind.ENABLE -> "pm enable ${action.info.pkg}"
    ActionKind.UNINSTALL -> "pm uninstall --user 0 ${action.info.pkg}"
}

private fun failed(output: String): Boolean =
    output.contains("Failure", ignoreCase = true) || output.trimStart().startsWith("!")

@Composable
fun DebloatScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var packages by remember { mutableStateOf<List<PkgInfo>?>(null) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(DebloatFilter.ALL) }
    var pending by remember { mutableStateOf<PendingAction?>(null) }
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var recentlyRemoved by remember { mutableStateOf(SluerPrefs.uninstalled(context)) }

    suspend fun reload() {
        packages = loadPackages(context)
    }

    LaunchedEffect(Unit) { reload() }

    fun runAction(action: PendingAction) {
        if (busy) return
        busy = true
        scope.launch {
            val cmd = commandFor(action)
            val out = ShizukuShell.exec(cmd).trim()
            note = "$ $cmd\n" + out.ifBlank { "done" }
            if (!failed(out) && action.kind == ActionKind.UNINSTALL) {
                SluerPrefs.addUninstalled(context, action.info.pkg)
                recentlyRemoved = SluerPrefs.uninstalled(context)
            }
            pending = null
            reload()
            busy = false
        }
    }

    fun restore(pkg: String) {
        if (busy) return
        busy = true
        scope.launch {
            val cmd = "pm install-existing --user 0 $pkg"
            val out = ShizukuShell.exec(cmd).trim()
            note = "$ $cmd\n" + out.ifBlank { "done" }
            if (!failed(out)) {
                SluerPrefs.removeUninstalled(context, pkg)
                recentlyRemoved = SluerPrefs.uninstalled(context)
            }
            reload()
            busy = false
        }
    }

    val filtered = (packages ?: emptyList()).filter { p ->
        val matchesQuery = query.isBlank() ||
            p.label.contains(query, ignoreCase = true) ||
            p.pkg.contains(query, ignoreCase = true)
        val matchesFilter = when (filter) {
            DebloatFilter.ALL -> true
            DebloatFilter.USER -> !p.isSystem
            DebloatFilter.SYSTEM -> p.isSystem
            DebloatFilter.DISABLED -> p.disabled
        }
        matchesQuery && matchesFilter
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Debloat", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Disable or uninstall apps for user 0. Nothing is deleted permanently - everything can be restored.",
            style = MaterialTheme.typography.bodySmall,
        )

        if (recentlyRemoved.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("Recently uninstalled (restorable)", style = MaterialTheme.typography.titleSmall)
                    recentlyRemoved.forEach { pkg ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                pkg,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { restore(pkg) }, enabled = !busy) {
                                Text("Restore")
                            }
                        }
                    }
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

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DebloatFilter.entries.forEach { f ->
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = { Text(f.label) },
                )
            }
        }

        if (note.isNotBlank()) {
            Text(note, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        }

        when (val list = packages) {
            null -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                Text("${filtered.size} of ${list.size} packages", style = MaterialTheme.typography.labelMedium)
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filtered, key = { it.pkg }) { p ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.label, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                Text(
                                    p.pkg + if (p.disabled) "  [disabled]" else "",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                            if (p.disabled) {
                                TextButton(
                                    onClick = { pending = PendingAction(p, ActionKind.ENABLE) },
                                    enabled = !busy,
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                ) { Text("Enable") }
                            } else {
                                TextButton(
                                    onClick = { pending = PendingAction(p, ActionKind.DISABLE) },
                                    enabled = !busy,
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                ) { Text("Disable") }
                            }
                            TextButton(
                                onClick = { pending = PendingAction(p, ActionKind.UNINSTALL) },
                                enabled = !busy,
                                contentPadding = PaddingValues(horizontal = 8.dp),
                            ) { Text("Uninstall") }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    pending?.let { action ->
        val critical = isCriticalPackage(action.info.pkg) && action.kind != ActionKind.ENABLE
        AlertDialog(
            onDismissRequest = { if (!busy) pending = null },
            title = { Text(action.kind.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(action.info.label, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "$ " + commandFor(action),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    )
                    if (critical) {
                        Text(
                            "Warning: this looks like a system-critical package. Disabling or removing it can break the device (boot loops, broken settings, no launcher).",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { runAction(action) }, enabled = !busy) {
                    Text(if (critical) "I understand - continue" else action.kind.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }, enabled = !busy) {
                    Text("Cancel")
                }
            },
        )
    }
}
