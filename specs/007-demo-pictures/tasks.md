# Tasks: 안 써본 기능 7차 — 시연용 그림 바로잡기

**Input**: `specs/007-demo-pictures/`의 spec.md, plan.md, research.md, quickstart.md
**작업 위치**: `/Users/jiwon/orca/workspaces/unused-feature-app/car-unused`(브랜치 `car-unused`, 되돌리기 태그 `backup/before-pictures-unused` = af01903). 경로는 이 폴더 기준이다. 빌드는 `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`.
**Tests**: 새 테스트를 먼저 쓰고 실패를 본 뒤 고친다(헌법 IV). 바꾸는 파일은 둘뿐이다: `app/src/testDebug/java/kr/skku/unusedfeature/SeedTest.kt`, `app/src/debug/java/kr/skku/unusedfeature/Seed.kt`.

## Phase 1: Setup

- [X] T001 브랜치가 `car-unused`이고 `git rev-parse --short backup/before-pictures-unused`가 af01903인지, 바꾸기 전 `./gradlew -q :app:testDebugUnitTest`가 56개 통과하는지 본다(결과 `app/build/test-results/testDebugUnitTest/`)

## Phase 2: Foundational — 테스트 먼저 (US2의 테스트, US1보다 먼저)

- [X] T002 `app/src/testDebug/java/kr/skku/unusedfeature/SeedTest.kt`에 `@Test fun picturesMatchTheSentences()`를 더한다(research R3 표). `Seed.records(now, TimeZone.getTimeZone("Asia/Seoul"))`의 기록마다 근거 표시에서 센 값과 `Seed.stats(r.pick)` 값을 글로 만들어 기능 → 글 map 둘을 `assertEquals` 한 번으로 견주고, 표에 없는 기능이면 실패한다. 세는 도우미는 이 테스트 안에만 둔다. `app/src/test`에는 두지 않는다(출시용 단위 테스트 컴파일이 깨진다)
- [X] T003 `./gradlew :app:testDebugUnitTest --tests kr.skku.unusedfeature.SeedTest`를 돌려 `picturesMatchTheSentences`만 실패하고, `app/build/test-results/testDebugUnitTest/TEST-kr.skku.unusedfeature.SeedTest.xml`의 실패 메시지에서 FRONT_DEFROST만 다른지(문장 8분, 센 값 11분) 본다. 다른 기록도 다르면 세는 법을 research R3와 대조해 고친다

**Checkpoint**: 새 테스트가 김서림 하나로 실패한다(spec US2-2).

## Phase 3: User Story 1 - 김서림 샘플 그림이 문장과 맞는다 (P1) 🎯 MVP

**Goal**: FR-001·002. **Independent Test**: Phase 5 캡처.

- [X] T004 [US1] `app/src/debug/java/kr/skku/unusedfeature/Seed.kt`의 `items`에서 FRONT_DEFROST 항목(28~29행) 근거 `listOf(...)` 끝에 `point(Kind.RECIRC, 12, "외기로 바꿈")`을 더한다. `{ rainRecircMillis = min(8) }`, 비 구간, 4분 점, 다른 항목은 그대로 둔다

## Phase 4: User Story 2 - 샘플 기록마다 테스트가 맞춰 본다 (P2)

- [X] T005 [US2] `./gradlew :app:testDebugUnitTest :app:assembleDebug` — 57개(56 + 새 1) 통과, `app/build/outputs/apk/debug/app-debug.apk` 생성. `git diff --stat backup/before-pictures-unused -- app/src/main`이 비어 있다(SC-002·003)

## Phase 5: 에뮬레이터 확인 — 다른 앱과 차례로 쓴다

에뮬레이터 단계는 메인 세션이 세 앱을 차례로(같은편 → DRIVE DNA → 안 써본 기능) 돌리고, 셋이 다 끝난 뒤 한 번 끈다(2-1 결정).

