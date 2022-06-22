package io.kakaoi.connectlive.demo

import android.Manifest
import android.app.PendingIntent
import android.content.*
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import io.kakaoi.connectlive.*
import io.kakaoi.connectlive.demo.util.Preferences
import io.kakaoi.connectlive.demo.util.isGranted
import io.kakaoi.connectlive.demo.util.onReceive
import io.kakaoi.connectlive.media.*
import io.kakaoi.connectlive.utils.AudioHelper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ConferenceService : LifecycleService() {
    private lateinit var notificationManager: NotificationManagerCompat
    private val state = MutableStateFlow<State>(State.DISCONNECTED)
    private lateinit var room: Room
    private val localCamera = MutableStateFlow<LocalCamera?>(null)
    private val localScreen = MutableStateFlow<LocalScreen?>(null)
    private val localAudio = MutableStateFlow<LocalAudio?>(null)
    private val remoteVideos = MutableStateFlow<List<RemoteVideo>>(emptyList())

    override fun onCreate() {
        super.onCreate()

        notificationManager = NotificationManagerCompat.from(this)

        _state.value = state

        val prefs = Preferences.default(this)

        ConnectLive.signIn {
            serviceId = requireNotNull(prefs.getString(getString(R.string.key_service_id), null))

            val useExternal = prefs.getBoolean(getString(R.string.key_use_external), false)
            if (useExternal) {
                token = prefs.getString(getString(R.string.key_token), null)
            } else {
                serviceKey = prefs.getString(getString(R.string.key_service_key), null)
                serviceSecret = prefs.getString(getString(R.string.key_secret), null)
            }

            endpoint =
                requireNotNull(prefs.getString(getString(R.string.key_provisioning_url), null))

            errorHandler = ErrorHandler { code, message, isFatal ->
                val priority = if (isFatal) Log.ERROR else Log.WARN
                Log.println(priority, TAG, "onError(signIn): [$code] $message")
            }
        }

        val preferredAudioDevice =
            prefs.getString(getString(R.string.key_speaker_device), null).let { device ->
                AudioHelper.Device.values().find { it.name == device }
            }

        if (preferredAudioDevice == null) AudioHelper.resetPreferences()
        else AudioHelper.prefer(preferredAudioDevice)

        AudioHelper.acquireFocus(this)

        room = ConnectLive.createRoom(events = OnEvents())

        lifecycleScope.launch {
            onReceive(IntentFilter(ACTION_STOP)) { _, _ -> stopSelf() }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        requireNotNull(intent)

        if (intent.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        startForeground()

        val roomId = intent.getStringExtra(ARG_ROOM_ID)

        room.connect(requireNotNull(roomId))

        val cameraEnabled = intent.getBooleanExtra(ARG_CAMERA_ENABLED, false)
        val micEnabled = intent.getBooleanExtra(ARG_MIC_ENABLED, false)

        if (cameraEnabled) {
            val preferFrontCamera = intent.getBooleanExtra(ARG_PREFER_FRONT_CAMERA, true)
            localCamera.value = ConnectLive.createLocalCamera(preferFrontCamera)
                .also { room.publish(it) }
        }

        if (micEnabled) {
            localAudio.value = ConnectLive.createLocalAudio()
                .also { room.publish(it) }
        }

        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return Binder()
    }

    override fun onDestroy() {
        super.onDestroy()
        room.disconnect()
        state.value = State.DISCONNECTED

        localCamera.value?.dispose()
        AudioHelper.releaseFocus()

        ConnectLive.signOut()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannelCompat
            .Builder(NOTIFICATION_CHANNEL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(getString(R.string.notification_channel_name))
            .build()

        notificationManager.createNotificationChannel(channel)
    }

    private fun startForeground() {
        val contentIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        val stopIntent = Intent(ACTION_STOP).let {
            PendingIntent.getBroadcast(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
            .setContentTitle(getString(R.string.title_ongoing_meeting))
            .setContentIntent(contentIntent)
            .setAutoCancel(false)
            .addAction(0, getString(R.string.stop), stopIntent)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()

        startForeground(NOTIFICATION_FOREGROUND, notification)
    }

    private fun setCameraEnabled(enabled: Boolean) {
        if (enabled) {
            check(isGranted(Manifest.permission.CAMERA))
            localCamera.value = (localCamera.value ?: ConnectLive.createLocalCamera())
        }

        localCamera.value?.isEnabled = enabled
    }

    private fun shareScreen(data: Intent) {
        if (localScreen.value == null) {
            val screen = ConnectLive.createLocalScreen(data) {
                Log.d(TAG, "Screen share stopped")
            }
            room.publish(screen)

            localScreen.value = screen
        }
    }

    private fun stopShareScreen() {
        localScreen.value?.let { room.unpublish(it) }
        localScreen.value = null
    }

    private fun setAudioEnabled(enabled: Boolean) {
        if (enabled) {
            check(isGranted(Manifest.permission.RECORD_AUDIO))
            localAudio.value = (localAudio.value ?: ConnectLive.createLocalAudio())
        }

        localAudio.value?.isEnabled = enabled
    }


    private inner class OnEvents : EventsCallback {
        override fun onConnecting(progress: Float) {
            state.value = State.CONNECTING(progress)
        }

        override fun onConnected(participants: List<RemoteParticipant>) {
            state.value = State.CONNECTED
            remoteVideos.value = participants.flatMap { it.videos.values }
        }

        override fun onDisconnected() {
            stopSelf()
        }

        override fun onError(code: Int, message: String, isFatal: Boolean) {
            Log.println(if (isFatal) Log.ERROR else Log.WARN, TAG, "onError: [$code] $message")
        }

        override fun onLocalVideoPublished(video: LocalVideo) {
            video.start()
        }

        override fun onLocalVideoUnpublished(video: LocalVideo) {
            video.dispose()
        }

        override fun onRemoteVideoPublished(participant: RemoteParticipant, video: RemoteVideo) {
            remoteVideos.value += video
        }

        override fun onRemoteVideoUnpublished(participant: RemoteParticipant, video: RemoteVideo) {
            remoteVideos.value -= video
        }
    }

    inner class Binder : android.os.Binder() {
        private val impl = this@ConferenceService
        val localParticipant get() = impl.room.localParticipant
        val localCamera get() = impl.localCamera.asStateFlow()
        val localScreen get() = impl.localScreen.asStateFlow()
        val localAudio get() = impl.localAudio.asStateFlow()
        val remoteVideos get() = impl.remoteVideos.asStateFlow()
        val remoteParticipants get() = impl.room.remoteParticipants.values

        val isCameraEnabled get() = localCamera.value?.isEnabled == true
        val isAudioEnabled get() = localAudio.value?.isEnabled == true
        val isAudioAlwaysOn get() = localAudio.value?.isAlwaysOn == true

        fun setCameraEnabled(enabled: Boolean) = impl.setCameraEnabled(enabled)
        fun shareScreen(data: Intent) = impl.shareScreen(data)
        fun stopShareScreen() = impl.stopShareScreen()

        fun setAudioEnabled(enabled: Boolean) = impl.setAudioEnabled(enabled)
        fun setAudioAlwaysOn(alwaysOn: Boolean): Boolean = localAudio.value?.apply {
            isAlwaysOn = alwaysOn
        } != null

        suspend fun switchCamera(): Boolean = suspendCoroutine { cont ->
            val camera = localCamera.value
            if (camera == null) {
                cont.resume(false)
            } else {
                camera.switchCamera().thenAccept {
                    cont.resume(it)
                }
            }
        }
    }

    sealed class State {
        object DISCONNECTED : State()
        data class CONNECTING(val progress: Float) : State()
        object CONNECTED : State()
    }

    companion object {
        private const val TAG = "ConferenceService"

        private const val ACTION_STOP = "io.kakaoi.connectlive.demo.STOP"

        private const val NOTIFICATION_CHANNEL = "conference-notification"
        private const val NOTIFICATION_FOREGROUND = 1

        private const val ARG_ROOM_ID = "roomId"
        private const val ARG_CAMERA_ENABLED = "cameraEnabled"
        private const val ARG_MIC_ENABLED = "micEnabled"
        private const val ARG_PREFER_FRONT_CAMERA = "preferFrontCamera"

        private val _state = MutableStateFlow(emptyFlow<State>())
        val state = _state.flatMapLatest { it }
            .stateIn(GlobalScope, SharingStarted.Eagerly, State.DISCONNECTED)

        private fun createIntent(context: Context) = Intent(context, ConferenceService::class.java)

        fun start(
            context: Context,
            roomId: String,
            cameraEnabled: Boolean,
            micEnabled: Boolean,
            preferFrontCamera: Boolean
        ) {
            context.startService(
                createIntent(context)
                    .putExtra(ARG_ROOM_ID, roomId)
                    .putExtra(ARG_CAMERA_ENABLED, cameraEnabled)
                    .putExtra(ARG_MIC_ENABLED, micEnabled)
                    .putExtra(ARG_PREFER_FRONT_CAMERA, preferFrontCamera)
            )
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