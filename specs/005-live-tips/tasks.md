# Tasks: 안 써본 기능 5차 — 주행 중 한마디(주행 최적화)

**Input**: `specs/005-live-tips/` (spec, plan, research, data-model, contracts/ui.md, quickstart)
**Prerequisites**: 에뮬레이터(Pleos Connect, 2560×1440), Virtual Console(`adb forward tcp:8088 tcp:8088`), JDK 17
**작업 위치**: `/Users/jiwon/orca/workspaces/unused-feature-app/live-tips` (브랜치 `live-tips`). 아래 경로는 이 폴더 기준이다.

**Tests**: 헌법 IV에 따라 판단 로직 테스트를 먼저 쓰고 실패를 본 뒤 구현한다.

**FR-014 분기**: T001에서 Pleos가 주행 최적화 선언을 받아들이지 않으면 `[화면]` 표시 작업을 건너뛴다(T011, T012, T014·T019의 화면 확인, T016). 문구 작업(T013)은 분기와 상관없이 한다.
- **T001 결과(2026-11-12)**: 받아들임(research R1). 내비 옆 칸은 창 손잡이를 끌어 만든 1:1 분할이다(research R2). 그래서 `[화면]` 작업을 모두 한다.

## Phase 1: Setup

- [X] T001 주행 최적화 시험(research R1·R2, SC-007)을 한다.
  - `app/src/main/AndroidManifest.xml`의 `MainActivity`에 `<meta-data android:name="distractionOptimized" android:value="true" />`만 넣고 `./gradlew -q :app:installDebug`로 설치한다.
  - 앱을 열고(`adb shell am start --user 10 -n kr.skku.unusedfeature/.MainActivity`) 내비를 연다(`ai.umos.maps.android.navigation.app/.MainActivity`). 앱이 작은 칸에 가는지 캡처로 본다.
    - 작은 칸으로 가지 않으면 내비를 닫고 전체 앱 칸으로 시험한다(research R2 분기). 이후 시연(T018)도 전체 칸이다.
  - 콘솔로 `gear=Drive`를 넣는다(`curl -X PUT "127.0.0.1:8088/vehicle/gear?gear=Drive"`, 하트비트는 `scripts/demo.sh`의 `browser/session`과 같다).
  - `adb logcat -d | grep HOME_SCREEN`에서 이 앱의 `distractionOptimized` 값과 `hideTask` 여부를 보고, 화면을 캡처한다(scratchpad).
  - 결과(받아들임/아님, 근거 줄, 캡처 경로, 작은 칸 여부)를 `specs/005-live-tips/research.md` R1·R2 "결과"에 적는다. P로 되돌린다.

## Phase 2: Foundational (판단 로직, 모든 이야기의 바탕)

- [X] T002 [P] 새 `app/src/test/java/kr/skku/unusedfeature/LiveCoachTest.kt`를 쓴다(data-model §1·§3·§4).
  - FR-005 앞 일곱 조건마다 실패 테스트 하나: `live` 없는 기능 제외(앰비언트), 신호 못 받음, 미지원, 기회 없음, 이미 씀, 익힘, 안내 3번, 이번 운전에 권함, 권함 2번, 5분 안, 방향지시등
    - 앰비언트 제외 테스트는 `DriveStats`를 직접 채운다(`nightMillis` = 11분, `heard`에 AMBIENT). 운전 중 `tick()`은 밤 시간을 채우지 않으므로, tick으로 만든 값이면 `live` 필터가 없어도 통과해 버린다.
  - FR-006 순서: 안내 횟수가 적은 것 → 표 순서
  - `Span.totalAt`·`Tracker.tick()`
    - 와이퍼를 켜고 앱 시계 6분 뒤 `tick()`이면 `rainMillis`가 6분이고 `REAR_DEFROST.opportunity`가 참
    - 와이퍼 LO와 `recirc(true)` 뒤 6분이 지나 `tick()`이면 `rainRecircMillis`가 6분이고 `FRONT_DEFROST.opportunity`가 참
    - 주행거리 신호가 없는 운전에서 앱 시계 21분 뒤 `tick()`이면 `driveMillis`가 21분이고 `CRUISE.opportunity`가 참
  - 시계는 `Tracker { now }`처럼 가짜 시계를 쓴다(`CoachTest`와 같은 방식).
