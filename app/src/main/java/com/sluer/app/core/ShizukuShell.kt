package com.sluer.app.core

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import com.sluer.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

enum class ShellStatus {
    UNKNOWN,
    SHIZUKU_NOT_RUNNING,
    PERMISSION_REQUIRED,
    PERMISSION_DENIED,
    CONNECTING,
    READY,
    ERROR,
}

/**
 * Single app-wide entry point to Shizuku: permission handling, binding of the
 * runner user service, and command execution.
 */
object ShizukuShell {

    const val PERMISSION_REQUEST_CODE = 4001

    private const val LOG_TAG = "SluerShell"

    private const val SERVICE_TAG = "sluer_runner"

    /**
     * Shizuku UserServiceArgs requires a process name suffix (it throws
     * "process name suffix must not be null" otherwise). The user service
     * runs in the process "<applicationId>:sluer".
     */
    private const val PROCESS_NAME_SUFFIX = "sluer"

    private val _status = MutableStateFlow(ShellStatus.UNKNOWN)
    val status: StateFlow<ShellStatus> = _status.asStateFlow()

    /** Detail of the last failure, shown on the dashboard when status is ERROR. */
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private var service: IRunnerService? = null
    private var bound = false

    fun binderAlive(): Boolean = try {
        Shizuku.pingBinder()
    } catch (t: Throwable) {
        false
    }

    fun hasPermission(): Boolean = try {
        Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (t: Throwable) {
        false
    }

    fun shizukuUid(): Int = try {
        Shizuku.getUid()
    } catch (t: Throwable) {
        -1
    }

    fun shizukuVersion(): Int = try {
        Shizuku.getVersion()
    } catch (t: Throwable) {
        -1
    }

    fun requestPermission() {
        try {
            Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
        } catch (t: Throwable) {
            _lastError.value = "${t.javaClass.simpleName}: ${t.message}"
            _status.value = ShellStatus.ERROR
            Log.e(LOG_TAG, "requestPermission failed", t)
        }
    }

    /** Called from the sticky binder-received listener in MainActivity. */
    fun onBinderReceived(context: Context) {
        if (!hasPermission()) {
            _status.value = ShellStatus.PERMISSION_REQUIRED
            return
        }
        connect(context)
    }

    fun onBinderDead() {
        service = null
        bound = false
        _status.value = ShellStatus.SHIZUKU_NOT_RUNNING
    }

    fun onPermissionResult(context: Context, requestCode: Int, grantResult: Int) {
        if (requestCode != PERMISSION_REQUEST_CODE) return
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            connect(context)
        } else {
            _status.value = ShellStatus.PERMISSION_DENIED
        }
    }

    /** Re-evaluates the current state; safe to call from UI at any time. */
    fun refresh(context: Context) {
        when {
            !binderAlive() -> _status.value = ShellStatus.SHIZUKU_NOT_RUNNING
            !hasPermission() -> _status.value = ShellStatus.PERMISSION_REQUIRED
            service != null -> _status.value = ShellStatus.READY
            else -> connect(context)
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = binder?.let { IRunnerService.Stub.asInterface(it) }
            bound = true
            if (service != null) {
                _lastError.value = null
                _status.value = ShellStatus.READY
                Log.i(LOG_TAG, "runner user service connected")
            } else {
                _lastError.value = "empty binder received"
                _status.value = ShellStatus.ERROR
                Log.e(LOG_TAG, "runner user service returned an empty binder")
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            bound = false
            _status.value = ShellStatus.CONNECTING
        }
    }

    fun connect(context: Context) {
        if (bound) {
            _status.value = ShellStatus.READY
            return
        }
        _status.value = ShellStatus.CONNECTING
        try {
            val args = Shizuku.UserServiceArgs(
                ComponentName(context.packageName, RunnerService::class.java.name)
            )
                .daemon(false)
                .processNameSuffix(PROCESS_NAME_SUFFIX)
                .debuggable(BuildConfig.DEBUG)
                .tag(SERVICE_TAG)
                .version(BuildConfig.VERSION_CODE)
            Shizuku.bindUserService(args, connection)
        } catch (t: Throwable) {
            _lastError.value = "${t.javaClass.simpleName}: ${t.message}"
            _status.value = ShellStatus.ERROR
            Log.e(LOG_TAG, "bindUserService failed", t)
        }
    }

    /** Runs [command] through the user service. Never throws; failures are text. */
    suspend fun exec(command: String): String = withContext(Dispatchers.IO) {
        val svc = service
            ?: return@withContext "! Not connected to the Shizuku user service yet."
        try {
            svc.exec(command)
        } catch (t: RemoteException) {
            "! RemoteException: ${t.message}"
        } catch (t: Throwable) {
            "! ${t.javaClass.simpleName}: ${t.message}"
        }
    }
}
