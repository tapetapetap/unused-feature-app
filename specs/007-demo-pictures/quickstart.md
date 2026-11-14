# Quickstart: 안 써본 기능 7차 확인

작업 위치는 `/Users/jiwon/orca/workspaces/unused-feature-app/car-unused`(브랜치 `car-unused`)다.

## 1. 단위 테스트와 빌드 (SC-002, SC-003)

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
./gradlew :app:testDebugUnitTest --tests kr.skku.unusedfeature.SeedTest  # 고치기 전: picturesMatchTheSentences만 실패
./gradlew :app:testDebugUnitTest :app:assembleDebug                     # 고친 뒤: 57개 통과
git diff --stat backup/before-pictures-unused -- app/src/main            # 빈 출력 = 출시 앱 그대로
```

- 고치기 전 실패 메시지(`app/build/test-results/testDebugUnitTest/TEST-kr.skku.unusedfeature.SeedTest.xml`)에서 두 map이 FRONT_DEFROST만 다르다(문장 8분, 센 값 11분).
- 고친 뒤 지금 56개(CoachTest 42, HistoryTest 3, LiveCoachTest 7, PickAreaTest 1, SeedTest 3)와 새 1개가 통과한다.

## 2. 에뮬레이터 캡처 (SC-001)

에뮬레이터 하나를 세 앱(안 써본 기능, 같은편, DRIVE DNA)이 함께 쓴다. 다른 앱 작업이 쓰는 중이면 끝난 뒤 차례로 쓴다.

- 화면 2560×1440, density 160(1dp = 1px). 앱 사용자는 `--user 10`이다.
- 꺼져 있으면 Cold boot로 켠다: `$HOME/Library/Android/sdk/emulator/emulator -avd pleos -no-snapshot &`(README "빌드").
- Virtual Console 페이지(127.0.0.1:8088)는 닫아 둔다. 열려 있으면 페이지의 토글 값이 다시 들어가 샘플 익힘이 바뀔 수 있다(README).

```sh
ADB=$HOME/Library/Android/sdk/platform-tools/adb
SHOT="/Users/jiwon/Desktop/현대자동차 공모전/화면 점검 1115/안 써본 기능"; mkdir -p "$SHOT"
$ADB forward tcp:8088 tcp:8088   # 부팅 뒤 한 번. 안 하면 demo.sh 쓰기가 조용히 실패한다
$ADB install -r --user 10 app/build/outputs/apk/debug/app-debug.apk
scripts/demo.sh seed             # "샘플 상태를 넣었어요. 화면: 앰비언트 라이트 안내, 추천 기록 6개"
```

1. 가로: UI 덤프(`$ADB shell uiautomator dump /sdcard/u.xml`, `$ADB shell cat /sdcard/u.xml`)에서 "추천 기록 6" 노드 bounds의 가운데를 `$ADB shell input tap <x> <y>`로 누른다. 목록에서 "앞유리 김서림 제거"(맨 위 날짜 묶음 = 어제 날짜의 둘째 카드, 오전 9:00)도 같은 방법으로 누른다.
2. 근거 카드("이번 운전에서 · 비 온 구간과 내기 순환")가 다 보이게 오른쪽 칸 안에서 위로 민다(예: `$ADB shell input swipe 2000 1100 2000 500 600`). `$ADB exec-out screencap -p > "$SHOT/01-김서림-가로.png"`.
3. 1:1 세로: `$ADB shell input swipe 2547 720 1500 720 800`로 나눈다. 앱 칸은 왼쪽(가로 853~1700, 세로 90~1300)이다. 다시 보기가 그대로인지 보고(6차 설정 변화 보존, 닫혔으면 1처럼 다시 연다), 위 칸 안에서 근거 카드까지 민다(예: `$ADB shell input swipe 1270 700 1270 200 600`). `"$SHOT/02-김서림-세로.png"`로 캡처한다.
4. 되돌리기: `$ADB shell input swipe 1706 720 2550 720 800`. 기기 임시 파일을 지운다(`$ADB shell rm -f /sdcard/u.xml`). 이 작업이 켠 에뮬레이터이고 다른 앱이 더 쓰지 않으면 `$ADB emu kill`로 끈다.

## 3. 캡처에서 볼 것

- 비 막대 "와이퍼 12분"(3~15분) 안에 점 둘: "4분 · 내기 순환 켬", "12분 · 외기로 바꿈"(research R2).
- 두 점 글자가 한 줄에 나란히 있고, 서로·막대 글·"출발"·"도착 18분"과 겹치지 않는다(가로·세로 모두).
- 카드 제목 "이번 운전에서 · 비 온 구간과 내기 순환", 오른쪽 숫자 없음. 이유 "비가 오는 동안 내기 순환을 8분 켜 두셨어요."
- 회귀(FR-004): `seed` 직후 첫 화면은 앰비언트 라이트 안내이고 버튼은 "추천 기록 6  ›"다(공백 두 칸, MainActivity.kt:186).
- impeccable audit는 돌리지 않고 이 확인으로 갈음한다(research R4).