에뮬레이터 하나를 세 앱이 함께 쓴다. 같은편·DRIVE DNA 작업이 쓰는 중이면 끝난 뒤에 쓴다. Pleos 2560×1440, density 160(1dp = 1px), adb `$HOME/Library/Android/sdk/platform-tools/adb`, 앱 사용자 `--user 10`, 부팅 뒤 `adb forward tcp:8088 tcp:8088`(quickstart §2). 캡처는 `/Users/jiwon/Desktop/현대자동차 공모전/화면 점검 1115/안 써본 기능/`에 둔다.

- [X] T006 [US1] `adb install -r --user 10 app/build/outputs/apk/debug/app-debug.apk` → `scripts/demo.sh seed` → "추천 기록 6"으로 시작하는 버튼 → "앞유리 김서림 제거"(UI 덤프 bounds로 누름) → 근거 카드까지 밀고 가로 캡처 `01-김서림-가로.png`(quickstart §2의 1·2)
- [X] T007 [US1] 화면 나누기 `adb shell input swipe 2547 720 1500 720 800` → 같은 다시 보기를 1:1 세로로 캡처 `02-김서림-세로.png` → 되돌리기 `adb shell input swipe 1706 720 2550 720 800`(quickstart §2의 3·4)
- [X] T008 [US1] 두 캡처로 quickstart §3을 본다(SC-001, FR-004 회귀). impeccable audit는 돌리지 않고 이 확인으로 갈음한다(research R4: 화면 코드 `app/src/main`이 그대로이고, 생길 수 있는 문제는 점 글자 겹침뿐). §3에 맞지 않으면 그리기나 샘플 값을 고치지 말고 캡처를 붙여 보고하고 멈춘다(2-1 결정). 끝나면 `adb shell rm -f /sdcard/u.xml`(에뮬레이터는 메인 세션이 마지막에 끈다)

## Phase 6: Polish & 리뷰

- [X] T009 ponytail-review(diff `git diff backup/before-pictures-unused -- app`, 대상 `Seed.kt`·`SeedTest.kt`)를 돌려 줄일 것을 반영한다
- [X] T010 `/code-review`(high)를 돌려 `Seed.kt`·`SeedTest.kt` 변경의 정확성 문제를 반영한다
- [X] T010b T009·T010에서 반영한 것이 있으면 `./gradlew :app:testDebugUnitTest :app:assembleDebug`(57개 통과)와 `git diff --stat backup/before-pictures-unused -- app/src/main`(빈 출력)을 다시 본다. `Seed.kt` 근거 값이 바뀌었으면 T006~T008도 다시 한다(2-1 결정)
- [X] T011 `/security-review`를 돌린다. 새 권한·외부 전송·내보낸 구성 요소가 없는지(`app/src/debug/AndroidManifest.xml` 그대로) 본다
- [X] T012 `specs/007-demo-pictures/verification.md`를 쓴다: 테스트 전후(T003 실패 메시지, T005 57개), 출시 앱 diff 0, 캡처 경로와 §3 결과, audit 갈음 근거, 리뷰 반영
- [X] T013 로컬 커밋(브랜치 `car-unused`, 푸시하지 않음): `app/src/debug/java/kr/skku/unusedfeature/Seed.kt`, `app/src/testDebug/java/kr/skku/unusedfeature/SeedTest.kt`, `specs/007-demo-pictures/`, `.specify/feature.json`, 설계 `docs/superpowers/specs/2026-11-14-demo-pictures-design.md`(6차 f662d08과 같은 묶음). main 합치기와 푸시는 사용자가 정한다

## Dependencies & Execution Order

- T001 → T002 → T003 → T004 → T005 → T006 → T007 → T008 → T009 → T010 → T011 → T012 → T013, 모두 차례로 한다.
- 테스트(T002·T003)가 고치기(T004)보다 먼저다. 고치기 전 어긋남을 잡는지 먼저 본다.
- 병렬 작업은 없다. 파일이 둘이고 다음 일이 앞 결과에 기댄다. 에뮬레이터(T006~T008)는 다른 앱과 차례를 맞춘다.

## Implementation Strategy

- MVP는 T004(김서림 그림)다. 제출 마감 2026-11-15 23:59 전에 T013까지 끝낸다.
