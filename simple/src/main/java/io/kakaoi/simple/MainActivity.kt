package io.kakaoi.simple

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import io.kakaoi.connectlive.ConnectLive
import io.kakaoi.connectlive.EventsCallback
import io.kakaoi.connectlive.RemoteParticipant
import io.kakaoi.connectlive.Room
import io.kakaoi.connectlive.entity.DisconnectedReason
import io.kakaoi.connectlive.media.LocalCamera
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
            serviceSecret = "ICLEXMPLPUBL0KEY:YOUR0SRVC0SECRET"
        }
        requestPermissionForActivateMedia.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
        )

        AudioHelper.acquireFocus(context = this)
        room = ConnectLive.createRoom(events = OnEventListener())

        binding.btnAction.setOnClickListener {
            if ((it as Button).text == "join") {
                // 로컬 프리뷰카메라 정지및 해제
                camera?.stop()
                camera?.dispose()

                // 해당 roomId로 접속
                room.connect(roomId = "lucas-test") // roomId는 영문 대소문자, 숫자,- 으로 구성이 가능하며, 길이 제한은 32자입니다.
                val localMedia = ConnectLive.createLocalMedia().apply {
                    video?.isEnabled = true
                    audio?.isEnabled = true
                }

                // 내 localMedia publish
                room.publish(localMedia)
            } else {
                // 연결해제
                room.disconnect()
            }
        }
    }

    private inner class OnEventListener : EventsCallback {
        override fun onConnecting(progress: Float) {
            // 접속중...
        }

        override fun onConnected(participants: List<RemoteParticipant>) {
            // 해당 Room에 접속 완료

            // 기존에 참여중이던 사람들의 목록을 받습니다.
            // 예제에서는 처음 들어는 비디오만 bind 하여 확인 합니다.
            if (participants.isNotEmpty()) {
                val remoteVideo = participants.flatMap { it.videos.values }.first()
                binding.remote.bind(remoteVideo)
            }

            // 버튼의 문구를 바꿔줍니다.
            binding.btnAction.text = "close"
        }

        override fun onDisconnected(reason: DisconnectedReason) {
            finish()
        }

        override fun onError(code: Int, message: String, isFatal: Boolean) {
            val errorMessage = "code: $code, message: $message, isFatal: $isFatal"
            Log.e("onError", errorMessage)
            if (isFatal)
                finish()
        }

        override fun onLocalVideoPublished(video: LocalVideo) {
            // 나의 video가 publish 되었을때 불리며, 내 video를 local에 렌더링합니다.
            localVideo = video
            binding.local.bind(localVideo)
        }

        override fun onRemoteVideoPublished(participant: RemoteParticipant, video: RemoteVideo) {
            // 내가 참여후 published 된 다른사람의 video를 VideoRenderer 에 bind
            binding.remote.bind(video)
        }

        override fun onRemoteVideoUnpublished(participant: RemoteParticipant, video: RemoteVideo) {
            // 다른사람이 퇴장 혹은 video를 unPublish 했을때 unbind 시킨다
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