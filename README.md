[API(latest)](https://kakaoi-clive.github.io/android-sample/api/latest/)

# 1. 시작

## 1.1. 서비스 가입
_TODO 웹 콘솔 가입, 서비스 키 생성 등 안내_

## 1.2. 환경
Android OS 최소지원 버전은 7.0(API 24, Nougat)이다.
Kotlin 1.6 이상 (또는 Java 1.8 이상)을 요구하며
이 문서는 Android Studio (Bumblebee 2021.1.1)의 개발환경을 전제해서 안내하고 있다. 

## 1.3. 빌드 설정
Android 모듈의 gradle 빌드 스크립트에 아래 의존성을 추가
```gradle
android {
    defaultConfig {
        minSdk 24   // SDK는 24까지 지원할 수 있도록 개발되었다.
    }
}
 
repositories {
    maven { url "https://icl.jfrog.io/artifactory/kakaoenterprise" }
}
 
dependencies {
    implementation 'com.kakaoenterprise:icl2:2.+'
}
```

## 1.4. 권한
SDK는 기본적으로 런타임 권한 요청이 필요한 2가지(`RECORD_AUDIO`, `CAMERA`)를 포함해서 몇가지 권한이 `AndroidManifest.xml`에 등록되어 있다.

| 권한 | 보호 | 등급 | 설명 |
| -- | -- | -- | -- |
| INTERNET | normal |  | 
| ACCESS_NETWORK_STATE | normal |  | 
| FOREGROUND_SERVICE | normal |  | 
| MODIFY_AUDIO_SETTINGS | normal |  | 
| RECORD_AUDIO | dangerous | 로컬 오디오 전송시 필요 | 
| CAMERA | dangerous | 로컬 카메라 전송시 필요 | 

## 1.5. SDK 초기화
Application 생성시 SDK를 초기화한다.
```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ConnectLive.init(this)
    }
}
```

# 2. 기본 흐름
## 2.1. 인증
화상회의 지속을 위해 토큰 생성 및 주기적 갱신을 시작한다.
```kotlin

ConnectLive.signIn {
    // endpoint = "https://icl2.provisioning.host/api/v1/rpc"
    serviceId = "************"
    serviceKey = "****************"
    serviceSecret = "****************"
 
    errorHandler = ErrorHandler { code, message, isFatal ->
        // code, message : 별도로 정의된 에러 코드와 메세지
        // isFatal : 정보성 오류와 작업 중단을 위한 오류를 구분
    }
}
 
// 인증 해제
ConnectLive.signOut()
```

## 2.2. Room 객체 생성
```kotlin
val config = Room.Config(
    videoReceiverInitialCount = 10,    // 초기 영상 리시버 개수 (default=10)
    videoReceiverGrowthRate = 10,      // 영상 리시버가 부족한 경우 증가 단위 (default=10)
    videoReceiverMaximumCount = 50,    // 최대 영상 리시버 개수 (default=50)
)
 
val events = object : Room.Events.Adapter() {
    // 필요한 콜백 구현
}
 
val room = ConnectLive.createRoom(config, events)
```

## 2.3. 접속하기
```kotlin
room.connect(roomId)
 
// 접속 중단
room.disconnect()
```

## 2.4. 참여자 처리
`Room`이 접속이 완료되면 `Room.localParticpant`로 현재 "나"에 해당하는 `LocalParticipant`에 접근이 가능해진다.
그리고 `Room.Events`의 콜백 또는 `Room.remoteParticipants`를 통해 참여자에 해당하는 `RemoteParticipant`를 참조할 수 있다.

```kotlin
interface Participant {
    val id: String
 
    val videos: Map<Int, VideoContent>
    val audios: Map<Int, AudioContent>
 
    val hasVideo: Boolean
    val hasAudio: Boolean
}
```
`Participant`에서는 해당 참여자의 ID와 공유중인 미디어에 대한 정보를 제공한다.

## 2.5. UI 연동
```xml
...
    <io.kakaoi.connectlive.view.VideoRenderer
        android:id="@+id/renderer"
        android:layout_width="160dp"
        android:layout_height="120dp" />
...
```
`VideoRenderer.bind()` 메소드는 내부적으로 초기화, 비디오 스트림의 구독 등의 처리를 은닉할수 있는 방법을 제공한다.

```kotlin
val binding: ItemRendererBinding
val content: VideoContent
 
binding.renderer.bind(content)
```

## 2.6. 로컬 미디어 전송
```kotlin
val localMedia = ConnectLive.createLocalMedia(preferFrontCamera)
 
room.publish(localMedia)
 
// 전송 중단
room.unpublish(localMedia)
```
전송이 개시되면 `Room.Events.onLocalVideoPublished`/`onLocalAudioPublished`에 개별 미디어가 전달된다.

# 3. 기타
## 3.1. Room.Events callbacks
```kotlin
interface Events {
    /**
     * 접속 중
     * [progress] 접속 진행율
     */
    fun onConnecting(progress: Float)
 
    /**
     * 접속 완료
     * [participants] 기존 참여자의 목록
     */
    fun onConnected(participants: List<RemoteParticipant>)
 
    /**
     * 접속 종료
     */
    fun onDisconnected()
 
    /**
     * 에러 전달
     * [isFatal] 회의를 지속할 수 없는 오류 발생시 true
     */
    fun onError(code: Int, message: String, isFatal: Boolean)
 
    /**
     * [participant] 신규 참여자
     */
    fun onParticipantEntered(participant: RemoteParticipant)
 
    /**
     * [participant] 이탈한 참여자
     */
    fun onParticipantLeft(participant: RemoteParticipant)
 
    /**
     * [video] 공유된 로컬 비디오(카메라, 화면)
     */
    fun onLocalVideoPublished(video: LocalVideo)
    fun onLocalVideoUnpublished(video: LocalVideo)
 
    /**
     * [audio] 공유된 로컬 오디오
     */
    fun onLocalAudioPublished(audio: LocalAudio)
    fun onLocalAudioUnpublished(audio: LocalAudio)
 
    /**
     * [participant] 비디오를 공유한 참여자
     * [video] 공유된 원격 비디오
     */
    fun onRemoteVideoPublished(participant: RemoteParticipant, video: RemoteVideo)
    fun onRemoteVideoUnpublished(participant: RemoteParticipant, video: RemoteVideo)
    fun onRemoteVideoStateChanged(participant: RemoteParticipant, video: RemoteVideo)
 
    /**
     * [participant] 비디오를 공유한 참여자
     * [audio] 공유된 원격 비디오
     */
    fun onRemoteAudioPublished(participant: RemoteParticipant, audio: RemoteAudio)
    fun onRemoteAudioUnpublished(participant: RemoteParticipant, audio: RemoteAudio)
    fun onRemoteAudioStateChanged(participant: RemoteParticipant, audio: RemoteAudio)
 
    /**
     * 원격 오디오가 수신되기 시작
     * 오디오의 수신은 서버에서 자동으로 처리됨
     */
    fun onRemoteAudioSubscribed(participant: RemoteParticipant, audio: RemoteAudio)
    fun onRemoteAudioUnsubscribed(participant: RemoteParticipant, audio: RemoteAudio)
}
```

## 3.2. 소리 끄기
회의상에서 재생중인 원격 오디오 전체의 소리를 끄는 방법으로 `Room.isMuted` 속성을 제공한다.
개별 오디오 스트림의 mute상태에 영향없이 회의 단위의 제어가 가능하다.

## 3.3. 오디오 장치 운용
(필수 사항은 아니지만) 회의가 진행되는 동안 오디오 장치 운용 방법을 제공하여 사용자에게 더 나은 경험을 제공할 수 있다.
```kotlin
// 회의가 시작하는 시점에 요청
// stream type(default=VOICE_CALL), audio mode(default=IN_COMMUNICATION)의 파라미터를 추가로 지원하며
// 통화 중 상태를 UI에서 처리하기 위한 콜백을 추가할 수 있다.
AudioHelper.acquireFocus(context)
 
// 회의가 종료되면 해체한다.
AudioHelper.releaseFocus()
```
오디오 장치를 선택하거나 배제할 수 있도록 `prefer()`, `avoid()`, `resetPreferences()` 메소드를 제공하는데,
장치들에 대한 선호도 개념이기 때문에 유효한 장치의 상황에 따라 동작을 보장하진 않는다.

## 3.4. 카메라 미리보기
```kotlin
// 카메라 접근 권한이 취득된 상태에서 LocalCamera 생성
val camera = ConnectLive.createLocalCamera()
 
val switching: CompletableFuture<Boolean> = camera.switchCamera() // front/rear 전환
 
camera.isFrontFacing    // 현재 카메라의 방향을 리턴
 
camera.isEnabled = true
camera.start()  // 외부 앱에서 소유권을 가져간 경우 복귀시 재호출
 
binding.renderer.bind(camera)    // VideoRenderer에 카메라 영상 제공
 
camera.isEnabled = false
 
camera.dispose()    // 카메라 소유권 반환
```

## 3.5. 화면 공유
화면 공유를 위한 세션을 개시하기 위해서 `MediaProjectionManager.createScreenCaptureIntent()`이 반환하는 Intent가 필요한데,
SDK에서 구현된 `VideoCapturerFactory.CreateScreenCapture`를 활용해서 Activity Result API로 `LocalScreen` 객체를 생성할 수 있다.

[Android 10 부터는 화면 공유를 전경 서비스에서 운용해야 함](https://developer.android.google.cn/reference/kotlin/android/media/projection/MediaProjectionManager#getmediaprojection)

```kotlin
private val publishLocalScreen =
    registerForActivityResult(VideoCapturerFactory.CreateScreenCapture) { data ->
        if (data != null) {
            val localScreen = ConnectLive.createLocalScreen(data)
			room.publish(localScreen)
        }
    }
```

## 3.6. (Experimental) RTCStats 
주기적으로 리포트를 제공하는 `Room.subscribeStats()`과 1회성 제공을 위한 `Room.getStats()`이 있다.

## 3.7. (Experimental) 음량 정보
`Room.getAudioLevels()`에서 `RTCStats`에 기반해서 참여자별 오디오의 음량을 제공한다.
