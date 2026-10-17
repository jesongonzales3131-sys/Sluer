package com.sluer.app.ui.screens

import android.app.ActivityManager
import android.content.Context
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.sluer.app.core.ShizukuShell
import com.sluer.app.core.SluerPrefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun availMem(context: Context): Long {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val info = ActivityManager.MemoryInfo()
    am.getMemoryInfo(info)
    return info.availMem
}

private fun fmtGb(bytes: Long): String =
    "%.2f GB".format(bytes.toDouble() / (1024.0 * 1024.0 * 1024.0))

@Composable
fun BoostScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var killList by remember { mutableStateOf(SluerPrefs.killList(context)) }
    var newPkg by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }
    var log by remember { mutableStateOf("") }
    var memBefore by remember { mutableStateOf<Long?>(null) }
    var memAfter by remember { mutableStateOf<Long?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val memNow = remember(refreshKey) { availMem(context) }

    fun updateKillList(list: List<String>) {
        killList = list
        SluerPrefs.saveKillList(context, list)
    }

    fun boost() {
        if (running) return
        running = true
        memBefore = availMem(context)
        memAfter = null
        scope.launch {
            val sb = StringBuilder()
            fun step(cmd: String, out: String) {
                sb.append("$ ").append(cmd).append('\n')
                if (out.isNotBlank()) sb.append(out.trim()).append('\n')
            }
            step("am kill-all", ShizukuShell.exec("am kill-all"))
            killList.forEach { pkg ->
                step("am force-stop $pkg", ShizukuShell.exec("am force-stop $pkg"))
            }
            delay(1500)
            memAfter = availMem(context)
            log = sb.toString().trim()
            refreshKey++
            running = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Boost", style = MaterialTheme.typography.headlineSmall)
        Text("Free memory and stop background apps", style = MaterialTheme.typography.bodySmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Memory", style = MaterialTheme.typography.titleMedium)
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Free now", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(110.dp))
                    Text(fmtGb(memNow), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
                memBefore?.let { before ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("Before", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(110.dp))
                        Text(fmtGb(before), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    }
                }
                memAfter?.let { after ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text("After", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(110.dp))
                        Text(fmtGb(after), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    }
                    val before = memBefore ?: after
                    val deltaMb = (after - before) / (1024.0 * 1024.0)
                    Text(
                        if (deltaMb >= 0) "Freed %.0f MB".format(deltaMb) else "Delta %.0f MB".format(deltaMb),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = { boost() },
                    enabled = !running,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (running) "Boosting..." else "Boost now")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Apps to force-stop", style = MaterialTheme.typography.titleMedium)
                Text(
                    "These apps are force-stopped when you boost. Add or remove package names to match your needs.",
                    style = MaterialTheme.typography.bodySmall,
                )
                killList.forEach { pkg ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            pkg,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { updateKillList(killList - pkg) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove")
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = newPkg,
                        onValueChange = { newPkg = it },
                        label = { Text("Package name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    Button(
                        onClick = {
                            val p = newPkg.trim()
                            if (p.isNotEmpty() && p !in killList) {
                                updateKillList((killList + p).sorted())
                            }
                            newPkg = ""
                        },
                        enabled = newPkg.trim().isNotEmpty(),
                    ) {
                        Text("Add")
                    }
                }
            }
        }

        if (log.isNotBlank()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Last boost", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(log, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            }
        }
    }
}
