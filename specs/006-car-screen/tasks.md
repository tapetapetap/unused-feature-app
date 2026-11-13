# Tasks: 안 써본 기능 6차 — 차 화면 개선

**Input**: `specs/006-car-screen/`의 spec.md, plan.md, research.md, contracts/ui.md, quickstart.md
**작업 위치**: `/Users/jiwon/orca/workspaces/unused-feature-app/car-unused`(브랜치 `car-unused`). 경로는 이 폴더 기준이다. 코드 폴더는 `app/src/main/java/kr/skku/unusedfeature/`(아래 `src/`로 줄임).
**Tests**: 근거 카드 숫자 규칙만 새 단위 테스트(헌법 IV). 화면은 에뮬레이터로 확인한다.

## Phase 1: Setup

- [X] T001 브랜치가 `car-unused`이고 태그 `backup/before-car-unused`가 main 74f28fa인지 확인하고(`git -C . rev-parse backup/before-car-unused`), 바꾸기 전 `./gradlew -q :app:testDebugUnitTest`가 통과하는지 본다(JAVA_HOME은 quickstart §1)

## Phase 2: Foundational (모든 이야기의 바탕)

- [X] T002 근거 카드 숫자 규칙 테스트를 먼저 쓴다: `app/src/test/java/kr/skku/unusedfeature/CoachTest.kt`에 `@Test fun evidenceCountRules()` — 순간만 2개 → "2번", 구간만(3분+5분) → "8분", 구간 1개 + 순간 1개 → "". `Mark(kind, at, end, text)`로 만든다. 테스트를 돌려 `evidenceCount`가 없어 실패하는 것을 확인한다
- [X] T003 [P] 색 토큰(contracts §3): `app/src/main/res/values/colors.xml`에 `text3` #FF62656D, `green_text` #FF017A40, 새 `blue_text` #FF0050D8. `app/src/main/res/values-night/colors.xml`에 새 `blue_text` #FF6CB4FF(`text3`, `green_text`는 그대로)
- [X] T004 `src/Screens.kt`의 `Ui` 도우미(contracts §1): `small` 24sp, `chip` 24sp·여백 20·10·22·10·아이콘 26dp, 새 `action(label, fg, bg, onClick)`(= `chip` + `minimumHeight`·`minimumWidth`·`minHeight`·`minWidth` 76dp, `gravity = CENTER`, 가로 여백 28, `setOnClickListener`), `cardTitle` 28/24sp, `where` 누를 버튼 24sp·나머지 22sp, `banner` 24sp

**Checkpoint**: 앱 빌드(`:app:assembleDebug`)는 되고, 단위 테스트는 `evidenceCount`가 없어 테스트 컴파일에서 실패한다(T002 확인).

## Phase 3: User Story 1 - 차에서 읽고 누를 수 있다 (P1) 🎯 MVP

**Goal**: 화면 글자 24sp 이상, 카드 제목 28sp 이상, 누르는 칸 76dp 이상(spec FR-001~004).
**Independent Test**: quickstart §2(UI 덤프와 캡처), §3(세로 칸).

- [X] T005 [US1] `src/Screens.kt` 화면별 고정 글자 20·22sp → 24sp(contracts §2): `firstRun` 단계 설명, `driving` 기다림 칩 글자, `caption` 작은 제목, `whereCard` "언제" 줄, `confirm` "처음 쓴 순간" 28sp·아래 줄 24sp, `none` 맨 아래 안내, `complete` 칸 제목·날짜, `history` 카드 시각, `zone` "언제 쓰면 좋아요"
- [X] T006 [US1] `src/Screens.kt`: `history`·`zone`의 "✕  닫기"를 `action(...)`으로 바꾸고, 머리 줄 `row(20, …)`의 간격을 24로, `history`의 바깥 `column(20, …)`을 `column(24, …)`로(기록 카드 사이 24dp). `Ui.grid`는 세로 화면(`ctx.resources.configuration.orientation == ORIENTATION_PORTRAIT`)이면 `columnCount = minOf(columns, 2)`
- [X] T007 [US1] `src/MainActivity.kt`: `recordScreen`의 "←  추천 기록"을 `ui.action(...)`으로, `legend` 글자 24sp, `zoneRow` 숫자 24sp·`minimumHeight = ui.px(76)`, 세로 구역 목록의 `lp(bottom = 12)`를 `lp(bottom = 24)`로
- [X] T008 [P] [US1] `app/src/main/res/layout/map_head.xml`: `mapSub` 24sp, `historyButton` 24sp·`android:minHeight="76dp"`·`android:minWidth="76dp"`·`android:gravity="center"`. `src/MainActivity.kt` `onCreate`의 historyButton 여백을 가로 24·세로 0으로
- [X] T009 [P] [US1] `src/Views.kt`: `TimelineView` `label`·`strong` 22sp(글자가 겹치면 줄 간격 `dp(32f)` → `dp(36f)`), `CarMapView` `label`·`strong` 22sp와 `drawLabel`의 상자 높이 `dp(40f)`. `drawLabel`의 밀어 넣기를 양쪽으로: `box.offset(maxOf(0f, -box.left) + minOf(0f, width - box.right), 0f)`. `CarMapView`에 `var labels = true`를 두고 `false`면 `drawLabel`을 부르지 않는다. `src/MainActivity.kt` `onCreate`에서 세로 화면이면 `map.labels = false`, `map.onZone = null`(누르기 끔, 지금 `onTouchEvent`가 `onZone == null`이면 받지 않음), 가로면 지금처럼 `::toggleZone`(contracts §2, 사용자 결정)

