# Quickstart: 안 써본 기능 2차 검증

## 준비

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=$HOME/Library/Android/sdk
./gradlew :app:testDebugUnitTest :app:assembleDebug
adb install -r -g app/build/outputs/apk/debug/app-debug.apk
adb shell am start --user 10 -n kr.skku.unusedfeature/.MainActivity
adb forward tcp:8088 tcp:8088
```

- 디버그 빌드는 앱 시계가 10배 빠르다(research R5). 1분 조건은 실제 6초, 5분은 30초, 10분은 60초.
- 기록 초기화: `adb shell am force-stop --user 10 kr.skku.unusedfeature && adb shell run-as kr.skku.unusedfeature --user 10 rm shared_prefs/progress.xml && adb shell sync`
- 음성 확인: `adb logcat -d | grep "doSpeak(kr.skku"`
- 브라우저에 Virtual Console 페이지(`127.0.0.1:8088`)를 열어 둔 채 스크립트를 돌리지 않는다. 스크립트가 연결할 때 페이지의 토글 상태(예: 앞·뒤 제상 켬)가 다시 들어가고, 앱은 그 기능을 바로 "익힘"으로 기록한다. 페이지가 열려 있는지는 스크립트를 멈춘 상태에서 `curl -s 127.0.0.1:8088/browser/session`의 `clients`가 0인지로 본다.

## 시나리오 (`scripts/demo.sh <이름>`)

| 이름 | 조작 | 기대 결과 |
|---|---|---|
| `reset` | 차 상태(공조·열선·와이퍼·조명·착좌)를 기본값으로 돌리고 앱 기록을 지움 | 첫 실행 화면 |
| `climate` | D → 풍량 2→4→5, 온도 24→23 → 7초 → P | 공조 AUTO 안내. 근거 3개(10초·30초·50초), AUTO 버튼 강조, 지도 공조 패널 점 파랑 깜빡임 |
| `learn` | D → AUTO 켬 → P | 확인 화면 "공조 AUTO, 써 보셨네요!", 도감 1칸 채움, 지도 공조 패널 초록 반짝임 |
| `wiper` | D → 와이퍼 OFF→LO→HI→LO→OFF(4초 간격) → P | 오토 와이퍼 안내. 근거 4개(OFF→LO, LO→HI, HI→LO, LO→OFF) |
| `rain` | D → 와이퍼 LO, 내기 순환 켬 → 32초 → P | 앞유리 김서림 제거 안내(다시 하면 뒷유리 열선). 근거 = 비 구간 막대 |
| `highbeam` | 전조등 켬 → D → 하이빔 켬·끔 ×2 → P | 하이빔 보조를 지원하는 차면 하이빔 보조 안내. 이 에뮬레이터는 하이빔 스위치 선택지에 AUTO가 없어(지원 안 함) 음성 없이 "권할 것 없음" |
| `night` | 전조등 켬 → D → 62초 → P | 앰비언트 라이트 안내 |
| `highway` | D → 지금 주행거리에서 3초마다 +1km × 26번 → P | 크루즈 컨트롤 안내. 근거 = "시속 70km 이상" 막대. 콘솔은 줄어드는 주행거리 쓰기를 무시하므로 늘 지금 값에서 시작 |
| `heat` | D → 설정 온도 27° → 7초 → P | 시트 열선 안내 |
| `multi` | D → 조수석 착좌, 온도 21, 시트 열선 2단, 전조등 켬·끔 → P (여러 번) | 매번 하나씩: 운전대 열선 → 동승석 개별 온도 → 동승석 시트 열선·통풍(오토 라이트를 지원하는 차면 맨 앞에 오토 라이트) |

## 화면 확인

1. 설치 직후 앱: 첫 실행 화면(진행 원 0/n, 회색 지도, 세 단계).
2. `climate` 실행 중(D): Pleos가 "주행 중에는 사용할 수 없습니다"로 앱을 가림(앱은 그 아래 주행 중 화면에 고정). N도 같고, R은 후방 카메라가 덮는다. 가려진 글자는 기어 N에서 `adb shell uiautomator dump /sdcard/ui.xml && adb shell cat /sdcard/ui.xml | grep -o 'text="[^"]*"'`로 본다.
   - 기록하는 운전(P → N): "운전 중이에요", "차를 세우고 P에 놓으면 알려 드릴게요."
   - 운전 도중 시작(기어 N에서 `adb shell am force-stop --user 10 kr.skku.unusedfeature` → `am start`): "운전 중이에요", "이번 운전은 기록하지 않아요. 다음 운전부터 지켜볼게요." → P로 돌리면 이전 상황 화면.
   - N에 6초(앱 시간 1분)보다 짧게 두면 운전으로 세지 않아 기록이 바뀌지 않는다.
3. 위 표의 결과 화면을 `adb exec-out screencap -p`로 캡처해 `docs/screenshots/`에 남긴다.
4. 구역 점 누르기: `adb shell input tap <x> <y>`로 공조 패널 점을 눌러 구역 카드가 열리는지 본다.
5. 다크: `adb shell cmd uimode night yes` → 확인 → `adb shell cmd uimode night no`.
6. 세로: 화면 크기만 바꾸면 Pleos가 앱 창을 다시 배치하지 않는다. 대신 앱 창을 세로 비율로 줄인다: `adb shell am task resize <taskId> 853 90 1653 1300` → 확인 → `adb shell am task resize <taskId> 853 90 2535 1300`.

## 통과 기준

- `./gradlew :app:testDebugUnitTest` 실패 0. 18개 기능마다 기회·사용 테스트가 있음.
- 위 시나리오에서 기대 결과와 다른 화면이 없음.
- 주행 중 `doSpeak` 로그 0건.
