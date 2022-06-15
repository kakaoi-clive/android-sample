package io.kakaoi.connectlive.demo

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

class ConferenceService : LifecycleService() {

    private val state = MutableStateFlow<State>(State.DISCONNECTED)

    override fun onCreate() {
        super.onCreate()
        _state.value = state

        lifecycleScope.launch {
            (1..5).forEach {
                state.value = State.CONNECTING(it / 5F)
                delay(1000)
            }

            state.value = State.CONNECTED
            delay(10000)

            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return Binder()
    }

    override fun onDestroy() {
        super.onDestroy()
        state.value = State.DISCONNECTED
    }

    inner class Binder : android.os.Binder() {

    }

    sealed class State {
        object DISCONNECTED : State()
        data class CONNECTING(val progress: Float) : State()
        object CONNECTED : State()
    }

    companion object {
        private val _state = MutableStateFlow(emptyFlow<State>())
        val state = _state.flatMapLatest { it }
            .stateIn(GlobalScope, SharingStarted.Eagerly, State.DISCONNECTED)

        private fun createIntent(context: Context) = Intent(context, ConferenceService::class.java)

        fun start(context: Context) {
            context.startService(createIntent(context))
        }

        fun stop(context: Context) {
            context.stopService(createIntent(context))
        }

        suspend fun bind(context: Context, onBind: (Binder) -> Unit): Nothing =
            suspendCancellableCoroutine { cont ->
                val conn = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                        onBind(service as Binder)
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        cont.cancel()
                    }
                }

                context.bindService(createIntent(context), conn, 0)

                cont.invokeOnCancellation { context.unbindService(conn) }
            }
    }
}