- [X] T003 [P] `app/src/test/java/kr/skku/unusedfeature/CoachTest.kt`에 하나를 더한다. 운전 중 권함으로 안내 횟수가 1이 된 기능을 그 운전 중에 익히면, `Coach.finish`의 `confirmed`가 그 기능이다(FR-012, 1차 확인 규칙 그대로).
- [X] T004 테스트를 돌려 새 테스트가 컴파일 실패 또는 실패인지 본다(`./gradlew -q :app:testDebugUnitTest`).
- [X] T005 [P] `app/src/main/java/kr/skku/unusedfeature/Feature.kt`에 `val live: String?`를 더한다. contracts/ui.md §3 문구 16개를 넣고, `REAR_SEAT_HEAT`·`AMBIENT`는 null이다.
- [X] T006 `app/src/main/java/kr/skku/unusedfeature/Coach.kt`에 순수 코드를 더한다.
  - `Span.totalAt(t)`, `Tracker.elapsed()`
  - `Tracker.tick()`: 운전 중이면 rain·rainRecirc 누적과 `driveMillis = t`를 stats에 쓴다(data-model §4).
  - `Tracker.turnSignal(on)`과 `signalOn`
  - `data class LiveTip(feature, at)`, `object LiveCoach { MAX_TIPS = 2; GAP = 5 * MINUTE; fun next(...) }`(data-model §3)
  - `Coach.finish`는 바꾸지 않는다.
- [X] T007 테스트를 돌려 1~4차 48개와 새 테스트가 모두 통과하는지 본다.

**Checkpoint**: 판단 로직이 테스트로 지켜진다.

## Phase 3: User Story 1 - 운전 중 쓸 만한 기능을 그 순간 듣고 본다 (P1) 🎯 MVP

**Goal**: 기회가 생기면 카드와 음성으로 한 번 권한다.
**Independent Test**: 와이퍼를 손으로 세 번 바꾸면 10초 안에 카드와 음성(SC-001).

- [X] T008 [US1] `app/src/main/java/kr/skku/unusedfeature/CarWatcher.kt`를 고친다.
  - `watchTurnSignal()`을 더한다(research R3의 API). LEFT·RIGHT면 `tracker.turnSignal(true)`, OFF면 false, null이면 무시한다. 값이 올 때마다 `Log.i(TAG, "방향지시등 …")`를 남긴다. 해제는 `unregisters`에 넣는다(다른 구독과 같다).
  - `start()`의 구독 목록에 "방향지시등"을 넣는다.
  - `poll.run()`에서 `readSwitches()` 뒤 `tracker.tick()`과 `onChange()`를 부른다.
  - 확인: 설치 뒤 콘솔 `PUT /vehicle/turn-signal?mode=Left`, `mode=Off`를 넣고 로그에 두 값이 오는지 본다.
- [X] T009 [US1] `app/src/main/java/kr/skku/unusedfeature/Progress.kt`를 고친다(data-model §7).
  - `countShown(f)`: `shown_<f>`를 1 늘린다. 기록·운전 수는 건드리지 않는다.
  - `record(o, count: Boolean = true)`: count가 false면 `shown_<f>`를 늘리지 않는다.
- [X] T010 [US1] `app/src/main/java/kr/skku/unusedfeature/WatchService.kt`를 고친다.
  - companion `@Volatile var tips: List<LiveTip>`
  - `onChange()`에서 `recording`이 false → true로 바뀌면 `tips`를 비운다.
  - 기록하는 운전(`tracker.driving`)이면 `LiveCoach.next(tracker.stats, tracker.elapsed(), tips, progress.shown(), progress.learned(), unsupported, tracker.signalOn)`를 부른다.
  - 결과가 있으면 `tips += LiveTip(f, elapsed)`, `progress.countShown(f)`, `speak(f.live!!)`(실패면 로그), `onUpdate`
  - 판단은 `progress.learn(newly, note)` 뒤에 한다. 방금 쓴 기능을 권하지 않게 하려는 것이다.
  - 상시 알림 문구를 contracts/ui.md §7로 바꾼다.
- [X] T011 [US1] [화면] `app/src/main/java/kr/skku/unusedfeature/Screens.kt`를 고친다.
  - `Ui.where(w, blink: Boolean = true)`: blink가 false면 `pulse`를 걸지 않는다.
  - `Ui.driving(recording, live: Pair<Feature, Boolean>?, compact: Boolean)`: 기다림, 권함, 써 봤어요 세 상태(contracts/ui.md §1). `compact`(세로 배치)면 위 여백 40dp, 타일 120이다. 간격은 32dp다.
