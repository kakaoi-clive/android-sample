# Kakao-i ConnectLive iOS SDK

# 2.2
## 2.2.4
* onStat(stat) 콜백 파라미터 데이터 변경
  QualityStat 데이터 구조 변경


## 2.2.3
* webRTC 빌드 M107/5304 로 변경
* VideoRenderer 소스의 Bitmap 이미지 제공
    * VideoRenderer.onBitmapListener = { bitmap -> }
* Room.Events onDisconnected 의 강퇴 상태 추가
  DisconnectedReason.KICKED
* 일부 버그가 수정되었습니다.


## 2.2.2
* 로컬 스트림 녹화기능 추가로 publish 함수 수정
    * publish(contents, record)
* Room getStats(up/down)의 구조 변경
    * getStats(up/down) -> getLocalStats, geRemoteStats 구분 제공
* onStat(stat) 콜백 파라미터 데이터 변경
  QualityStat 데이터 구조 변경


## 2.2.1
* 미디어타입 볼륨컨트롤 제공
    * AudioHelper.acquireFocus(this, AudioManager.STREAM_MUSIC, AudioManager.MODE_NORMAL)
      (시청시에만 사용 권장)
* 유저 메시지 관련 메쏘드와 이벤트 콜백 변경
    * Room sendUserMessage(targets, message) -> sendUserMessage(targets, message, type)
    * Room.Events onUserMessage(senderId, message) -> onUserMessage(senderId, message, type)
* onStat 이벤트 콜백 추가
    * Room.Events onStat(stat)


## 2.2.0
Kakao-i ConnectLive 2.0의 Android SDK가 정식 오픈 버전입니다.


# 2.1
## 2.1.x
Kakao-i ConnectLive 2.0 서비스를 미리 살펴볼수 있는 프리뷰 버전으로 현재는 지원하지 않습니다.