package com.sluer.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sluer.app.core.ShizukuShell
import kotlinx.coroutines.launch

private data class AnimSetting(val key: String, val label: String)

private val ANIM_SETTINGS = listOf(
    AnimSetting("window_animation_scale", "Window animation"),
    AnimSetting("transition_animation_scale", "Transition animation"),
    AnimSetting("animator_duration_scale", "Animator duration"),
)

private val ANIM_PRESETS = listOf("0" to "Off", "0.5" to "0.5x", "1" to "1x")

@Composable
fun TweaksScreen() {
    val scope = rememberCoroutineScope()

    // Animations
    var animValues by remember { mutableStateOf<Map<String, String>?>(null) }
    var animNote by remember { mutableStateOf("") }

    suspend fun loadAnim() {
        animValues = ANIM_SETTINGS.associate { s ->
            s.key to ShizukuShell.exec("settings get global ${s.key}").trim()
        }
    }
    LaunchedEffect(Unit) { loadAnim() }

    // Private DNS
    var dnsMode by remember { mutableStateOf<String?>(null) }
    var dnsSpecifier by remember { mutableStateOf("") }
    var dnsNote by remember { mutableStateOf("") }
    var showCustomDns by remember { mutableStateOf(false) }

    suspend fun loadDns() {
        dnsMode = ShizukuShell.exec("settings get global private_dns_mode").trim()
        dnsSpecifier = ShizukuShell.exec("settings get global private_dns_specifier").trim()
    }
    LaunchedEffect(Unit) { loadDns() }

    // Cached process limit
    var procLimit by remember { mutableStateOf<String?>(null) }
    var procNote by remember { mutableStateOf("") }

    suspend fun loadProc() {
        procLimit = ShizukuShell.exec("device_config get activity_manager max_cached_processes").trim()
    }
    LaunchedEffect(Unit) { loadProc() }

    fun applyAnim(key: String, value: String) {
        scope.launch {
            val cmd = "settings put global $key $value"
            val out = ShizukuShell.exec(cmd).trim()
            animNote = cmd + if (out.isNotBlank()) "\n$out" else ""
            loadAnim()
        }
    }

    fun applyDns(cmd: String) {
        scope.launch {
            val out = ShizukuShell.exec(cmd).trim()
            dnsNote = cmd + if (out.isNotBlank()) "\n$out" else ""
            loadDns()
        }
    }

    fun applyProc(cmd: String) {
        scope.launch {
            val out = ShizukuShell.exec(cmd).trim()
            procNote = cmd + if (out.isNotBlank()) "\n$out" else ""
            loadProc()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Tweaks", style = MaterialTheme.typography.headlineSmall)
        Text("System-level tweaks applied through Shizuku.", style = MaterialTheme.typography.bodySmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Animations", style = MaterialTheme.typography.titleMedium)
                ANIM_SETTINGS.forEach { s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(
                            animValues?.get(s.key) ?: "...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ANIM_PRESETS.forEach { (value, label) ->
                            OutlinedButton(onClick = { applyAnim(s.key, value) }) {
                                Text(label)
                            }
                        }
                    }
                }
                if (animNote.isNotBlank()) {
                    Text(animNote, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Private DNS", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Mode: ${dnsMode ?: "..."}" +
                        if (dnsMode == "hostname") " ($dnsSpecifier)" else "",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = {
                        applyDns("settings put global private_dns_mode off")
                    }) { Text("Off") }
                    OutlinedButton(onClick = {
                        applyDns("settings put global private_dns_mode automatic")
                    }) { Text("Automatic") }
                    OutlinedButton(onClick = {
                        applyDns(
                            "settings put global private_dns_mode hostname && " +
                                "settings put global private_dns_specifier dns.google"
                        )
                    }) { Text("dns.google") }
                    OutlinedButton(onClick = {
                        applyDns(
                            "settings put global private_dns_mode hostname && " +
                                "settings put global private_dns_specifier 1dot1dot1dot1.cloudflare-dns.com"
                        )
                    }) { Text("Cloudflare") }
                    OutlinedButton(onClick = { showCustomDns = true }) { Text("Custom...") }
                }
                if (dnsNote.isNotBlank()) {
                    Text(dnsNote, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Cached process limit", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Current limit: " + (procLimit ?: "...").let { if (it == "null") "default" else it },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Lower values make Android reclaim memory more aggressively. 0 is not recommended.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("8", "16", "32").forEach { n ->
                        OutlinedButton(onClick = {
                            applyProc("device_config put activity_manager max_cached_processes $n")
                        }) { Text(n) }
                    }
                    OutlinedButton(onClick = {
                        applyProc("device_config delete activity_manager max_cached_processes")
                    }) { Text("Default") }
                }
                if (procNote.isNotBlank()) {
                    Text(procNote, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        }
    }

    if (showCustomDns) {
        var host by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCustomDns = false },
            title = { Text("Custom DNS hostname") },
            text = {
                Column {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("Hostname") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Example: dns.google", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val h = host.trim()
                        if (h.isNotEmpty()) {
                            applyDns(
                                "settings put global private_dns_mode hostname && " +
                                    "settings put global private_dns_specifier $h"
                            )
                        }
                        showCustomDns = false
                    },
                    enabled = host.isNotBlank(),
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDns = false }) { Text("Cancel") }
            },
        )
    }
}
