package com.sluer.app.ui.screens

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sluer.app.core.ShizukuShell
import com.sluer.app.core.ShellStatus

private fun readMemory(context: Context): Pair<Long, Long> {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val info = ActivityManager.MemoryInfo()
    am.getMemoryInfo(info)
    return info.totalMem to info.availMem
}

private fun readStorage(): Pair<Long, Long> {
    val stat = StatFs(Environment.getDataDirectory().absolutePath)
    return stat.totalBytes to stat.availableBytes
}

private fun formatGb(bytes: Long): String =
    "%.2f GB".format(bytes.toDouble() / (1024.0 * 1024.0 * 1024.0))

@Composable
fun HomeScreen(onOpenConsole: () -> Unit) {
    val context = LocalContext.current
    val status by ShizukuShell.status.collectAsState()
    val lastError by ShizukuShell.lastError.collectAsState()

    LaunchedEffect(Unit) { ShizukuShell.refresh(context) }

    var refreshKey by remember { mutableIntStateOf(0) }
    val memory = remember(refreshKey) { readMemory(context) }
    val storage = remember(refreshKey) { readStorage() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Sluer", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Device optimizer powered by Shizuku",
            style = MaterialTheme.typography.bodyMedium,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Shizuku", style = MaterialTheme.typography.titleMedium)
                StatusRow("Service", if (ShizukuShell.binderAlive()) "running" else "not running")
                StatusRow(
                    "Permission",
                    if (ShizukuShell.hasPermission()) "granted" else "not granted",
                )
                StatusRow(
                    "Identity",
                    when (val uid = ShizukuShell.shizukuUid()) {
                        0 -> "root (uid 0)"
                        2000 -> "adb shell (uid 2000)"
                        -1 -> "-"
                        else -> "uid $uid"
                    },
                )
                StatusRow(
                    "API version",
                    ShizukuShell.shizukuVersion().let { if (it < 0) "-" else "v$it" },
                )
                StatusRow(
                    "Runner service",
                    when (status) {
                        ShellStatus.READY -> "connected"
                        ShellStatus.CONNECTING -> "connecting..."
                        ShellStatus.PERMISSION_REQUIRED -> "permission needed"
                        ShellStatus.PERMISSION_DENIED -> "permission denied"
                        ShellStatus.SHIZUKU_NOT_RUNNING -> "start Shizuku app first"
                        ShellStatus.ERROR -> lastError?.let { "error: $it" } ?: "error - check Shizuku app"
                        ShellStatus.UNKNOWN -> "unknown"
                    },
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { ShizukuShell.refresh(context) }) {
                        Text("Reconnect")
                    }
                    if (status == ShellStatus.PERMISSION_REQUIRED || status == ShellStatus.PERMISSION_DENIED) {
                        Button(onClick = { ShizukuShell.requestPermission() }) {
                            Text("Grant permission")
                        }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Device", style = MaterialTheme.typography.titleMedium)
                StatusRow("RAM free", "${formatGb(memory.second)} / ${formatGb(memory.first)}")
                StatusRow("Storage free", "${formatGb(storage.second)} / ${formatGb(storage.first)}")
                StatusRow("CPU cores", Runtime.getRuntime().availableProcessors().toString())
                StatusRow("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = { refreshKey++ }) {
                    Text("Refresh stats")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Tools", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Run shell commands with Shizuku privileges to verify the connection.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(4.dp))
                Button(onClick = onOpenConsole) {
                    Text("Open console")
                }
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(120.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}