**Checkpoint**: quickstart §2의 3·5(누르는 칸 크기·간격)와 §3의 3·5(누를 곳 그림·구역 줄·지도 이름표·칩 2열)가 통과한다. 글자 크기 코드 검사(§2의 4)와 문구 확인은 Phase 7에서 한다.

## Phase 4: User Story 2 - 낮·밤 모두 또렷하다 (P1)

**Goal**: 40sp 미만 글자 대비 4.5:1 이상(FR-005).
**Independent Test**: quickstart §2의 5와 낮·밤 캡처.

- [X] T010 [US2] `src/Screens.kt`: `whereCard`와 `zone`의 "버튼 위치는 차종마다 조금 다를 수 있어요."를 24sp `R.color.text3`으로, `zone` 상태 칩 SHOWN의 글자색을 `R.color.blue_text`로
- [X] T011 [US2] `src/Screens.kt`: `firstRun` 3단계 번호 원을 `green_soft` 바탕·`green_text` 숫자로(1·2단계 그대로), `confirm`의 새로 익힌 칩을 `chip(..., R.color.green_text, R.color.green_soft, icon)`으로 만들고 `background = round(c(R.color.green_soft), 100f, c(R.color.green))`(1.5dp 초록 테두리)
- [X] T012 [P] [US2] `src/MainActivity.kt` historyButton 글자색 `R.color.blue_text`. `src/Views.kt` `TimelineView.strong.color`와 `CarMapView.drawLabel`의 강조 글자색을 `R.color.blue_text`로(막대·점·테두리 색은 blue 그대로)

**Checkpoint**: 낮·밤 캡처에서 각주·칩·버튼 글자가 또렷하다.

## Phase 5: User Story 3 - 보던 칸을 잃지 않는다 (P2)

**Goal**: 뒤로 버튼과 설정 변화(FR-006·007, contracts §5).
**Independent Test**: quickstart §4 표.

- [X] T013 [US3] `src/MainActivity.kt`: `@Deprecated("Deprecated in Java") override fun onBackPressed()` — `openHistory ?: -1 >= 0`이면 `openHistory = -1`(render 뒤 `listY`로 스크롤, 지금 목록 복귀와 같은 방식), `openHistory != null || openZone != null`이면 둘 다 `null`, 아니면 `super.onBackPressed()`
- [X] T014 [US3] `src/MainActivity.kt`: `onSaveInstanceState`에 `zone`(이름), `history`, `listY`, `scrollY`(`R.id.scroll`), `drivesSeen`을 넣고, `onCreate`에서 `savedInstanceState`로 되살린다(구역은 `Zone.valueOf`, 없으면 null). 되살린 `scrollY`는 첫 `render` 뒤 한 번 `scroll.post { scrollTo(0, y) }`
- [X] T015 [US3] `src/MainActivity.kt` `onPause`: `if (!isChangingConfigurations) { openZone = null; openHistory = null }`

**Checkpoint**: quickstart §4가 모두 기대대로다.

## Phase 6: User Story 4 - 문구와 숫자가 서로 맞는다 (P2)

**Goal**: FR-008~011.
**Independent Test**: quickstart §5.

