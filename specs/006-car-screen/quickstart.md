# Quickstart: 안 써본 기능 6차 확인

작업 위치는 `/Users/jiwon/orca/workspaces/unused-feature-app/car-unused`(브랜치 `car-unused`)다. 에뮬레이터 준비와 콘솔 연결은 README "시연"과 같다. 에뮬레이터를 켤 때마다 `adb forward tcp:8088 tcp:8088`을 한다(안 하면 `demo.sh` 쓰기가 조용히 실패한다).

## 1. 단위 테스트와 빌드 (SC-006)

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
./gradlew -q :app:testDebugUnitTest :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

- 기대: 기존 테스트와 새 `evidenceCount` 테스트가 모두 통과한다.

## 2. 글자와 누르는 칸 (SC-001, SC-002)

1. `scripts/demo.sh reset` 뒤 `scripts/demo.sh seed`로 추천 기록을 만든다.
2. 다음 화면을 낮(`adb shell cmd uimode night no`)과 밤(`yes`)에서 캡처한다(`adb exec-out screencap -p`): P 안내, 추천 기록 목록, 기록 다시 보기, 구역 카드, 첫 실행 화면은 `pm clear` 뒤.
3. 각 화면에서 `adb shell uiautomator dump`의 `clickable="true"` 노드 bounds를 읽어 contracts §2의 누르는 칸이 모두 76×76 이상인지 본다.
4. 코드 검사: `grep -nE '\b(1[0-9]|2[0-3])f\b' app/src/main/java/kr/skku/unusedfeature/Screens.kt app/src/main/java/kr/skku/unusedfeature/MainActivity.kt`로 후보를 모두 뽑고, 글자 크기 인자인 것만 본다(반지름·간격·높이 인자는 무시). 남는 글자 크기는 누를 곳 그림(`where`)의 22f뿐이다. `Views.kt`의 `sp(..f)`는 22f(이름표·타임라인)만 있다.
5. 간격: 세로 구역 목록과 추천 기록 목록의 UI 덤프 bounds에서 위 줄 아래 끝과 다음 줄 위 끝의 차가 24 이상이다.
6. 대비: contracts §3 표의 값으로 다시 계산해 4.5:1(spec 용어의 큰 글자 예외는 3:1) 이상인지 본다.

## 3. 세로 1:1 칸과 주행 중 카드 (spec US1-6, research R4)

1. 내비를 열고 창 손잡이를 가운데로 끌어 1:1로 나눈다(`adb shell input swipe 2547 720 1500 720 800`).
2. `scripts/demo.sh live`를 시작하고, 기어가 D로 바뀐 뒤 첫 권함 전(장면 시작 후 약 10초 안)에 앱 칸(가로 853~1700, 세로 90~1300)을 `adb exec-out screencap -p`로 캡처한다(기다림 카드). 권함이 뜨면 한 번 더 캡처한다.
3. 가장 긴 누를 곳 그림: 분할을 둔 채 `scripts/demo.sh reset` 뒤 `scripts/demo.sh rain`을 돌려 앞유리 김서림 제거 권함("앞유리 김서림", "뒷유리 열선", "A/C")을 캡처한다.
4. 기대: 기다림 문구(contracts §4), 커진 누를 곳 버튼 그림이 칸 안에 한 줄로 들어온다.
5. P로 바꾼 뒤(장면 끝) 세로 화면을 캡처한다: 구역 줄이 76dp 이상·사이 24dp, 기능 없는 전조등 구역 줄 오른쪽이 "기능 없음", 세로 차 지도에는 이름표가 없고 구역 점을 눌러도 아무 일이 없으며, 구역 줄을 누르면 구역 카드가 열린다. 가로 화면 차 지도 이름표("전조등 · 기능 없음" 포함)는 지도 칸 안에 있다(T020 뒤에 확인).
5-1. 칩 격자와 새 칩: 분할 화면과 가로 화면 각각에서 `scripts/demo.sh highbeam`(이 에뮬레이터에서는 "권할 것 없음") 뒤 P 화면, `scripts/demo.sh climate` → `scripts/demo.sh learn`("공조 AUTO, 써 보셨네요") 뒤 P 화면을 캡처한다. 기대: 세로는 칩 2열·가로는 3열, 새로 익힌 칩이 진한 초록 글자에 초록 테두리. 도감 완성 화면은 §2의 4 코드 검사로만 확인한다.
6. 분할을 푼다(`adb shell input swipe 1706 720 2550 720 800`).

## 4. 뒤로 버튼과 낮·밤 (SC-003)

| 시작 | 동작 | 기대 |
|---|---|---|
| 기록 다시 보기 | `adb shell input keyevent 4` | 기록 목록, 보던 스크롤 자리 |
| 기록 목록 | 뒤로 | 상황 화면 |
| 구역 카드 | 뒤로 | 상황 화면 |
| 상황 화면 | 뒤로 | 앱을 나감(홈) |
| 구역 카드 | `cmd uimode night yes` | 같은 구역 카드가 밤 색으로 |
| 기록 목록(아래로 스크롤) | 낮·밤 바꿈 | 같은 목록, 같은 스크롤 |
| 기록 다시 보기 | 낮·밤 바꿈 | 같은 기록 다시 보기, 같은 스크롤 |
| 기록 다시 보기(목록을 스크롤해서 연 것) | 낮·밤 바꿈 → 뒤로 | 기록 목록, 열기 전 스크롤 자리 |
| 구역 카드 | 홈 → 다시 열기 | 상황 화면(지금과 같음) |

## 5. 문구 (SC-004)

1. 주행 중 기다림 카드(§3의 2에서 권함 전 캡처)의 두 문구.
2. `scripts/demo.sh seed` 뒤 "추천 기록" → 앞유리 김서림 제거 기록을 연다. 근거 카드 제목 "이번 운전에서 · 비 온 구간과 내기 순환", 오른쪽 숫자 없음, 표시 "4분 · 내기 순환 켬"(샘플 기록 글자도 T019에서 바꾼다). `rain` 장면은 주행 중 권함 때문에 P에서 뒷유리 열선을 안내하므로(5차 verification) 이 확인에 쓰지 않는다.
3. 차 지도의 "전조등 · 기능 없음", 전조등 구역 카드의 "이 차에서는 쓸 수 있는 기능이 없어요".

## 6. 회귀와 점검

- 1~5차 장면(`climate`, `wiper`, `rain`, `highway`, `heat`, `multi`, `live`)이 5차 verification과 같은 안내를 고른다.
- `/impeccable critique`와 `audit`를 다시 돌려 점수를 적는다(SC-005).
- 캡처는 `/Users/jiwon/Desktop/현대자동차 공모전/화면 점검 1113/고친 뒤/안 써본 기능/`에 둔다.
