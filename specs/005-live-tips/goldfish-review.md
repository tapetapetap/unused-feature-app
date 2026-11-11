# Goldfish Review — 안 써본 기능 5차 — 주행 중 한마디(주행 최적화)

- 날짜: 2026-11-12
- 리뷰어 상태: comprehension 완료 · readiness GO(첫 실행 NO-GO → 재리뷰 GO)

## Comprehension의 다시 쓰기

## 다시 쓰기
"안 써본 기능"은 Pleos 차량 앱이다. 지금은 운전이 끝나 P가 되면, 그 운전에서 쓸 기회가 있었는데 쓰지 않은 차 기능 하나를 알려 준다.
5차는 필요한 순간에 바로 알게 하는 것이 목적이다. 앱을 주행 최적화(DO)로 선언하고, 운전자가 앱을 내비 옆 작은 칸에 띄워 두면, 손 닿는 기능 16개 가운데 기회가 생긴 기능을 그 자리에서 권한다. 권함은 움직이지 않는 카드 하나와 한 문장 음성이다.
운전을 방해하지 않도록 한 운전에 2번까지, 5분 간격으로만 권하고, 방향지시등이 켜진 동안은 미루며, 화면은 누르기를 받지 않는다. 권한 기능을 쓰면 카드가 말없이 "써 봤어요"로 바뀐다.
P 안내는 이번 운전에서 권한 기능을 빼고 고른다. 권함도 안내 횟수에 세지만 추천 기록에는 남기지 않는다. 이를 위해 헌법 원칙 I을 고친다. Pleos가 DO 선언을 받아들이지 않으면 카드는 빼고 음성만 남긴다.
4차 상태는 main과 백업 태그로 남겨 되돌릴 수 있게 한다.

## 기존 시스템
이 문서에 따르면 지금 시스템은 이렇게 동작한다.
- **운전 기록**: 백그라운드 서비스가 차 신호를 받아 P를 벗어난 때부터 다시 P가 될 때까지를 한 운전으로 기록한다. N이나 R도 운전 중으로 본다. 앱이 운전 도중에 시작되면 그 운전은 기록하지 않는다.
- **P 안내**: 기능 18개마다 1~2차에서 정한 "기회" 조건이 있다(예: 와이퍼를 손으로 3번 바꿈). P가 되면 기회가 있었는데 쓰지 않은 기능 하나를 음성과 화면으로 안내한다.
  - 1분 미만 운전에는 안내하지 않는다.
  - 운전 시작 때 이미 켜져 있던 기능은 쓴 것으로 본다.
  - 익힌 기능과 안내를 3번 받은 기능은 뺀다.
  - 안내 횟수가 적은 기능을 먼저 고르고, 같으면 기능 표 순서를 따른다.
- **기록**: 안내는 추천 기록에 남고 안내 횟수가 1 는다. 도감과 확인 화면도 있지만, 문서는 무엇인지 설명하지 않는다.
- **주행 중**: 고정된 "운전 중이에요" 화면만 보이고 말하지 않는다(헌법 원칙 I "정차 중에만 말한다"). DO를 선언하지 않았으므로 D에서는 Pleos가 앱을 가린다.
- **신호 처리**: 스위치는 운전 중 3초마다 읽는다. 비, 비와 내기 순환이 겹친 시간, 전조등 시간은 진행 중인 구간을 누적값에 넣지 않는다(FR-008에서 추론).
- **시연**: 디버그 빌드는 앱 시계가 10배 빠르고, scripts/demo.sh 장면으로 시연한다.

## 지적과 처리

