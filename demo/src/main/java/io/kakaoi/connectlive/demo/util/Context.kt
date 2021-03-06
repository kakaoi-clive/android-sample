package io.kakaoi.connectlive.demo.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.awaitCancellation

fun Context.isGranted(permission: String) =
    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

fun Context.createAppSettingsIntent() = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
    data = Uri.fromParts("package", packageName, null)
    flags = Intent.FLAG_ACTIVITY_NEW_TASK
    addCategory(Intent.CATEGORY_DEFAULT)
}

suspend fun Context.onReceive(
    filter: IntentFilter,
    block: (context: Context, intent: Intent) -> Unit
): Nothing {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            block(context, intent)
        }
    }
    registerReceiver(receiver, filter)

    try {
        awaitCancellation()
    } finally {
        unregisterReceiver(receiver)
    }
}
