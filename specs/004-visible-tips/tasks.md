# Tasks: 안 써본 기능 4차 — 음성 자막, 기록 카드 지도, 눈에 띄는 추천 구역

**Input**: `specs/004-visible-tips/`의 spec.md, plan.md, research.md, data-model.md, contracts/ui.md, quickstart.md
**Prerequisites**: 1~3차 완료(main 3b723b4, `CoachTest` 45개)

**Tests**: 헌법 IV에 따라 순수 코드(`Seed`)는 `app/src/test/java/kr/skku/unusedfeature/SeedTest.kt`(새)를 먼저 쓴다. 판단은 바꾸지 않으므로 `CoachTest` 45개가 그대로 통과해야 한다.

**경로 기준**: 저장소 루트 `/Users/jiwon/Desktop/현대자동차 공모전/unused-feature-app`. 빌드는 `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`.

## Format: `[ID] [P?] [Story] Description`

---

## Phase 1: Setup

- [X] T001 `./gradlew :app:testDebugUnitTest`가 45개 통과하는지 확인한다.

---

## Phase 2: User Story 1 - 들은 안내를 화면에서도 읽는다 (Priority: P1) 🎯 MVP

- [X] T002 [US1] `app/src/main/java/kr/skku/unusedfeature/WatchService.kt`를 고친다(data-model Spoken, research R1).
  - `speak(text): Boolean`(준비됨 + 예외 없음이면 true)
  - companion `lastSpoken`, `onDriveEnd`에서 `Spoken(pick, uptimeMillis, speak(pick.speech))`
- [X] T003 [US1] `app/src/main/java/kr/skku/unusedfeature/Views.kt`에 `SoundBars`를 더한다. `set(until)`, 값은 contracts/ui.md 표다. 움직일 때만 `postInvalidateOnAnimation`, 아니면 정지 모습이다.
- [X] T004 [US1] 자막 카드를 넣는다(contracts/ui.md).
  - `app/src/main/res/drawable/ic_speaker.xml`(새, Material Icons volume_up, Apache 2.0, 24dp 벡터)
  - `app/src/main/java/kr/skku/unusedfeature/Screens.kt`: `Ui.caption(f, title, until)`, `tip(…, caption: View)`의 hero 다음 자리
  - `MainActivity.kt`: TIP은 `caption(pick, "방금 들은 음성", until)`, `recordScreen`은 `caption(r.pick, "그때 들은 음성", 0)`
  - 확인·없음 화면은 그대로 둔다.

---

## Phase 3: User Story 2 - 추천 기록에서 기능 위치를 그림으로 본다 (Priority: P1)

- [X] T005 [US2] `Views.kt`에 `ZoneThumb(feature, state: RowState)`를 더한다(research R2). `car_top`, 점·링, 익힘 `green`·아직 `blue`·지원 안 함 `dot_line`이다.
- [X] T006 [US2] `Screens.kt`의 `history(records, learned, unsupported, onOpen, onClose)` 카드 오른쪽에 `ZoneThumb`(72×128dp)를 넣는다. 상태는 익힘 > 지원 안 함 > 아직이다. `MainActivity`가 `WatchService.unsupported`를 넘긴다.

---

## Phase 4: User Story 3 - 새 안내의 구역이 먼저 눈에 들어온다 (Priority: P2)

- [X] T007 [US3] `Views.kt`의 `CarMapView.set`에 `spotlightUntil`(벽시계, 기본 0)을 더한다(research R3, contracts/ui.md "차 지도 강조").
  - 움직임 켜짐: 1.2초 주기 링 r → 3.4r
  - 움직임 줄이기: r + 6dp 고정 원
  - 이름표 굵기 800(두 번째 Paint)
  - 강조 중이면 `postInvalidateDelayed(spotlightUntil − now)`
- [X] T008 [US3] `MainActivity.kt`에서 TIP이면 `spotlightUntil = lastSpoken.at + 10_000`(uptime)을 넘긴다(data-model, 구현 중 바뀜).

---

## Phase 5: User Story 4 - 시연을 기록이 있는 상태에서 시작한다 (Priority: P3)

- [X] T009 [P] [US4] `app/src/testDebug/java/kr/skku/unusedfeature/SeedTest.kt`를 쓴다(data-model 불변식 여섯 개). 시간대는 `Asia/Seoul`과 `UTC` 두 번 본다.
- [X] T010 [US4] `app/src/debug/java/kr/skku/unusedfeature/Seed.kt`(새, 순수, 디버그 소스셋)에 `Seed.records(now, zone)`, `Seed.learned`, `Seed.learnedAt(records)`를 쓴다(data-model 표). T009가 통과해야 한다.
- [X] T011 [US4] 디버그 빌드 전용 수신기를 더한다. 대상은 `app/src/debug/AndroidManifest.xml`과 `app/src/debug/java/kr/skku/unusedfeature/SeedReceiver.kt`다.
  - `progress`를 비우고 data-model 키 표대로 쓴다(`commit()`).
  - `exported="true"`, `permission="android.permission.DUMP"`(research R4)
- [X] T012 [US4] `scripts/demo.sh`에 `seed`를 더한다: 차 상태 되돌리기(`reset`과 같은 신호) → force-stop → broadcast → start. 머리 주석에 한 줄 적는다.

---

## Phase 6: Polish & 검증

- [X] T013 단위 테스트(45 + Seed), `assembleDebug`, 설치. 병합 매니페스트로 출시 빌드에 `SeedReceiver`가 없고 디버그에 있는지 본다(quickstart).
- [X] T014 에뮬레이터: quickstart 표대로 한다. `reset` → `climate`(2초·15초 캡처, screenrecord로 막대), `learn`, `seed`(첫 화면·추천 기록 캡처), 기록 열기, 움직임 줄이기. 1~3차 장면 결과가 같은지 본다(SC-001~004). 캡처는 `docs/screenshots/` 12번부터 둔다.
- [X] T015 ponytail-review로 과설계를 점검하고 반영한다.
- [X] T016 /code-review(high)로 정확성을 점검하고 반영한다.
- [X] T017 보안을 직접 점검한다: 디버그 수신기 노출 범위(디버그 빌드만, DUMP 보호), 저장 값.
- [X] T018 문서를 고친다.
  - `README.md`·`사용설명서.md`에 4차 기능과 `seed`
  - `specs/004-visible-tips/verification.md`
  - `approval-gate.md`의 "승인 뒤 구현 중 바뀐 것"
- [X] T019 저장소 밖: "안 써본 기능 주행 영상" 아티팩트(https://claude.ai/artifact/VjEiCZ5kRJwx4woxe2fMzc)를 갱신한다.
  - 장면: 지금 페이지의 14장면 + `seed`
  - 녹화: `adb shell screenrecord`(1280×720) → H.264 mp4(15fps)
  - 자막: WebVTT. 앱이 말한 문장(logcat `doSpeak(kr.skku.unusedfeature) text=…`)을 말한 시각에 둔다.
  - 확인: 페이지에서 모든 영상이 재생되고 자막 트랙이 켜져 있다. `climate` 영상의 P 화면에 자막 카드, 움직이는 막대, 10초 링이 보인다.

## Dependencies & Execution Order

- US1(자막) → US2(미니 지도) → US3(강조) → US4(샘플)는 서로 독립이다. 다만 `Views.kt`·`Screens.kt`·`MainActivity.kt`를 함께 고치므로 순서대로 한다.
- Phase 6은 모두 끝난 뒤 한다.

## Implementation Strategy

- MVP는 US1(자막)이다. 같은편 요청의 "켜졌는지 알기 어려움"에 바로 대응한다.
