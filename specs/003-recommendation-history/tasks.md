# Tasks: 추천 기록 페이지

**Input**: Design documents from `specs/003-recommendation-history/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ui.md, quickstart.md

**Tests**: 원칙 IV에 따라 저장 형식(`History`)의 JUnit 테스트를 포함한다. 테스트를 먼저 쓰고, 실패하는 것을 확인한 뒤 구현한다.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 다른 파일이고 앞 작업에 기대지 않아 동시에 할 수 있음
- 경로는 저장소 루트 기준이다. 소스는 `app/src/main/java/kr/skku/unusedfeature/`, 테스트는 `app/src/test/java/kr/skku/unusedfeature/`다.

## Phase 1: Setup

새 의존성, 새 파일, 새 권한이 없어서 할 일이 없다(plan Technical Context).

---

## Phase 2: Foundational (기록 저장)

**Purpose**: 안내가 나간 운전마다 기록을 남기고 읽는다. 세 이야기 모두 이 단계가 있어야 한다.

- [X] T001 `app/src/test/java/kr/skku/unusedfeature/CoachTest.kt`: `History` 테스트를 먼저 쓴다. `History`가 아직 없어 컴파일이 실패하는 것을 확인한다.
  - 왕복: 근거 여러 개, 근거 없음, 이유에 `|`·`→`·`°`가 든 기록 2개를 `encode` → `decode`하면 같은 값이다.
  - 손상 건너뛰기: 필드 수가 다른 기록, 모르는 기능 이름, 숫자가 아닌 `at`이 섞여 있어도 나머지 기록은 읽힌다.
  - 덧붙이기: `append(null, r)`·`append("", r)`는 기록 1개, `append(기존, r)`는 기존 뒤에 붙는다. 앞의 손상 기록도 문자열에 남는다.
- [X] T002 `app/src/main/java/kr/skku/unusedfeature/Coach.kt`: `object Marks` 바로 아래에 둘을 더한다.
  - `data class Record(val at: Long, val pick: Feature, val reason: String, val driveMs: Long, val marks: List<Mark>)`
  - `object History { encode, decode, append }`
  - 형식은 data-model "저장"을 따른다. 기록끼리 `\u001E`, 필드끼리 `\u001F`로 나누고, 근거는 `Marks.encode/decode`를 쓴다. `decode`는 필드가 정확히 5개이고 숫자와 기능 이름을 읽을 수 있는 기록만 남긴다(`runCatching`).
  - T001이 통과해야 한다.
- [X] T003 `app/src/main/java/kr/skku/unusedfeature/Progress.kt`:
  - `record(o)`에서 `o.pick`이 있으면 `putString("history", History.append(p.getString("history", null), Record(System.currentTimeMillis(), pick, o.reason, o.driveMillis, o.marks)))`를 한다.
  - `fun history(): List<Record> = History.decode(p.getString("history", null)).asReversed()`를 더한다(최신이 앞).

**Checkpoint**: `./gradlew :app:testDebugUnitTest`가 통과한다. `scripts/demo.sh reset` → `climate` 뒤 `adb shell run-as kr.skku.unusedfeature --user 10 cat shared_prefs/progress.xml`에 `history` 키가 있다.

---

## Phase 3: User Story 1 - 지난 안내를 목록으로 본다 (Priority: P1) 🎯 MVP

**Goal**: 지도 머리의 "추천 기록" 버튼으로 날짜별 기록 목록을 열고 닫는다.

**Independent Test**: quickstart 시나리오 1, 2, 4, 7(빈 목록, 기록 2개 최신순, 권할 것 없던 운전은 안 남음, 앱을 다시 시작해도 남음).

- [X] T004 [P] [US1] `app/src/main/res/layout/map_head.xml`: 진행 원 옆 글자 묶음(세로 `LinearLayout`, "내 차 기능"·`mapSub`)의 셋째 줄에 `TextView` `@+id/historyButton`을 더한다(`layout_marginTop` 10dp, `wrap_content`). 가로·세로가 같은 파일을 쓴다. 글자·색·바탕은 T006에서 코드로 정한다.
- [X] T005 [P] [US1] `app/src/main/java/kr/skku/unusedfeature/Screens.kt`: `fun Ui.history(records: List<Record>, learned: Set<Feature>, onOpen: (Int) -> Unit, onClose: () -> Unit): View`를 만든다. 글자와 크기는 contracts/ui.md §2를 따른다.
  - 머리 줄은 `small("지난 안내")`, `headline("추천 기록", 44f)`, 닫기 칩 "✕  닫기"다. 구역 카드 `Ui.zone()`의 머리 줄과 같은 모양이다.
  - `records`(최신이 앞)를 `day(at)`이 바뀔 때마다 `small(day(at))` 날짜 머리로 묶는다.
  - 항목은 `card(row(24, tile(f.icon, 익힘이면 Tile.GREEN 아니면 Tile.BLUE, 96), column(8, row(12, 이름, 시각, 칩), body(이유, 24f)), grow = 1))`다.
    - 이름은 `text(f.title, 30f, R.color.text1, 700)`이다.
    - 시각은 `text(SimpleDateFormat("a h:mm", Locale.KOREA).format(at), 22f, R.color.text3)`이다.
    - 칩은 익힘일 때만 `chip("써 봄 ✓", R.color.green_text, R.color.green_soft)`이다.
    - 카드 전체를 누르면 `onOpen(그 기록의 번호)`를 부른다.
  - 기록이 없으면 `hero(tile(R.drawable.ic_clock, Tile.GREY), "아직 추천받은 기능이 없어요", "운전이 끝나 P에 놓으면 기능 하나를 알려 드리고, 여기에 남겨요.")`를 보인다.
- [X] T006 [US1] `app/src/main/java/kr/skku/unusedfeature/MainActivity.kt`:
  - `private var openHistory: Int? = null`(null 닫힘, -1 목록)을 더하고, `State`에 `HISTORY`를 더한다. 우선순위는 `DRIVING` 다음, `ZONE` 앞이다.
  - `State.HISTORY -> ui.history(progress.history(), learned, onOpen = { openHistory = it; render() }, onClose = { openHistory = null; render() })`
  - `toggleZone`에서 구역을 열 때 `openHistory = null`로 한다(FR-008).
  - `render()`에서 `historyButton`의 글자를 정한다. 기록이 있으면 "추천 기록 {n}  ›", 없으면 "추천 기록  ›"다. 색은 파랑 글자에 `blue_soft` 둥근 바탕, 22sp, 굵기 600이다.
  - 버튼을 누르면 `openHistory = -1; openZone = null; render()`를 한다. 목록이나 다시 보기가 열려 있어도 같다. 늘 목록으로 가고, 버튼으로는 닫지 않는다(FR-006).

**Checkpoint**: 목록을 열고 닫을 수 있고, 기록이 최신부터 보인다.

---

## Phase 4: User Story 2 - 그때 안내를 다시 본다 (Priority: P1)

**Goal**: 목록 항목을 누르면 그때의 안내 화면(근거 타임라인 + 누를 곳)을 다시 본다.

**Independent Test**: quickstart 시나리오 3, 5(공조 AUTO 다시 보기에 근거 3개·AUTO 강조·선택 링, 익힌 뒤 "써 봄 ✓ 날짜").

- [X] T007 [US2] `app/src/main/java/kr/skku/unusedfeature/Screens.kt`: `Ui.tip()`에 마지막 매개변수 `title: View = recent(driveMs)`를 더하고, 본문에서 `recent(driveMs)` 대신 `title`을 쓴다. 기존 호출(`MainActivity`의 TIP)은 그대로 둔다.
- [X] T008 [US2] `app/src/main/java/kr/skku/unusedfeature/MainActivity.kt`:
  - `State`에 `RECORD`를 더한다. 우선순위는 `DRIVING` 다음, `HISTORY` 앞이다. `openHistory`가 0 이상이고 `progress.history()` 범위 안일 때만 `RECORD`이고, 범위 밖이면 `HISTORY`로 본다.
  - 화면은 `ui.tip(r.pick, r.reason, r.marks, r.driveMs, banner = 위쪽 줄, title = ui.small("${moment(r.at)} 운전 · ${r.driveMs / MINUTE}분"))`다.
  - 위쪽 줄은 `chip("←  추천 기록", R.color.text2, R.color.chip)`이고, 누르면 `openHistory = -1; render()`다. 익힘이면 초록 칩 "써 봄 ✓ {day(learnedAt)}"를 붙인다. `learnedAt`이 0이면 "써 봄 ✓"만 쓴다.
  - 지도는 `CarMapView.set(spots, pulse = null, sparkle = null, selected = r.pick.zone, dim = false)`다.

**Checkpoint**: 목록 → 다시 보기 → 목록을 오갈 수 있고, 다시 보기 화면이 그 운전 직후 안내 화면과 같다.

---

## Phase 5: User Story 3 - 운전 중에는 기록이 닫힌다 (Priority: P1)

**Goal**: 기어가 P가 아니면 기록 페이지가 닫히고 버튼이 막힌다(원칙 I).

**Independent Test**: quickstart 시나리오 6(기록 페이지를 연 채 기어 N → 덤프에 "운전 중이에요" → P → 상황 화면).

- [X] T009 [US3] `app/src/main/java/kr/skku/unusedfeature/MainActivity.kt`:
  - `render()` 머리의 닫기 규칙(`if (WatchService.driving || progress.drives() != drivesSeen) openZone = null`)에서 `openHistory`도 함께 `null`로 한다.
  - `historyButton.isEnabled = !WatchService.driving`로 하고, 주행 중이면 `alpha = 0.4f`(아니면 1f)로 한다. 비활성 뷰는 누르기를 받지 않으므로 처리기 안에서 따로 막지 않는다. 화면 덤프에는 `enabled="false"`로 나온다.

**Checkpoint**: 기어가 P가 아닐 때 기록 페이지가 보이는 경우가 없다.

---

## Phase 6: Polish & 검증

- [X] T010 에뮬레이터에서 quickstart 시나리오 1~10을 실행한다. 목록과 다시 보기 화면을 `docs/screenshots/10-history-list.png`, `docs/screenshots/11-history-record.png`로 저장한다. `logcat -b crash`가 0건인지 확인한다.
- [X] T011 [P] `specs/003-recommendation-history/verification.md`: 테스트 결과, 시나리오별 결과와 근거, 확인하지 못한 것을 적는다.
- [X] T012 [P] `README.md`: 첫 단락과 "문서"에 추천 기록 페이지를 더하고, 화면 캡처 목록에 10·11번을 더한다.

- [X] T013 리뷰 반영(verification.md "리뷰 반영"). ponytail-review는 지적이 없었다. /code-review high 9건 중 5건을 `app/src/main/java/kr/skku/unusedfeature/MainActivity.kt`와 `Screens.kt`에 반영했다.
  - 목록 위치 되돌리기와 같은 화면이면 스크롤 유지(`listY`, `prev`)
  - `onPause`에서 페이지 닫기
  - 연도가 붙는 날짜(`dayLabel`, `timeOfDay`)
  - 버튼 처리기에서도 주행 중 확인(T009의 비활성 처리와 함께)
  - 날짜 글자를 기록마다 한 번만 만들기

---

## Dependencies & Execution Order

- 순서는 Phase 2 → US1(Phase 3) → US2(Phase 4) → US3(Phase 5) → Phase 6이다.
- T002는 T001 뒤, T003은 T002 뒤다.
- US1·US2·US3는 모두 `MainActivity.kt`를 고치므로 T006 → T008 → T009 차례로 한다. `Screens.kt`를 고치는 T005와 T007도 차례로 한다.
- 세 이야기 모두 P1이다. US3(주행 중 닫힘)는 원칙 I 때문에 시연·제출 전에 반드시 들어가야 한다.

## Parallel Example

```text
Phase 3: T004(map_head.xml)와 T005(Screens.kt)를 동시에 — 서로 다른 파일
Phase 6: T011과 T012를 동시에
```

## Implementation Strategy

1. Phase 2: 기록이 쌓이는지 테스트와 기록 파일로 먼저 확인한다.
2. US1: 목록만으로도 "지난 안내를 다시 본다"의 절반이 된다(MVP).
3. US2: 다시 보기로 "어디를 누르나"와 "왜"에 답한다.
4. US3: 주행 중 닫힘. 작지만 원칙 I이므로 빠뜨리지 않는다.
5. Phase 6: 에뮬레이터 검증과 문서.
