# Tasks: 안 써본 기능

**Input**: Design documents from `/specs/001-unused-feature-coach/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/ui.md, quickstart.md

**Tests**: 원칙 IV에 따라 판단 로직(`Coach.kt`) JUnit 테스트를 포함합니다. 테스트는 구현보다 먼저 쓰고 실패를 확인합니다.

## Format: `[ID] [P?] [Story] Description`

## Phase 1: Setup

- [X] T001 Gradle 프로젝트 골격: `settings.gradle.kts`(Pleos Maven 포함), `build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, Gradle wrapper 8.11.1
- [X] T002 `app/build.gradle.kts`: AGP 8.8.2, compileSdk 35, minSdk 28, targetSdk 34, JDK 17, 의존성(Vehicle 2.3.2, vehicle-data 1.1.11, TextToSpeech 3.3.1, core-ktx, junit)
- [X] T003 `app/src/main/AndroidManifest.xml`: automotive feature, 권한(R2), 포그라운드 서비스 specialUse, BOOT_COMPLETED, POST_NOTIFICATIONS, Activity
- [X] T004 [P] `.gitignore`, `app/src/main/res/values/strings.xml`(앱 이름), `res/drawable/ic_notify.xml`

## Phase 2: Foundational

- [X] T005 `app/src/test/java/kr/skku/unusedfeature/CoachTest.kt`: DriveStats 집계 규칙(첫 값은 변화 아님, AUTO 켜진 동안 수동 변화 미집계), 기능 6개 기회·사용 조건, 고르기 규칙(최소 안내 횟수 → 표 순서, 익힘·3회 제외, 이번 운전 사용 제외), 새 익힘 확인 문장, 1분 미만 무시 — 먼저 작성하고 실패 확인
- [X] T006 `app/src/main/java/kr/skku/unusedfeature/Coach.kt`: `Feature` enum(문장은 contracts/ui.md), `DriveStats`, `Coach.finish(stats, progress) → Outcome` — T005 통과
- [X] T007 `app/src/main/java/kr/skku/unusedfeature/Progress.kt`: SharedPreferences로 shown/learned/last 카드 읽기·쓰기

## Phase 3: User Story 1 - 운전이 끝나면 놓친 기능 하나를 듣는다 (P1) 🎯 MVP

**Goal**: 기어 P 전환 시 놓친 기능 하나를 음성·알림·카드로 알린다.
**Independent Test**: quickstart 3단계 1~3.

- [X] T008 [US1] `CarWatcher.kt`: `Vehicle` 초기화, 기어 구독으로 운전 시작·끝 판정, 공조·시트·운전대·전조등·크루즈 구독을 DriveStats에 반영, `release()`
- [X] T009 [US1] `WatchService.kt`: 포그라운드 시작, CarWatcher 연결, 운전 끝에 `Coach.finish` → Progress 저장 → Pleos TTS 발화 → 알림 → 화면 갱신 브로드캐스트
- [X] T010 [US1] `BootReceiver.kt`: BOOT_COMPLETED에서 WatchService 시작

## Phase 4: User Story 2 - 써 본 기능은 다시 듣지 않는다 (P2)

**Goal**: 사용 신호 → 익힘, 안내했던 기능이면 확인 문장.
**Independent Test**: quickstart 3단계 4~5.

- [X] T011 [US2] `WatchService.kt`: 정차 중 사용 신호도 즉시 익힘으로 저장(CarWatcher 사용 콜백), 운전 끝 Outcome의 익힘 확인을 카드에 저장

## Phase 5: User Story 3 - 내 차 기능 현황을 본다 (P3)

**Goal**: 마지막 카드 + 기능 6개 상태.
**Independent Test**: quickstart 3단계 1.

- [X] T012 [US3] `res/layout/activity_main.xml` + `MainActivity.kt`: 카드(빈 상태 문구 포함), 목록, 상태 줄, 서비스 시작, 알림 권한 요청, 갱신 브로드캐스트 수신

## Phase 6: Polish

- [X] T013 `./gradlew :app:testDebugUnitTest :app:assembleDebug` 통과
- [X] T014 에뮬레이터에서 quickstart 시연 확인(가능한 범위), 결과를 `specs/001-unused-feature-coach/verification.md`에 기록
- [X] T015 [P] `README.md`: 빌드·실행·시연 방법, 한계

## Dependencies

- T001 → T002 → T003 → (T005 → T006) → T007 → T008 → T009 → T010/T011 → T012 → T013 → T014
- T004, T015는 병렬 가능.
- US2는 US1의 서비스 위에 얹히고, US3 화면은 Progress만 읽으므로 US1 없이도 보입니다.

## Parallel Example

```text
T004 (리소스) 와 T005 (테스트 작성) 은 서로 다른 파일이라 동시에 진행 가능
```

## Implementation Strategy

MVP = Phase 1~3. 여기까지 되면 "P에서 한 기능 안내"가 시연됩니다. 이어서 US2(익힘), US3(현황 화면) 순서.