- [X] T016 [US4] `src/Coach.kt`에 순수 함수 `fun evidenceCount(marks: List<Mark>): String`(research R3, contracts §4)을 더하고 T002 테스트를 통과시킨다
- [X] T017 [US4] `src/Screens.kt` `tip`: 근거 카드 오른쪽 숫자를 `evidenceCount(marks)`로 바꾼다(지금의 `points`·`minutes` 식 삭제)
- [X] T018 [US4] `src/Screens.kt` `driving`: 기다림 본문(기록하는 운전) "쓸 만한 기능이 생기면 짧게 알려 드릴게요."와 칩 "한 운전에 두 번까지만 알려요"
- [X] T019 [US4] `src/Feature.kt` `FRONT_DEFROST`의 evidenceTitle "비 온 구간과 내기 순환". `src/Coach.kt` `Tracker.recirc`의 표시 글자 "내기 순환 켬"/"외기로 바꿈". 디버그 샘플 `app/src/debug/java/kr/skku/unusedfeature/Seed.kt`의 `point(Kind.RECIRC, 4, "내기 순환")`도 "내기 순환 켬"으로(시연 기록이 새 글자를 보이게). `app/src/test/java/kr/skku/unusedfeature/CoachTest.kt`의 `recirculationToggles`에 `assertEquals(listOf("내기 순환 켬", "외기로 바꿈"), s.marks.filter { it.kind == Kind.RECIRC }.map { it.text })`를 더한다. `CarWatcher.kt`의 `transient("내기 순환")`·`unsupported("내기 순환")`은 신호 이름이라 그대로 둔다
- [X] T020 [US4] 지원 기능 0개 구역: `src/Views.kt` `drawLabel`이 `s.total == 0`이면 "${z.title} · 기능 없음", `src/MainActivity.kt` `zoneRow`의 오른쪽 글이 숫자 대신 "기능 없음", `src/Screens.kt` `zone`의 익힘 줄이 `total == 0`이면 "이 차에서는 쓸 수 있는 기능이 없어요"

**Checkpoint**: T002 테스트 통과, quickstart §5 확인.

T018(Screens.kt)과 T019(Feature.kt·Coach.kt·Seed.kt)는 서로 다른 파일이라 T016·T017 뒤에 둘끼리는 동시에 할 수 있다.

## Phase 7: Polish & 검증

- [X] T021 빌드·테스트·설치(quickstart §1). 기존 테스트 전부와 `evidenceCountRules` 통과
- [X] T022 에뮬레이터 확인 quickstart §2~5(§2의 4 글자 크기 코드 검사, §3의 5-1 칩 격자 포함). 캡처를 `/Users/jiwon/Desktop/현대자동차 공모전/화면 점검 1113/고친 뒤/안 써본 기능/`에 두고, `specs/006-car-screen/verification.md`를 만들어 UI 덤프 크기·간격표를 적는다
- [ ] T023 장면 회귀(quickstart §6): `climate`, `wiper`, `rain`, `highway`, `heat`, `multi`, `live`가 5차 verification과 같은 안내를 고른다
- [X] T024 `/impeccable audit`와 `/impeccable critique`를 다시 돌려 점수와 남은 문제를 verification.md에 적고, 남은 P1이 있으면 `/impeccable polish`로 다듬는다(contracts 범위 안)
- [X] T025 ponytail-review(diff `git diff backup/before-car-unused -- app`)를 돌려 줄일 것을 반영한다
- [X] T026 `/code-review`(high)를 돌려 정확성 문제를 반영하고, 보안 직접 점검(새 권한·외부 전송·로그에 개인 정보가 없는지)을 verification.md에 적는다
- [X] T027 `specs/006-car-screen/verification.md`(T022에서 만든 파일)를 마저 채운다: SC-001~006 결과, 캡처 경로, 점수 전후, 리뷰 반영
- [ ] T028 로컬 커밋(브랜치 `car-unused`, 푸시하지 않음). main에 합치기와 푸시는 사용자가 정한다

## Dependencies & Execution Order

- Phase 1 → Phase 2 → 이야기들. T003·T004가 끝나야 US1·US2를 시작한다.
- US1(T005~T009)과 US2(T010~T012)는 같은 파일(Screens.kt, MainActivity.kt, Views.kt)을 고쳐 차례로 한다. US3(MainActivity.kt)과 US4는 US1·US2 뒤.
- T016 → T017. T002가 T016보다 먼저다.
- Phase 7은 모든 이야기 뒤.

### Parallel Opportunities

- T003(색 XML)은 T002·T004와 동시에 할 수 있다.
- T008(map_head.xml)·T009(Views.kt)는 T005~T007과 다른 파일이다.
- T018·T019는 서로 다른 파일이다(T016·T017 뒤).

## Implementation Strategy

- MVP는 Phase 1~3(글자와 누르는 칸)이다. 점검 P1의 가장 큰 몫이고 세 앱 공통 도우미 변경의 본보기다.
- 같은편(`/Users/jiwon/orca/workspaces/unused-feature-app/car-sameside/specs/006-car-screen/`)과 DRIVE DNA(`/Users/jiwon/orca/workspaces/unused-feature-app/car-dna/specs/004-car-screen/`)가 같은 도우미 변경을 따른다. 이 앱에서 값이 바뀌면(예: 줄 간격) 그 앱들의 계약도 같이 고친다.
