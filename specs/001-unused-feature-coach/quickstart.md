# Quickstart: 안 써본 기능 검증

## 준비

- JDK 17, Android SDK(platforms 34·35, build-tools 35), Pleos Connect 에뮬레이터 v2.1.0 이미지(`system-images;android-34;pleos_car_emulator;arm64-v8a`)
- 환경 변수: `JAVA_HOME`(JDK 17), `ANDROID_HOME`

## 1. 판단 로직 테스트

```bash
./gradlew :app:testDebugUnitTest
```
기대: `CoachTest` 전부 통과.

## 2. 빌드

```bash
./gradlew :app:assembleDebug
```
기대: `app/build/outputs/apk/debug/app-debug.apk`

## 3. 에뮬레이터 시연 (P1 → P2 → P3)

```bash
emulator -avd pleos -no-snapshot-load &   # Cold boot (Quick boot면 VHAL 사용 불가)
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n kr.skku.unusedfeature/.MainActivity
adb forward tcp:8088 tcp:8088            # 브라우저 127.0.0.1:8088 = Virtual Console
```

1. 앱 화면: 기능 6개가 "아직", 카드 자리에 "첫 운전이 끝나면…"이 보입니다.
2. Virtual Console에서 기어 D → 풍량을 세 번 바꿈 → 1분 기다림 → 기어 P.
3. 기대: 3초 안에 "바람과 온도를 여러 번 손으로 바꾸셨어요…" 음성, 알림, 카드. 목록에서 공조 AUTO가 "안내함 1회".
4. 기어 D → AUTO 켬 → 1분 → P.
5. 기대: 카드 맨 위 "공조 AUTO, 써 보셨네요", 목록에서 "익힘".

## 4. 주행 중 침묵 확인

기어 D에서 풍량을 여러 번 바꿔도 P 전에는 음성·알림이 없어야 합니다.
