package com.sluer.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.sluer.app.core.loadPackages
import kotlinx.coroutines.launch

private val STANDBY_BUCKETS = listOf(
    "active" to 10,
    "working_set" to 20,
    "frequent" to 30,
    "rare" to 40,
    "restricted" to 45,
)

private fun bucketName(output: String): String? {
    val code = output.trim().toIntOrNull() ?: return null
    return STANDBY_BUCKETS.firstOrNull { it.second == code }?.first ?: "code $code"
}

@Composable
fun BatteryScreen() {
    var mode by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Battery", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Control Doze bypass and per-app standby buckets.",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == 0,
                onClick = { mode = 0 },
                label = { Text("Doze whitelist") },
            )
            FilterChip(
                selected = mode == 1,
                onClick = { mode = 1 },
                label = { Text("Standby buckets") },
            )
        }
        if (mode == 0) DozeSection() else BucketSection()
    }
}

@Composable
private fun ColumnScope.DozeSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var entries by remember { mutableStateOf<List<String>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }

    suspend fun reload() {
        val out = ShizukuShell.exec("cmd deviceidle whitelist")
        entries = out.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("!") }
            .map { it.substringBefore(',').trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
            .toList()
    }

    LaunchedEffect(Unit) { reload() }

    fun change(cmd: String) {
        if (busy) return
        busy = true
        scope.launch {
            val out = ShizukuShell.exec(cmd).trim()
            note = "$ $cmd" + if (out.isNotBlank()) "\n$out" else ""
            reload()
            busy = false
        }
    }

    Text(
        "Battery-optimization exempt apps. These are allowed to run in the background during Doze.",
        style = MaterialTheme.typography.bodySmall,
    )
    if (note.isNotBlank()) {
        Text(note, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
    }

    when (val list = entries) {
        null -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        else -> {
            Text("${list.size} whitelisted", style = MaterialTheme.typography.labelMedium)
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(list, key = { it }) { pkg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            pkg,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = { change("cmd deviceidle whitelist -$pkg") },
                            enabled = !busy,
                            contentPadding = PaddingValues(horizontal = 8.dp),
                        ) { Text("Remove") }
                    }
                    HorizontalDivider()
                }
            }
            Button(
                onClick = { showPicker = true },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add app to whitelist")
            }
        }
    }

    if (showPicker) {
        AppPickerDialog(
            onDismiss = { showPicker = false },
            onPick = { pkg ->
                showPicker = false
                change("cmd deviceidle whitelist +$pkg")
            },
        )
    }
}

@Composable
private fun AppPickerDialog(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<PkgInfo>?>(null) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { apps = loadPackages(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick an app") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
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
                        val filtered = list.filter {
                            query.isBlank() ||
                                it.label.contains(query, ignoreCase = true) ||
                                it.pkg.contains(query, ignoreCase = true)
                        }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                        ) {
                            items(filtered, key = { it.pkg }) { p ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onPick(p.pkg) }
                                        .padding(vertical = 8.dp),
                                ) {
                                    Text(p.label, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                                    Text(
                                        p.pkg,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun ColumnScope.BucketSection() {
    val context = LocalContext.current

    var apps by remember { mutableStateOf<List<PkgInfo>?>(null) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<PkgInfo?>(null) }

    LaunchedEffect(Unit) { apps = loadPackages(context).filter { !it.isSystem } }

    Text(
        "Standby buckets decide how aggressively Android limits an app in the background. active = barely limited, restricted = heavily limited.",
        style = MaterialTheme.typography.bodySmall,
    )
    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        label = { Text("Search apps") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
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
            val filtered = list.filter {
                query.isBlank() ||
                    it.label.contains(query, ignoreCase = true) ||
                    it.pkg.contains(query, ignoreCase = true)
            }
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(filtered, key = { it.pkg }) { p ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = p }
                            .padding(vertical = 8.dp),
                    ) {
                        Text(p.label, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                        Text(
                            p.pkg,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    selected?.let { info ->
        BucketDialog(info = info, onDismiss = { selected = null })
    }
}

@Composable
private fun BucketDialog(info: PkgInfo, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf<String?>(null) }
    var choice by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(info.pkg) {
        val out = ShizukuShell.exec("am get-standby-bucket ${info.pkg}")
        current = bucketName(out) ?: out.trim()
    }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(info.label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Current bucket: ${current ?: "checking..."}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    info.pkg,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                STANDBY_BUCKETS.forEach { (name, _) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = choice == name, onClick = { choice = name })
                        Text(name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val target = choice ?: return@TextButton
                    busy = true
                    scope.launch {
                        ShizukuShell.exec("am set-standby-bucket ${info.pkg} $target")
                        busy = false
                        onDismiss()
                    }
                },
                enabled = choice != null && !busy,
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") }
        },
    )
}