- [X] T012 [US1] [화면] `app/src/main/java/kr/skku/unusedfeature/MainActivity.kt`의 `render()`를 고친다.
  - 주행 중 화면 키를 `(driving, recording, 마지막 권함 기능, 써 봤는지)`로 둔다(data-model §6). 키가 같으면 다시 그리지 않는다.
  - 써 봤는지는 `Progress.learned()`에 그 기능이 있는지로 본다.
  - 세로 배치(`resources.configuration.orientation == ORIENTATION_PORTRAIT`)이고 주행 중이면 `R.id.mapPane`을 GONE으로, 아니면 VISIBLE로 둔다(research R8).
  - `ui.driving(WatchService.recording, live, compact = 세로)`를 부른다.
- [X] T013 [US1] 문구와 매니페스트를 고친다(FR-014 분기와 상관없이 한다).
  - `app/src/main/AndroidManifest.xml`: `MainActivity` 위 주석을 5차 내용으로 바꾼다. 내용: 주행 중에는 움직이지 않는 권함 카드만 보이므로 주행 최적화를 선언한다(contracts/ui.md §5). 서비스 용도 문구(`PROPERTY_SPECIAL_USE_FGS_SUBTYPE`)를 §7로 바꾼다.
  - `Screens.kt`: 첫 실행 1단계와 권할 것 없음 문구를 §7로 바꾼다.

**Checkpoint**: 장면 `wiper`처럼 와이퍼를 세 번 바꾸면 권함이 나간다.

## Phase 4: User Story 2 - 권한 기능을 켜면 "써 봤어요" (P1)

**Goal**: 권한 기능이 켜지면 카드가 바뀌고 말하지 않는다.
**Independent Test**: SC-002

- [X] T014 [US2] 에뮬레이터에서 확인한다.
  - 권함 상태에서 먼저 누를 곳 버튼 줄이 화면 안에 있는지 본다(`uiautomator dump`에서 버튼 글자 노드의 bounds가 앱 창 bounds 안).
  - 그다음 `body front_wiper=Auto`를 넣는다.
  - 5초 안에 카드가 "써 봤어요"와 "오토 와이퍼" 두 줄을 보이는지 본다. 화면은 `uiautomator dump`의 글자로 본다.
  - 그 사이 `doSpeak(kr.skku.unusedfeature)` 줄이 없는지 로그로 본다.
  - 화면 확인은 FR-014면 건너뛴다. 이 경우 로그만 본다.

## Phase 5: User Story 3 - 권함이 운전을 방해하지 않는다 (P1)

**Goal**: 두 번, 5분, 방향지시등, 움직임 없음
**Independent Test**: SC-003, SC-004와 T002 테스트

- [X] T015 [US3] `.specify/memory/constitution.md`의 원칙 I을 FR-013 문장으로 고친다. 버전 1.1.0, Last Amended 2026-11-12로 하고, Development Workflow는 그대로 둔다.
- [X] T016 [US3] [화면] 주행 중 권함 카드 상태에서 1초 간격으로 두 장 캡처해 비교한다(SC-004).
  - 자를 영역: `adb shell uiautomator dump`에서 `package="kr.skku.unusedfeature"`인 가장 바깥 노드의 bounds다.
  - 통과: PIL `ImageChops.difference(a, b).getbbox() is None`

## Phase 6: User Story 4 - P에서 같은 말을 다시 듣지 않는다 (P2)

**Goal**: 이번 운전 권함이 P에서 뽑히면 화면만 보이고 말하지 않는다.
**Independent Test**: SC-006

- [X] T017 [US4] `app/src/main/java/kr/skku/unusedfeature/WatchService.kt` `onDriveEnd`를 고친다(data-model §5).
  - 뽑힌 기능이 `tips`에 있으면 `progress.record(outcome, count = false)`를 하고, 발화·알림을 하지 않으며 `lastSpoken = null`로 둔다. 지난 P에 같은 기능을 말한 `Spoken`이 남아 있으면 말하지 않은 이번 P에 "방금 들은 음성" 자막이 붙기 때문이다.
  - 없으면 지금과 같다.
  - `tips`는 다음 운전 시작 때 비우므로 여기서는 지우지 않는다.

## Phase 7: User Story 5 - 시연 장면 (P3)

**Goal**: `scripts/demo.sh live` 한 번으로 보여 준다.
**Independent Test**: 끝 코드 0, 캡처 네 장

