# Goldfish Review — 안 써본 기능 2차 — 보이는 화면과 기능 18개

- 날짜: 2026-11-08
- 리뷰어 상태: comprehension 완료 · readiness GO
  - 구현이 끝난 뒤 실행했다. CLAUDE.md 2-0 단계가 이 기능의 2-1 게이트 뒤에 생겼고, 리뷰어 유형도 처음에는 이 세션에 등록되어 있지 않았다.
  - readiness는 사용량 한도로 한 번 중단되었고, 같은 리뷰어를 이어서 실행해 끝까지 받았다.

## Comprehension의 다시 쓰기

### 다시 쓰기

Pleos(현대차 AAOS) 차량 앱 "안 써본 기능"의 2차 작업이다. 이 앱은 운전(P → 다른 기어 → P)이 끝날 때마다, 이번 운전에서 쓸 기회가 있었는데 쓰지 않은 차 기능 하나를 음성·알림·화면으로 권한다.
2차는 권할 기능을 6개에서 18개로 늘린다. 이를 위해 새 차 신호를 구독하고, 와이퍼 스위치를 3초마다 읽고, 주행거리로 시속 70km 이상 구간을 추정한다. 6개뿐이면 몇 번 운전한 뒤에는 더 권할 것이 없기 때문이다.
사용자가 "무엇이 바뀌었는지 안 보인다"고 한 문제도 함께 푼다. 안내 화면에 이번 운전의 근거 타임라인과 누를 버튼 그림을 넣고, 위에서 본 차 지도(7구역)와 진행 원으로 익힌 기능(도감)을 보여 준다. 상황 화면은 Pleos 가이드에 맞춰 라이트·다크, 가로·세로 모두로 다시 만든다.
시연용 디버그 빌드는 앱 시계를 10배 빠르게 해서 1분·5분 같은 시간 조건을 짧은 시연 안에 채운다. 처음 계획은 21개였지만, 표시등을 읽으려면 내부 전용 권한이 필요해 오토 홀드·차로 유지 보조·i-PEDAL을 뺐다.

### 기존 시스템

문서가 "1차"라고 부르는 현재 동작은 다음과 같다.
- 부팅 때 자동으로 시작하는 백그라운드 서비스가 Pleos Vehicle SDK로 기어와 차 신호를 구독한다.
- P → 다른 기어 → P를 운전 한 번으로 보고, 1분이 안 되는 운전은 버린다.
- 운전 중에는 기능 6개의 기회 조건과 사용 여부를 기능별 Boolean(`autoOn` 등)으로 센다. 6개 가운데 002 문서에서 확인되는 것은 공조 AUTO, 오토 라이트(전조등 수동 토글 2번 이상), 크루즈(20분 이상 운전) 셋뿐이고, 나머지 셋은 적혀 있지 않다. 차속 신호가 없어서 크루즈는 운전 시간만으로 판단한다.
- P가 되면 후보를 고른다. 후보는 신호를 받은 적이 있고, 기회가 있었고, 이번 운전에 쓰지 않았고, 익히지 않았고, 안내가 3번 미만인 기능이다. 그중 안내 횟수가 가장 적은 것을 고르고, 같으면 정해진 순서를 따른다.
- 고른 기능은 TTS와 시스템 알림("안 써본 기능: {기능}", 누르면 앱)으로 알린다. 안내했던 기능을 나중에 쓰면 "써 보셨네요" 확인을 보인다. 주행 중에는 음성도 알림도 없다.
- 기록은 기기 안 SharedPreferences "progress"(`shown_`·`learned_`·`last_pick`·`last_confirmed`·`drove`)에 둔다. 권한은 CAR_INFO·DRIVE·CLIMATE·SEATS·LIGHTS 조회 권한이다.
- 화면은 Activity + View로 되어 있다. 하지만 1차 화면이 무엇을 보여 줬는지는 002 문서에 없고, 사용자가 "무엇이 바뀌었는지 안 보인다"고 했다는 것만 나온다.
- 002 Status가 Implemented라서, 계획과 data-model은 앞으로 할 설계라기보다 이미 구현한 결과의 기록으로 읽힌다.