| # | 리뷰어 | 심각도 | 위치 | 요지 | 처리 | 근거·변경 |
|---|---|---|---|---|---|---|
| 1 | comprehension | Major | spec 1~4차와의 관계, FR-016 | P에서 권한 기능을 빼면 1~4차 장면 P가 대개 "권할 것 없음"이 되는데 문서에 드러나지 않음 | 수정(사용자 결정) | 위임 결정(사용자 2026-11-12 "추천안으로 끝까지 진행"): P는 그대로 고르고, 권한 기능이 뽑히면 화면만·말 없음(FR-012, data-model §5, 설계 D5). spec에 장면별 4차/5차 표 |
| 2 | comprehension | Major | spec FR-016·US5-1·SC-006, quickstart P | "확인" 정의 없음, P 기대값이 확인 규칙과 어긋남 | 수정(사용자 결정) | #1과 같은 결정. 용어에 확인 정의, US4-4·US5-1·SC-006·quickstart P 기대값 하나로(시트 열선 화면 + 오토 와이퍼 확인 띠) |
| 3 | comprehension | Major | contracts §1 자리, FR-003·SC-004 | 작은 칸 전체 구성(지도 칸)과 스크롤 여부 없음 | 수정(사용자 결정) | 위임 결정: 세로 배치 주행 중 지도 칸 숨김, 여백 40·타일 120(research R8, contracts §1, FR-003 "스크롤 없이") |
| 4 | comprehension | Major | US2-1·FR-011·SC-002, contracts §6 | "2초 안"의 기준이 두 가지로 읽힘, 3초 읽기 사실 누락 | 수정 | CarWatcher.kt `POLL_MILLIS = 3_000L`: 스위치 조작 기준 5초, 사용 신호 기준 2초로 나눠 씀(FR-011, SC-002, research R5 영향) |
| 5 | comprehension | Major | US4-2, 1~4차와의 관계, FR-005 | 도감·익힘 정의 없음, US4-2 확인 화면 특정 불가 | 수정 | MainActivity.kt `zoneScreen`의 "안내함 n회"/"익힘": 용어에 도감·익힘 정의, US4-3을 "정차 중 시트 구역 카드 '안내함 1회'"로 |
| 6 | comprehension | Major | FR-008, FR-004 | 세 누적값의 까닭과 크루즈(운전 20분) 누락 | 수정 | Feature.kt CRUISE `driveMillis >= 20 * MINUTE`, Coach.kt `endDrive`: data-model §1 후보 16개 조건 표, FR-008·tick에 운전 시간, 밤 시간 제외 |
| 7 | comprehension | Major | research R1, plan Constraints | "같은편" 정의 없음, 관찰 대상이 이 앱인지 불분명 | 수정 | 용어에 같은편, R1에 "같은편 로그" 명시(이후 T001로 이 앱도 확인) |
| 8 | comprehension | Major | FR-016, plan 구조 | 주행 중 말하지 않는다는 기존 문구를 바꾸는지 범위에 없음 | 수정 | Screens.kt 208·219·310행, WatchService.kt 알림, 매니페스트 FGS 문구: contracts §7 표와 FR-016 |
| 9 | comprehension | Nit | FR-009, contracts §3 | 40자에 공백을 세는지 없음(앞유리 문장 42자) | 수정 | 사실 오류(42자): 공백 포함으로 정하고 앞유리 문장을 39자로 줄임 |
| 10 | comprehension | Nit | FR-006, data-model §3 | "기능 표 순서"가 어느 표인지 | 기록 | #9를 고치며 contracts §3에 "표 순서 = 열거 순서 = 우선순위" 한 줄을 함께 넣음 |
| 11 | comprehension | Nit | 용어, FR-005·012 | "안내 횟수"가 권함도 셈 | 기록 | — |
| 12 | comprehension | Nit | Key Entities, data-model §2 | "권한 뒤 썼는지" 속성이 data-model에 없음 | 수정 | 문서 사이 불일치: spec Key Entities를 "익힘으로 판단"으로 |
| 13 | comprehension | Nit | SC-005, data-model §3 | 여덟째 조건은 순수 함수 밖 | 수정 | data-model §3 "부르는 쪽": SC-005를 일곱 조건 + 여덟째는 코드 리뷰·시연으로 |
| 14 | readiness | Blocker | FR-011·SC-002, T014·T018 | 3초 스위치 읽기로는 2초 안 "써 봤어요" 불가 | 수정 | CarWatcher.kt `POLL_MILLIS`, 002 research R1(와이퍼 동작 신호 없음): 조작 기준 5초, T014·contracts §6도 5초 |
| 15 | readiness | Major | T018, contracts §6-4 | 콘솔에 INT 없음 → 바꿈 2번뿐 | 수정 | 002 research R1 `front_wiper=Off|Auto|Low|High`: Off→Low→High→Low |
| 16 | readiness | Major | T018, contracts §6-3 | 주행거리 주입 미정, 크루즈가 먼저 뽑힐 수 있음 | 수정 | Feature.kt 열거 순서 CRUISE → SEAT_HEAT: live 장면에서 주행거리 빼고 시간표를 계약에 적음 |
| 17 | readiness | Major | FR-016, quickstart §3, T003·T019 | countShown 때문에 권한 뒤 쓴 기능이 P 확인이 됨 | 수정(사용자 결정) | #1·#2와 같은 결정: 확인을 살리고 T003에 확인 테스트, SC-006 기대값 |
| 18 | readiness | Major | data-model §4, T006 | tick이 driveMillis를 안 써 크루즈(거리 없는 차) 불가 | 수정 | #6과 같음. T002에 "21분 tick → CRUISE" 테스트 |
| 19 | readiness | Major | contracts §1 | 작은 칸에서 버튼 줄이 화면 밖 | 수정(사용자 결정) | #3과 같은 결정(research R8) |
| 20 | readiness | Major | T008 | 방향지시등 구독 확인 절차 없음 | 수정 | T008에 로그 확인, T019·contracts §6에 Left 켬/끔 확인과 콘솔 명령(DRIVE DNA demo.py `turn-signal?mode=`) |
| 21 | readiness | Major | T021 | 기존 사용자 문구 수정 작업과 README 자리 없음 | 수정 | Screens.kt 208행, 사용설명서 17·50행, README 77·96행: contracts §7, T013·T021 |
| 22 | readiness | Major | FR-002·SC-002 ↔ contracts §1 | "써 봤어요" 형식이 문서마다 다름 | 수정 | 두 줄 형식으로 통일(FR-002, SC-002, T014) |
| 23 | readiness | Major | research R2, T001·T018 | 작은 칸이 안 될 때 분기 없음 | 수정 | FR-001이 전체 칸 허용: R2·T001·T018에 전체 칸 분기 |
| 24 | readiness | Major | T016 | 비교 영역·판정 기준 없음 | 수정 | uiautomator 앱 루트 bounds, `getbbox() is None` |
| 25 | readiness | Nit | contracts §3 | 앞유리 문장 42자 | 수정 | #9와 같음 |
| 26 | readiness | Nit | plan Technical Context | 테스트 48 = 45 + Seed 3 | 수정 | CoachTest.kt 45, testDebug SeedTest.kt 3 |
| 27 | readiness | Nit | research R5 | FR-001 → SC-001 | 수정 | 참조 번호 |
| 28 | readiness | Nit | T010 | `learn(newly)` → `learn(newly, note)` | 수정 | Progress.kt 시그니처 |
| 29 | readiness(재리뷰) | Major | T008, research R3 | 방향지시등 API 세부와 꺼짐 값 불일치 | 수정 | Vehicle-2.3.2.aar javap: register/unregister, `onTurnSignalStateUpdated(TurnSignalValue?)`, 값 OFF·LEFT·RIGHT, null 무시(R3·T008) |
| 30 | readiness(재리뷰) | Major | FR-014 분기, T011, contracts §7 | 분기 때 문구 작업도 빠짐, 판정 기준 없음 | 수정 | 문구를 T013으로 옮겨 분기와 무관하게, R1에 판정 기준. T001 결과 받아들임이라 분기 없음 |
| 31 | readiness(재리뷰) | Major | contracts §7 | 사용설명서 52·130·131행, README 96행 빠짐 | 수정 | 사용설명서.md 52·130·131행: §7 표에 세 줄과 새 그림 |
| 32 | readiness(재리뷰) | Major | spec 장면 표 `rain` | 권함이 있으면 P는 뒷유리 열선(말함) | 수정 | Coach.kt `.minByOrNull(count)`, data-model §7: `rain` 행을 두 경우로 |
| 33 | readiness(재리뷰) | Major | T017 | 지난 `Spoken`이 남아 말 안 한 P에 자막 | 수정 | MainActivity.kt 93·125행, WatchService.kt 122행: 말 없는 분기에서 `lastSpoken = null` |
| 34 | readiness(재리뷰) | Major | T009·T017·T019 | 횟수·US4-3·US1-3·FR-003 확인 없음, T020에 learn·highbeam 없음 | 수정 | T019 확인 셋 더함, T020 장면 둘 더함 |
| 35 | readiness(재리뷰) | Major | T014 | 버튼 줄 확인 시점이 "써 봤어요" 뒤 | 수정 | contracts §1(써 봤어요에는 버튼 줄 없음): 권함 상태에서 확인 |
| 36 | readiness(재리뷰) | Major | T002 | rainRecirc tick 테스트 없음, 앰비언트 테스트가 무력 | 수정 | data-model §1(밤 시간 운전 중 안 채움): 테스트 둘 고침 |
| 37 | readiness(재리뷰) | Major | contracts §1 | 써 봤어요 타일 크기·간격 없음 | 수정 | Screens.kt `column(32, …)`: 간격 32, 타일 150/120 |
| 38 | readiness(재리뷰) | Nit | plan Project Structure | R1~R7 → R8 | 수정 | research.md R8 |
| 39 | readiness(재리뷰) | Nit | plan Summary, contracts §1 | `Ui.driving` 인자 셋 | 수정 | tasks T011·T012 |
| 40 | readiness(재리뷰) | Nit | FR-009·FR-013 | "한 문장"인데 문구는 두 문장 | 수정 | contracts §3: "문구 하나(두 문장까지)"로 |

## 재리뷰

- 대상: readiness. 첫 VERDICT NO-GO(Blocker #14 수정) → 재리뷰 VERDICT GO
- 새 지적: #29~#40(Blocker 없음). `재지적` 행 없음.

## 집계

지적 40개 중 문서를 바꾼 것 38개