- [X] T018 [US5] `scripts/demo.sh`에 `live)` 장면과 맨 위 사용법 한 줄을 더한다(contracts/ui.md §6 1~9단계와 시간표).
  - 와이퍼: `put body front_wiper=Off`로 시작해 Low → High → Low, 4초 간격이다.
  - 주행거리는 넣지 않는다.
  - 방향지시등: `put turn-signal mode=BlinkLeft` / `mode=Off`(콘솔 값, research R3)
  - 난방: `put hvac driver_temp=26`
  - 캡처: `SHOT_DIR`이 있으면 `$ADB exec-out screencap -p > "$SHOT_DIR/<이름>.png"`
  - 기다림: `wait_min`(앱 1분 = 6초)
  - 내비 옆 칸에 두기(research R2 결과): 앱 `am start` 뒤 `adb shell input swipe 2547 720 1500 720 800`으로 창 손잡이를 끌어 1:1 분할을 만든다. 분할이 안 되면(홈 화면 로그에 `driver: 1, passenger: 1`이 없으면) 전체 칸으로 이어 간다.
- [X] T019 [US5] `reset` 뒤 `live`를 돌린다. 캡처와 로그를 scratchpad에 남기고 아래를 확인한다.
  - SC-001: 세 번째 바꿈 뒤 10초 안 권함과 음성 한 번
  - SC-002: T014와 같다.
  - SC-003: 권함 2번, 두 권함의 앱 시계 간격 5분 이상(로그 시각 × 10), 방향지시등 켠 동안 `doSpeak` 없음
  - SC-006: P 화면이 시트 열선 안내이고 P에서 `doSpeak`가 없으며 "오토 와이퍼, 써 보셨네요" 띠가 있음
  - US4-3: P 뒤 차 지도의 시트 구역 카드에서 시트 열선 줄이 "안내함 1회"(dump 글자)
  - US1-3: 시연 끝에 한 번 더, 다른 앱(설정)을 앞에 둔 채 와이퍼를 세 번 바꿔 음성이 나오는지 보고, 앱을 열어 카드가 그 권함인지 본다.
  - FR-003: 주행 중 dump에서 이 앱 노드에 `clickable="true"`가 없음
- [X] T020 [US5] 1~4차 장면 회귀: `reset` 뒤 `climate`, `learn`, `wiper`, `rain`, `highbeam`, `heat`, `highway`, `night`, `multi`를 돌린다. 결과를 spec "1~4차와의 관계" 표와 견주어 verification에 적는다.

## Phase 8: Polish & Cross-Cutting Concerns

- [X] T021 [P] 문서를 고친다.
  - `사용설명서.md` 17·50·52·130·131행: contracts/ui.md §7 문구와 그림(`docs/screenshots/40-live-tip.png`, T019 `live-tip1.png`를 1280×720으로)
  - `README.md`: 77행 시연 설명과 96행 한계를 §7대로 고친다. "4차" 절과 같은 형식으로 "5차: 주행 중 한마디" 절을 더한다(무엇이 바뀌는지, `live` 장면, 1:1 분할 만드는 법, 되돌리기 태그).
- [X] T022 ponytail-review로 `git diff backup/before-live-tips -- app scripts`를 본다. 지적은 고치거나 까닭을 적는다.
- [X] T023 /code-review로 같은 diff를 본다. 맞는 지적은 고친다.
- [X] T024 보안을 직접 점검한다(이 작업 폴더 밖에서 /security-review가 돌지 않으므로 같은 관점). 새 권한이 없는지, 외부 전송이 없는지, 매니페스트 변경을 본다.
- [X] T025 `specs/005-live-tips/verification.md`를 쓴다. 단위 테스트 수, R1 결과와 근거, SC-001~SC-007 결과와 캡처 경로, 회귀 결과, 리뷰 결과, 남은 한계를 적는다.
- [X] T026 `live-tips` 브랜치에 로컬로 커밋한다(푸시하지 않음, 사용자 위임 2026-11-12).

## Dependencies & Execution Order

- T001(시험) → 결과가 [화면] 작업 여부와 시연 자리(작은 칸/전체 칸)를 정한다. 판단 작업(T002~T010)은 결과와 상관없이 한다.
- Phase 2(T002~T007) → Phase 3 이후 전부
- T008·T009 → T010 → T011·T012(화면) → T014·T016(확인)
- T017은 T009·T010 뒤
- T018 → T019 → T020
- T021~T025는 T020 뒤, T026은 맨 끝

## Parallel Opportunities

- T002와 T003(다른 테스트 파일), T005(Feature.kt)는 동시에 할 수 있다.
- T015(헌법)와 T021(문서)은 코드와 상관없어 언제든 한다.

## Implementation Strategy

- MVP: Phase 1~3(권함 판단, 음성, 카드). 그다음 US2·US3 확인, US4, 시연 순이다.
- 화면이 받아들여지지 않으면(FR-014) MVP는 음성 권함이고, 시연 캡처 대신 음성 로그로 확인한다.
