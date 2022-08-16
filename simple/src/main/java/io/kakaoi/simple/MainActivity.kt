package io.kakaoi.simple

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.MutableLiveData
import com.google.android.material.button.MaterialButton
import io.kakaoi.connectlive.ConnectLive
import io.kakaoi.connectlive.EventsCallback
import io.kakaoi.connectlive.RemoteParticipant
import io.kakaoi.connectlive.Room
import io.kakaoi.connectlive.entity.DisconnectedReason
import io.kakaoi.connectlive.media.LocalCamera
import io.kakaoi.connectlive.media.LocalContents
import io.kakaoi.connectlive.media.LocalVideo
import io.kakaoi.connectlive.media.RemoteVideo
import io.kakaoi.connectlive.utils.AudioHelper
import io.kakaoi.simple.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var room: Room
    private var camera: LocalCamera? = null
    private var localVideo: LocalVideo? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ConnectLive.init(this)
        ConnectLive.signIn {
            serviceId = "ICLEXMPLPUBL"
            serviceKey = "ICLEXMPLPUBL0KEY"
            secret = "YOUR0SRVC0SECRET"
        }
        AudioHelper.acquireFocus(this)
        room = ConnectLive.createRoom(events = OnEventListener())

        requestPermissionForActivateMedia.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )

        binding.btnAction.setOnClickListener {
            if ((it as MaterialButton).text == "join") {
                camera?.stop()
                camera?.dispose()

                room.connect("icl2")
                val localMedia = ConnectLive.createLocalMedia().apply {
                    video?.isEnabled = true
                    audio?.isEnabled = true
                }
                room.publish(localMedia)


            } else {
                room.disconnect()
            }
        }
    }

    private inner class OnEventListener : EventsCallback {
        override fun onConnecting(progress: Float) {
            // 접속중...
        }

        override fun onConnected(participants: List<RemoteParticipant>) {
            // 접속 완료

            // 예제에서는 처음 들어는 비디오만 bind 하여 확인 합니다.
            if (participants.isNotEmpty()) {
                val remoteVideo = participants.flatMap { it.videos.values }.first()
                binding.remote.bind(remoteVideo)
            }

            binding.btnAction.text = "close"
        }

        override fun onDisconnected(reason: DisconnectedReason) {
            finish()
        }

        override fun onError(code: Int, message: String, isFatal: Boolean) {
            val errorMessage = "code: $code, message: $message, isFatal: $isFatal"
            Log.d("onError", errorMessage)
            if (isFatal)
                finish()
        }

        override fun onLocalVideoPublished(video: LocalVideo) {
            localVideo = video
            binding.local.bind(localVideo)
        }

        override fun onRemoteVideoPublished(participant: RemoteParticipant, video: RemoteVideo) {
            // 내가 참여후 published 된 다른사람의 video를 VideoRenderer 에 bind
            binding.remote.bind(video)
        }

        override fun onRemoteVideoUnpublished(participant: RemoteParticipant, video: RemoteVideo) {
            binding.remote.unbind()
        }
    }

    private val requestPermissionForActivateMedia =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            result[Manifest.permission.CAMERA]?.let { cameraGranted ->
                if (cameraGranted && camera == null) {
                    camera = ConnectLive.createLocalCamera().apply { start() }
                    binding.local.bind(camera)
                }
            }
        }

    override fun onResume() {
        super.onResume()
        camera?.start()
        localVideo?.start()
    }

    override fun onPause() {
        super.onPause()
        camera?.stop()
        localVideo?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        camera?.dispose()
        localVideo?.dispose()
        AudioHelper.releaseFocus()
        ConnectLive.signOut()
    }
}