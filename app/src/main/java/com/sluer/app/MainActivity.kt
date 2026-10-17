package com.sluer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sluer.app.core.ShizukuShell
import com.sluer.app.ui.SluerApp
import com.sluer.app.ui.theme.SluerTheme
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        ShizukuShell.onBinderReceived(this@MainActivity)
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        ShizukuShell.onBinderDead()
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            ShizukuShell.onPermissionResult(this@MainActivity, requestCode, grantResult)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
        ShizukuShell.refresh(this)

        setContent {
            SluerTheme {
                SluerApp()
            }
        }
    }

    override fun onDestroy() {
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        super.onDestroy()
    }
}
