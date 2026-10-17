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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

private val PRESETS = listOf(
    "id",
    "pm list packages -3",
    "dumpsys battery | head -30",
    "getprop ro.product.model",
)

@Composable
fun ConsoleScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("id") }
    var output by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }

    fun run() {
        if (running) return
        val cmd = input.trim()
        if (cmd.isEmpty()) return
        running = true
        scope.launch {
            val result = ShizukuShell.exec(cmd)
            val block = "${'$'} $cmd\n$result"
            output = if (output.isBlank()) block else output + "\n\n" + block
            running = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Console", style = MaterialTheme.typography.titleLarge)
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PRESETS.forEach { preset ->
                OutlinedButton(onClick = { input = preset }) {
                    Text(preset, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Command") },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { run() }, enabled = !running) {
                Text(if (running) "Running..." else "Run")
            }
            OutlinedButton(onClick = { output = "" }, enabled = output.isNotEmpty()) {
                Text("Clear")
            }
        }

        Spacer(Modifier.height(2.dp))

        Text(
            text = output.ifBlank {
                "Output appears here. Commands run with the Shizuku identity (adb shell or root)."
            },
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        )
    }
}
