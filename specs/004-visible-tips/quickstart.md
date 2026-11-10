# Quickstart: 안 써본 기능 4차 검증

1~3차 준비(에뮬레이터 Cold boot, `adb forward tcp:8088 tcp:8088`, Virtual Console 페이지 닫기)를 그대로 한다. 같은 에뮬레이터에 깔린 다른 두 앱(같은편 `kr.skku.sameside`, DRIVE DNA `kr.skku.drivedna`)은 `adb shell am force-stop --user 10 <패키지>`로 멈춰 둔다(P에서 음성이 겹치지 않게).

```sh
./gradlew :app:testDebugUnitTest      # CoachTest 45 + SeedTest
./gradlew :app:assembleDebug && adb install -r --user 10 app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:processReleaseMainManifest :app:processDebugMainManifest   # SeedReceiver: 출시 없음, 디버그 있음
```

| 장면 | 기대 |
|---|---|
| `reset` → `climate` | 안내 화면 hero 아래 "방금 들은 음성"과 문장 "이번 운전에서 바람과 온도를 여러 번 손으로 바꾸셨어요. 다음엔 공조 AUTO를 켜 보세요. 알아서 맞춰 줘요."(SC-001). 막대가 6초 움직이고 멈춘다. 차 지도 공조 패널에 넓은 링(10초) |
| `learn` | 확인 화면에 자막 없음 |
| `seed` | 첫 화면은 앰비언트 라이트 안내(자막 제목 "안내 음성", 막대 정지, 강조 없음, "방금 끝난 운전 · 16분"). 진행 원 2개 익힘. 추천 기록 6개(날짜 묶음 셋), 카드마다 미니 지도. 공조 AUTO·시트 열선 카드는 초록 점과 "써 봄 ✓"(SC-002) |
| 기록 하나 열기 | "그때 들은 음성"과 그 기능 문장, 막대 정지 |
| 움직임 줄이기 | `adb shell settings put global animator_duration_scale 0` → `reset` → `climate` → P 뒤 2초·15초 캡처: 2초에 굵은 테두리와 굵은 이름표, 15초에 없음, 막대는 정지 모습. 끝나면 `... animator_duration_scale 1` |
| 1~3차 장면 | 안내 기능·문장 그대로(SC-004) |

- 캡처: `climate` 뒤 2초(링 있음)와 15초(링 없음)(SC-003)
- 막대의 움직임과 정지는 캡처 한 장으로 가릴 수 없다. `adb shell screenrecord`로 P 뒤 0~10초를 찍어 6초 전 움직임과 그 뒤 정지를 본다.
- `seed`는 `climate` 확인 뒤에 한다. 샘플에서 공조 AUTO를 익힌 상태라 그 뒤 `climate`는 공조 AUTO를 권하지 않는다.
