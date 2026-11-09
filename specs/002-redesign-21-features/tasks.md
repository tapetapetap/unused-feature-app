# Tasks: 안 써본 기능 2차 — 보이는 화면과 기능 18개

**Input**: Design documents from `specs/002-redesign-21-features/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ui.md, quickstart.md

**Tests**: 원칙 IV에 따라 판단 로직(`Feature.kt`, `Coach.kt`) JUnit 테스트를 포함한다. 테스트를 먼저 쓰고 실패를 확인한 뒤 구현한다.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 다른 파일이고 앞 작업에 기대지 않아 동시에 할 수 있음
- 경로는 저장소 루트 기준

## Phase 1: Setup

**Purpose**: 권한, 색, 아이콘, 차 그림 리소스

- [X] T001 `app/src/main/AndroidManifest.xml`: 조회 권한 `pleos.car.permission.CAR_WINDOWS`, `CAR_MIRRORS` 추가(`CAR_SAFETY`는 넣었다가 뺐다, 구현 메모), 앱 아이콘을 `@mipmap/ic_launcher`로
- [X] T002 [P] `app/src/main/res/values/colors.xml`, `app/src/main/res/values-night/colors.xml`: Pleos 토큰(research R8) — 배경, 카드, 선, 글자 3단, 파랑, 초록, 빨강, 흐린 파랑·초록 바탕, 차 그림 색
- [X] T003 [P] `app/src/main/res/drawable/ic_f_*.xml`: 기능 18개 선 아이콘(24dp, 굵기 1.8, 둥근 끝, 흰색 — 코드에서 색 입힘). 시안 SVG를 옮기고 새 12개를 그림
- [X] T004 [P] `app/src/main/res/drawable/ic_z_*.xml`, `ic_k_*.xml`, `ic_check.xml`, `ic_clock.xml`, `ic_spark.xml`, `ic_ok.xml`: 구역 7개, 근거 종류, 화면 보조 아이콘(주행 중 타일은 구역 아이콘 `ic_z_wheel`을 쓴다)
- [X] T005 [P] `app/src/main/res/drawable/car_top.xml`: 시안의 위에서 본 차(300×560)를 벡터로, 색은 color 리소스 참조
- [X] T006 [P] `app/src/main/res/drawable/ic_launcher_bg.xml`, `ic_launcher_fg.xml`, `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`, `app/src/main/res/drawable/ic_notify.xml`: 적응형 앱 아이콘(보라→파랑 그라데이션 + 흰 반짝임, 안전 영역 안), 알림 아이콘도 반짝임으로

**Checkpoint**: `./gradlew :app:assembleDebug` 성공. 다크 테마에서 차 그림 색이 바뀐다(`docs/screenshots/08-dark-theme.png`). 앱 아이콘 전경 꼭짓점이 모두 중심에서 29.3 이내다(108 viewport, 안전 영역 반지름 33, `ic_launcher_fg.xml` 좌표로 계산).

---

## Phase 2: Foundational

**Purpose**: 판단 모델을 18개를 담을 수 있게 바꾼다(1차 6개 동작은 그대로). 모든 이야기의 바탕.

- [X] T007 `app/src/test/java/kr/skku/unusedfeature/CoachTest.kt`: 가짜 시계 `Tracker { now }`로 1차 테스트 17개를 새 모델(`used`/`heard` 집합)로 옮기고, 근거 기록(Mark: 풍량·온도 변경 시각과 글자), `Span`(켜짐→꺼짐 누적, 운전 끝에 닫기), `Marks.encode/decode` 왕복, `Outcome`의 reason·marks·used·driveMillis 테스트 추가 — 먼저 작성하고 실패 확인
- [X] T008 `app/src/main/java/kr/skku/unusedfeature/Feature.kt`: `Zone`(7개, 이름·아이콘·좌표·이름표 방향, data-model), `Kind`(근거 종류), `Where`(자리, 버튼들, 강조 번호), `Feature`(이름, 구역, 아이콘, 음성, 이유 함수, 근거 제목, 언제, 누를 곳, 근거 종류, 기회 함수) — 1차 6개만 먼저 contracts/ui.md 문장으로
- [X] T009 `app/src/main/java/kr/skku/unusedfeature/Coach.kt`: `Feature` 떼어 냄. `DriveStats`(횟수, 온도, 구간 시간, `used`, `heard`, `marks`), `Mark`, `Span`, `Tracker(clock)`(1차 setter + 시각 기록), `Outcome(pick, confirmed, reason, marks, used, driveMillis)`, `Coach.finish`, `Marks.encode/decode` — T007 통과
- [X] T010 `app/src/main/java/kr/skku/unusedfeature/Progress.kt`: 새 키(`learned_at_`, `learned_note_`, `last_reason`, `last_marks`, `last_used`, `last_drive_ms`, `drives`, `first_drive`), `drove` 대신 `drives`, `record(outcome)`, `learn(features, note)`. 확인: `reset` → `climate` → `learn` 뒤 `adb shell run-as kr.skku.unusedfeature --user 10 cat shared_prefs/progress.xml`에 `drives`=2, `first_drive`, `shown_AUTO_CLIMATE`=1, `learned_AUTO_CLIMATE`=true, `learned_at_AUTO_CLIMATE`, `learned_note_AUTO_CLIMATE`, `last_confirmed`=AUTO_CLIMATE가 있다. 1차 `drove`만 있으면 `drives()`는 1이다(`Progress.drives()`)

**Checkpoint**: 1차 6개 판단이 새 모델에서 그대로 통과

---

## Phase 3: User Story 1 - 더 많은 순간에서 놓친 기능을 찾아 준다 (Priority: P1) 🎯 MVP

**Goal**: 18개 기능의 기회·사용을 차 신호로 판단해 하나를 안내한다.

**Independent Test**: `scripts/demo.sh wiper` → P 전환 때 "오토 와이퍼" 음성(`adb logcat -d | grep "doSpeak(kr.skku"`, quickstart).

- [X] T011 [US1] `app/src/test/java/kr/skku/unusedfeature/CoachTest.kt`: 새 12개 기능의 기회 있음/없음·사용 테스트(data-model 표), 가짜 시계로 비·밤·비+내기 구간(5분·10분 경계), 고속 구간(1분 창, 시속 70km 경계, 주행거리 신호 없을 때 크루즈 20분 대체), 와이퍼·하이빔·내기 수동 변경 횟수, 표 순서 우선순위 — 먼저 작성하고 실패 확인
- [X] T012 [US1] `app/src/main/java/kr/skku/unusedfeature/Feature.kt`: 새 12개 기능을 data-model 순서와 contracts/ui.md 문장으로 추가
- [X] T013 [US1] `app/src/main/java/kr/skku/unusedfeature/Coach.kt`: Tracker에 새 신호 setter(와이퍼 동작·스위치, 하이빔·하이빔 스위치, 내기, 오토 내/외기, MAX A/C, 앞·뒤 제상, 미러 열선, 주행 모드, 주행거리, 조수석·뒷좌석 시트 온도, 뒷좌석 착좌, 앰비언트)와 구간 계산 — T011 통과
- [X] T014 [US1] `app/src/main/java/kr/skku/unusedfeature/CarWatcher.kt`: 앱 시계(디버그 10배, research R5)로 Tracker 생성, 새 신호 구독(research R1, 좌석·창·미러 Area 고르기 공통화), 운전 중 3초 주기 읽기(`getCurrentWiperSwitch`, 전조등·하이빔 스위치), 운전 끝에 주기 멈춤, 차에 없다고 확인된 기능을 기능 단위로 알림(data-model "지원 기능")
- [X] T015 [US1] `app/src/main/java/kr/skku/unusedfeature/WatchService.kt`: `Outcome`을 Progress에 저장, `driving`(주행 중 여부)과 `unsupported`(지원 안 되는 기능) 공개, 익힘 때 상황 글("운전을 시작하고 n분 뒤"/"정차 중") 저장, 음성·알림은 1차 그대로
- [X] T016 [US1] `scripts/demo.sh`: 10배 시계에 맞춰 대기 시간 조정, 시나리오 `wiper`, `rain`, `highbeam`, `night`, `highway`, `heat`, `reset` 추가(quickstart 표, Virtual Console 경로는 research R1)

**Checkpoint**: 화면을 바꾸기 전에도 새 시나리오에서 맞는 음성이 나온다

---

## Phase 4: User Story 2 - 안내 화면이 "왜"와 "어디"를 보여 준다 (Priority: P1)

**Goal**: 안내 화면(아이콘 타일, 문장, 근거 타임라인, 누를 곳)과 지도 구역 깜빡임.

**Independent Test**: `scripts/demo.sh climate` 뒤 근거 3개, AUTO 강조, 공조 패널 점 깜빡임(quickstart).

- [X] T017 [US2] `app/src/main/java/kr/skku/unusedfeature/Views.kt`: `TimelineView`(점·막대, "{m}분 · 글자", 점 6개 넘으면 눈금+횟수, 출발/도착 표시), `RingView`(진행 원), `CarMapView`(car_top 그리기, 구역 7개 원·아이콘·진행 링·이름표, 파랑 무한 깜빡임, 초록 3번 반짝임, 흐림 모드, 누르기 콜백)
- [X] T018 [US2] `app/src/main/res/layout/activity_main.xml`(가로: 왼쪽 600dp 지도 칸 — 진행 원 머리, CarMapView, 범례 / 오른쪽 상황 칸 ScrollView), `app/src/main/res/layout-port/activity_main.xml`(세로: 위 상황 칸 / 아래 지도 + 구역 목록)
- [X] T019 [US2] `app/src/main/java/kr/skku/unusedfeature/Screens.kt`: 글자·카드·칩·아이콘 타일·누를 곳 띠를 만드는 작은 함수들(색은 colors.xml), 안내 화면(작은 제목, 타일+큰 제목+이유, 근거 카드+TimelineView, 누를 곳 카드+각주), 확인 띠, 을/를 조사
- [X] T020 [US2] `app/src/main/java/kr/skku/unusedfeature/MainActivity.kt`: 뼈대 레이아웃 연결, TIP 상태에서 안내 화면 + 지도 해당 구역 깜빡임, `WatchService.onUpdate`로 다시 그리기

**Checkpoint**: 안내 화면만으로 왜·어디를 답할 수 있다

---

## Phase 5: User Story 3 - 내 차 지도와 도감으로 진행을 본다 (Priority: P2)

**Goal**: 구역 진행도, 진행 원, 구역 카드, 도감 완성.

**Independent Test**: 기록이 있는 상태에서 구역 점을 눌러 구역 카드가 열리고 숫자가 기록과 맞는지(quickstart 화면 확인 4).

- [X] T021 [US3] `app/src/main/java/kr/skku/unusedfeature/Screens.kt`: 구역 카드 화면(구역 이름, 익힌/지원, 기능 카드: 상태 칩, 언제, 누를 곳, ✕ 닫기), 도감 완성 화면(큰 진행 원, 기간·운전 횟수, 기능별 날짜 칸)
- [X] T022 [US3] `app/src/main/java/kr/skku/unusedfeature/MainActivity.kt`: 지도·세로 구역 목록 누르기 → ZONE 상태, 닫기, 진행 원 머리 글자, 지원 수 = 18 − `WatchService.unsupported`, COMPLETE 판단

**Checkpoint**: 지도에서 구역을 눌러 기능을 둘러볼 수 있다

---

## Phase 6: User Story 4 - 상황마다 화면이 알맞게 바뀐다 (Priority: P2)

**Goal**: 첫 실행, 주행 중, 확인, 권할 것 없음 화면과 알림 줄, 라이트·다크, 가로·세로.

**Independent Test**: quickstart 화면 확인 1, 2, 5, 6.

- [X] T023 [US4] `app/src/main/java/kr/skku/unusedfeature/Screens.kt`: 첫 실행(보라 타일, 세 단계), 주행 중(운전대 타일, 큰 글자, 띠), 확인(초록 타일+체크, 도감 칸 막대·칩, 처음 쓴 순간), 권할 것 없음(쓴 기능 칩, 남은 기능 칩), 알림 줄(연결 실패. 문구는 contracts §5이고, 넣는 쪽 `CarWatcher`와 지우는 쪽 `WatchService`가 같은 상수 `NO_SIGNAL`(`CarWatcher.kt`)을 쓴다. 없는 기능은 알림 줄 없이 구역 카드에 "이 차에 없음")
- [X] T024 [US4] `app/src/main/java/kr/skku/unusedfeature/MainActivity.kt`: 상태 우선순위(DRIVING > ZONE > FIRST_RUN > COMPLETE > TIP > CONFIRM > NONE, data-model), 주행 중 지도 흐림·누르기 막기·화면 고정, 알림 줄

**Checkpoint**: 6가지 상황이 모두 시안대로 나온다

---

## Phase 7: Polish & 검증

- [X] T025 에뮬레이터에서 quickstart 시나리오 전부 실행, 다크·세로 확인, 화면을 `docs/screenshots/`에 저장
- [X] T026 [P] `specs/002-redesign-21-features/verification.md`: 테스트 결과, 시나리오별 결과·근거, 확인 못 한 것
- [X] T027 [P] `README.md`: 기능 18개, 시나리오, 시연용 시간 압축, 한계 갱신

---

## Phase 8: goldfish 리뷰 뒤 (2026-11-08, 사용자 결정 포함)

**Purpose**: 진행 원 0/14 확인, goldfish-review #6·#17 사용자 결정 반영.

- [X] T028 `app/src/main/java/kr/skku/unusedfeature/CarWatcher.kt`: 전조등·하이빔 스위치 선택지를 로그로 남긴다("전조등 스위치 선택지: …"). 에뮬레이터는 둘 다 `[OFF, ON]`이라 오토 라이트·하이빔 보조가 지원 안 함인 것을 확인
- [X] T029 `app/src/main/java/kr/skku/unusedfeature/Coach.kt`, `WatchService.kt`, `app/src/test/java/kr/skku/unusedfeature/CoachTest.kt`: 지원 안 하는 기능은 신호가 와도 안내 후보에서 뺀다(`Coach.finish`의 `unsupported`, data-model "지원 기능"). 테스트 `unsupportedIsNeverPicked` 먼저 작성
- [X] T030 `CarWatcher.kt`(`notParked`, 기어가 바뀔 때마다 `onChange`), `WatchService.kt`(`driving` = 기어 ≠ P, `recording`), `Screens.kt`(`driving(recording)`), `MainActivity.kt`: 기어가 P가 아니면 늘 주행 중 화면, 기록하지 않는 운전이면 아랫줄 "이번 운전은 기록하지 않아요. 다음 운전부터 지켜볼게요." (goldfish #17)
- [X] T031 에뮬레이터: N·R 기어에서 화면이 가려지는지(캡처), 가려진 글자 두 문구(UI 트리 덤프), `highbeam`·`multi` 다시 실행, T010 기록 키 확인 (goldfish #6)
- [X] T032 [P] `spec.md`, `data-model.md`, `contracts/ui.md`, `quickstart.md`, `verification.md`, `README.md`: 위 결과 반영

- [X] T033 ponytail-review(지적 없음), /code-review high 10건 중 7건 반영: 기록 안 하는 운전의 익힘 메모 "운전 중", 같은 연결 실패 띠면 다시 그리지 않음, 초기화 성공 때 띠 지움, TIP은 지원 기능일 때만, `onDestroy`에서 화면 알림, 운전 끝 정리를 `onChange` 한 곳으로, `notParked` 정리(verification.md "위 변경의 리뷰")

**Checkpoint**: 테스트 42개 통과, 진행 원 0/14, `highbeam`은 "권할 것 없음", 주행 중 화면 두 문구가 덤프에 보임

---

## Dependencies & Execution Order

- Phase 1 → Phase 2 → US1(Phase 3) → US2(Phase 4) → US3·US4(Phase 5·6, 같은 `Screens.kt`/`MainActivity.kt`라 차례로) → Phase 7.
- US1은 화면 없이도 음성으로 확인할 수 있어 첫 증분이다. US2부터는 Phase 1의 색·아이콘·차 그림이 필요하다.
- 같은 파일을 고치는 작업(T007/T011, T008/T012, T009/T013, T019/T021/T023, T020/T022/T024)은 순서대로.

## Parallel Example

```text
Phase 1: T002, T003, T004, T005, T006 동시에 (서로 다른 리소스 파일)
Phase 7: T026, T027 동시에
```

## Implementation Strategy

1. MVP = Phase 1~3: 18개 기능 판단과 음성 안내. 1차 화면으로도 동작 확인.
2. Phase 4: 안내 화면 — 사용자가 지적한 "안 보임"을 해결하는 핵심.
3. Phase 5~6: 지도 탐색, 나머지 상황 화면, 테마·방향.
4. Phase 7: 에뮬레이터 검증과 문서.

## 구현 메모 (2026-11-08)

- T001·T003·T014: 표시등 읽기(`getTelltalesInfo`)가 구현상 `PRIVATE_CAR_SAFETY`를 요구해(에뮬레이터 SecurityException, `javap -v SafetyImpl`) 오토 홀드·LFA·i-PEDAL과 `CAR_SAFETY` 권한, 브레이크 구독, 해당 아이콘 3개를 뺐다. 기능 21개 → 18개. 설계 문서 "구현 중 바뀐 것".
- T016: 시나리오 `hold`·`hold-learn` 대신 `highbeam`, `night`(앰비언트), `reset`(차 상태 기본값 + 앱 기록 삭제)을 넣었다. `reset`이 없으면 앞선 시연에서 켜 둔 기능(예: AUTO)이 남아 앱이 바로 "익힘"으로 기록해 시연이 매번 달라졌다.
- T018: 지도 칸 고정 600dp → 3:5 비율, 지도 머리는 `layout/map_head.xml`로 공유.
- goldfish 리뷰 뒤(2026-11-08): 작업 본문을 18개 기준으로 고쳐 썼다(위 메모는 바뀐 이력). T011이 요구한 시속 70km 경계 테스트가 빠져 있어 `highwayBoundaryIs70`을 더했다. T023의 연결 실패 문구가 1차 문구로 남아 있어 contracts §5 문구와 공용 상수로 고쳤다.
