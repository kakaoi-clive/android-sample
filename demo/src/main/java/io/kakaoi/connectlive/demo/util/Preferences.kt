package io.kakaoi.connectlive.demo.util

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import io.kakaoi.connectlive.demo.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

object Preferences {
    private fun default(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)

    private fun <V> SharedPreferences.changes(
        key: String,
        getValue: SharedPreferences.(String) -> V
    ) = callbackFlow<String> {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            trySend(key)
        }

        registerOnSharedPreferenceChangeListener(listener)

        awaitClose {
            unregisterOnSharedPreferenceChangeListener(listener)
        }
    }
        .filter { it == key }
        .map { getValue(key) }
        .onStart { emit(getValue(key)) }

    fun cameraEnabled(context: Context) =
        default(context).changes(context.getString(R.string.key_camera_enabled)) {
            getBoolean(it, false)
        }
}