## 지적과 처리

| # | 리뷰어 | 심각도 | 위치 | 요지 | 처리 | 근거·변경 |
|---|---|---|---|---|---|---|
| 1 | comprehension | Major | spec.md › FR-001·User Story 3, data-model.md › Progress | "익힘"의 정의가 없다. 안내받은 뒤 써야 익힘인지, 정차 중이나 구독 직후 값도 익힘인지 두 가지로 읽힌다 | 수정 | 1차 FR-006("사용 신호를 받으면 익힘")과 FR-001 "1차 규칙 유지"로 이미 정한 내용이다. spec Key Entities에 "익힘" 정의를 추가하고 data-model `learned_<id>` 비고를 보강했다. 코드 확인: `WatchService.onChange`의 `tracker.usedNow() - progress.learned()` |
| 2 | comprehension | Major | spec.md › Edge Cases·FR-001 | "1차" 기준 문서의 경로가 없다. "운전 도중 앱을 열면" 문장이 서비스가 운전 도중 시작된 경우와 섞여 있다 | 수정 | spec 머리에 "1차 기준 문서"(001 spec·data-model)를 추가했다. Edge Case는 두 경우로 나눠 결과를 적었다. 근거: 1차 Edge Case "그 운전의 기록은 버림", `CarWatcher.kt` onGear 주석, `MainActivity` 상태 판단. 이 경우 화면을 바꿀지는 #17에서 묻는다 |
| 3 | comprehension | Major | spec.md › FR-002·Status | 18개 기능 표를 기능 폴더 밖의 21개 기준 설계 문서에 맡겨, 기준 표가 둘이다 | 수정 | 대화에서 정한 21→18 조정이다(표시등 읽기에 PRIVATE_CAR_SAFETY 필요). FR-002가 data-model Feature 표를 가리키게 하고, 뺀 3개와 이유를 Status에 직접 적었다 |
| 4 | comprehension | Major | spec.md › FR-007, plan.md › Constitution Check II, data-model.md › 화면 상태 | 진행 원 분모인 "지원 기능"의 정의가 문서마다 다르다 | 수정 | 2차 /code-review 2·5번에서 정한 내용이다. 능력 확인 실패, 대상 자리 없음, 스위치에 AUTO 없음만 빼고 일시 실패는 로그만 남긴다. data-model에 "지원 기능" 정의와 경우별 표를 두고, FR-007·plan을 같은 문장으로 맞췄다. 에뮬레이터 수도 지금 코드 기준 14개로 고쳤다. 이전에 적힌 "16개"는 사실과 다르다(화면이 0/14) |
| 5 | comprehension | Major | spec.md › User Story 2·SC-005 | P 뒤에 앱이 스스로 앞으로 뜨는지, 열려 있을 때만 화면이 바뀌는지 모호하다 | 수정 | 1차에서 정한 내용이다(음성·알림, 알림을 누르면 앱: contracts §6). `WatchService.onDriveEnd`는 음성·알림·`onUpdate`만 하고 액티비티를 띄우지 않는다. US2와 SC-005에 "열려 있으면 바뀌고, 아니면 알림"을 적었다 |
| 6 | comprehension | Major | spec.md › Assumptions·Edge Cases·User Story 4 | 주행 중 화면은 D에서 Pleos가 가리는데, Assumptions는 "신호 대기나 시연에서 보이는 화면"이라고 한다. 누가 언제 보는지, US4·SC-006에서 어떻게 확인하는지 없다 | 수정(사용자 결정) | 사용자: "추천대로"(N에서 확인하고, 가려지면 확인 대상에서 뺌). 에뮬레이터 확인: N도 Pleos가 가리고, R은 후방 카메라가 덮는다. Assumptions·Edge Case에 그 사실을, US4 Independent Test·SC-006에 "주행 중 화면은 빼고 UI 트리 덤프로 글자 확인"을, quickstart 화면 확인 2에 덤프 방법을 적었다 |
| 7 | comprehension | Major | data-model.md › DriveStats, spec.md › User Story 1 AS1 | 횟수 필드마다 무엇을 1로 세는지, "손으로"의 조건, 첫 값 제외 여부, heard·used의 범위가 없다 | 수정 | 근거는 코드와 2차 /code-review 4번 결정(전조등·하이빔 스위치도 3초마다 읽음)이다. 코드: `Coach.kt` Tracker의 "첫 값은 기준점", light()·highBeam()은 스위치가 AUTO 아님으로 확인된 때만 셈, startDrive() 뒤 refresh(). data-model에 세는 법·필드 표·heard/used 정의를 적고, plan 핵심 설계 4와 AS1 문장을 고쳤다 |
| 8 | comprehension | Nit | spec.md › Edge Cases·FR-004, plan.md › Constitution Check III·핵심 설계 2, contracts/ui.md › 3 | 21개 설계의 흔적이 남아 있다(차로 유지 보조, 근거 "정차", "21개 버튼 그림", "오토 홀드를", LFA와 차로 유지 보조 이름 혼용) | 기록 | — |
| 9 | comprehension | Nit | spec.md › Input·FR-009·SC-006, plan.md › Scale/Scope | 상황 화면 수가 8, 6, 7로 문서마다 다르다 | 기록 | — |
| 10 | comprehension | Nit | spec.md › FR-005 | 버튼이 "3~4개"라고 했지만 contracts에는 2개짜리 기능이 있다 | 기록 | — |
| 11 | comprehension | Nit | research.md › R11 | 세로 화면 확인 방법이 quickstart와 반대다 | 기록 | — |
| 12 | comprehension | Nit | spec.md › User Story 2, data-model.md › Feature | 지도의 "구역"과 누를 곳의 "자리"가 어떤 관계인지 없다 | 기록 | — |
| 13 | readiness | Major | tasks.md › 제목, T001·T004·T011·T013·T014·T022·T023·Implementation Strategy | 작업 본문이 아직 21개 기준이다(CAR_SAFETY, 브레이크·표시등·i-PEDAL, `getTelltalesInfo`, "21 −", 없는 기능 알림 줄, 저장소에 없는 `ic_steer`) | 수정 | 대화에서 정한 21→18 조정이다. 본문을 18개 기준으로 고쳐 썼다(T003·T012·T016·T027과 Phase 목적·목표 포함). T004는 `Screens.kt`의 `ic_z_wheel`을, T023은 contracts §5를 따른다. 구현 메모는 이력으로 남겼다 |
| 14 | readiness | Major | tasks.md › Phase 3 Independent Test | 없는 시나리오 `demo.sh hold`(빠진 기능)를 가리킨다. 실행하면 기본 분기(공조)가 돌아 엉뚱한 결과로 판정하게 된다 | 수정 | `scripts/demo.sh`에 `hold`가 없고 Feature에도 오토 홀드가 없다. spec US1 Independent Test에 맞춰 `demo.sh wiper` → "오토 와이퍼" 음성(logcat)으로 바꿨다 |
| 15 | readiness | Major | data-model.md › 화면 상태 "지원 기능", plan › Constitution Check II, spec › FR-007 | 지원 기능 정의가 문서마다 다르고, 어떤 실패를 세는지 정하지 않았다 | 수정 | #4와 같은 변경이다. 경우별 표에 구독 호출 예외(로그만 남김, `CarWatcher.start`)와 통풍 능력 확인 실패(빼지 않음)를 넣었다 |
| 16 | readiness | Major | data-model.md › Mark, contracts/ui.md › 2 | 순간 근거를 언제 남기는지, 운전 시작 때 켜져 있던 상태를 0분 근거로 남기는지 없다 | 수정 | 근거는 코드와 2차 /code-review 10번 결정이다. 코드: `Coach.kt` startDrive의 "이미 켜져 있는 상태는 근거도 0분에", `temp()`의 21°·17° 기준, `ac()`. data-model Mark에 종류별 "남기는 때·운전 시작 때" 표를 추가했다 |
| 17 | readiness | Major | spec.md › Edge Cases, data-model.md › 화면 상태 1. DRIVING | 서비스가 운전 도중 시작되면 기어가 P가 아닌데도 일반 화면이 나오고 구역을 누를 수 있다(FR-010, 원칙 I) | 수정(사용자 결정) | 사용자: "추천대로"(P가 아니면 늘 주행 중 화면, 기록하지 않는 운전이면 아랫줄을 바꿈). FR-010·Edge Case·data-model DRIVING·contracts §3을 고치고 코드에 반영했다: `CarWatcher.notParked`, `WatchService.driving`/`recording`, `Screens.driving(recording)`. N 기어 UI 트리 덤프로 두 문구를 확인했다 |
| 18 | readiness | Major | contracts/ui.md › 5. 알림 줄 | 연결 실패 문구가 코드(1차 문구 + 예외 메시지)와 다르다. 지우는 쪽은 접두어로 비교한다 | 수정 | 계약 문구가 /plan에서 정한 설계다. 코드를 계약에 맞췄다: `NO_SIGNAL` 상수(`CarWatcher.kt`)를 넣는 쪽과 지우는 쪽(`WatchService.onChange`)이 함께 쓰고, T023에 적었다. 테스트 41개 통과, `assembleDebug` 성공. 에뮬레이터 설치는 아직 안 했다 |
| 19 | readiness | Major | quickstart.md › 시나리오 `wiper` | 기대 결과 "근거 3개"가 실제 4개와 다르다 | 수정 | `scripts/demo.sh` 51–52행이 OFF→LO→HI→LO→OFF로 바꾸고, `Coach.wiperSwitch()`는 OFF↔LO도 센다. 캡처 `03-tip-auto-wiper.png`도 "4번"이다. 조작과 기대를 "근거 4개"로 고쳤다 |
| 20 | readiness | Major | tasks.md › Phase 1(T001–T006), T010, T011 | 확인 절차가 없다(Phase 1 Checkpoint, T006 안전 영역, T010 Progress). T011이 요구한 시속 70km 경계 테스트도 없다 | 수정 | T011에 정한 경계 테스트가 빠진 것은 사실 오류다. `CoachTest.highwayBoundaryIs70`(70km는 고속, 69km는 아님)을 추가했고 41개 모두 통과했다. Phase 1 Checkpoint(빌드, 다크 캡처, 아이콘 전경 좌표 계산: 중심에서 29.3 이내 < 반지름 33)와 T010 확인 절차(progress.xml 키)를 적었다. T010은 `climate`까지 확인했다(drives=1, first_drive, shown_AUTO_CLIMATE=1). `learn` 뒤 값은 아직 확인하지 못했다 |
| 21 | readiness | Nit | data-model.md › DriveStats, plan › 핵심 설계 3 | `ventOn`이 코드에 없다. 고속 구간은 Span이 아니라 따로 계산한다 | 기록 | — |
| 22 | readiness | Nit | data-model.md › 고속 구간 계산 | 운전 시작 때 직전 주행거리를 0ms 표본으로 넣는다는 규칙이 없다 | 기록 | — |
| 23 | readiness | Nit | contracts/ui.md › 3. 상황 화면 글자 | 코드에만 있는 문구가 있다(주행 모드 "바꿔 보세요", 구간뿐일 때 "{m}분", "없어요", "운전을 시작하자마자") | 기록 | — |
| 24 | readiness | Nit | data-model.md › Mark "최대 60개" | 근거가 60개를 넘으면 "{n}번"과 이유 문장의 숫자가 어긋난다 | 기록 | — |
| 25 | readiness | Nit | quickstart.md › 화면 확인 4·6, research › R11 | 탭 좌표와 taskId를 얻는 방법이 없다. R11과도 서로 모순된다 | 기록 | — |

## 집계

지적 25개 중 문서를 바꾼 것 15개
