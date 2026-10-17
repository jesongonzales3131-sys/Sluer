package com.sluer.app.core

import android.os.Process
import java.util.concurrent.TimeUnit

/**
 * Runs inside a process started by the Shizuku server with shell/root identity.
 * The Shizuku server instantiates this class by reflection and talks to it over
 * [IRunnerService]. Keep this class free of Android-app-only assumptions: it is
 * not a regular app process.
 */
class RunnerService : IRunnerService.Stub() {

    /** Reserved method: Shizuku calls this (transaction 16777114) to tear the service down. */
    override fun destroy() {
        System.exit(0)
    }

    override fun exec(command: String): String {
        val cmd = command.trim()
        if (cmd.isEmpty()) return "! Empty command."

        return try {
            val started = System.currentTimeMillis()
            val process = ProcessBuilder("/system/bin/sh", "-c", cmd)
                .redirectErrorStream(true)
                .start()

            // No stdin for the command; EOF instead of a hang.
            process.outputStream.close()

            val output = process.inputStream.bufferedReader().use { it.readText() }
            val finished = process.waitFor(60, TimeUnit.SECONDS)
            if (!finished) {
                process.destroyForcibly()
                return "! Timed out after 60s.\n$output"
            }

            val elapsed = System.currentTimeMillis() - started
            buildString {
                append(output)
                if (output.isNotEmpty() && !output.endsWith("\n")) append('\n')
                append("[exit=").append(process.exitValue())
                append(" | ").append(elapsed).append(" ms]")
            }
        } catch (t: Throwable) {
            "! ${t.javaClass.simpleName}: ${t.message}"
        }
    }

    override fun getUid(): Int = Process.myUid()
}
