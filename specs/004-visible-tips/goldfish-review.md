# Goldfish Review — 안 써본 기능 4차 — 음성 자막, 기록 카드 지도, 눈에 띄는 추천 구역

- 날짜: 2026-11-11
- 리뷰어 상태: comprehension 완료 · readiness GO

## Comprehension의 다시 쓰기

### 다시 쓰기
"안 써본 기능"은 차량용 안드로이드 앱이다. 운전을 마치고 P에 놓으면, 그 운전에서 쓸 만했지만 쓰지 않은 차 기능 하나를 음성과 화면으로 권한다. 4차는 어떤 기능을 권할지 정하는 판단은 그대로 두고 표현만 더한다.
(1) 안내 화면 제목 영역 아래에, 말한 문장(`Feature.speech`)을 스피커 아이콘·소리 막대와 함께 자막으로 보인다. 막대는 말한 뒤 6초만 움직인다. 기록 다시 보기에서는 막대가 멈춘 채 "그때 들은 음성"으로 보인다. (2) 추천 기록 카드마다 위에서 본 작은 차 그림을 두고, 그 기능 구역에 상태색 점을 찍는다(익힘 초록, 아직 파랑, 지원 안 함 회색). (3) 새 안내가 뜨면 10초 동안 차 지도의 추천 구역에 넓은 링을 퍼뜨리고 이름표를 굵게 한다.
(4) 디버그 빌드에만 있는 수신기와 `demo.sh seed`로 상태를 만들어 두고 시연을 시작한다. 3일치 기록 6개, 익힘 2개, 운전 6번이 들어간다.
목적은 세 가지다. 소리를 못 들었거나 무음 영상에서도 안내를 알아채게 하고, 기록에서 기능 위치를 바로 보게 하고, 시연을 빈 화면이 아닌 상태에서 시작한다. 다만 출발점이라는 "같은편 요청"이 무엇인지는 문서로 알 수 없다.

### 기존 시스템
- **안내 판단과 음성**: `WatchService`가 운전을 지켜본다. P가 되면 판단(`Coach`: 어떤 기능을 권할지, 익힘, 안내 횟수)이 기능 하나를 고른다. `onDriveEnd`에서 그 기능의 `Feature.speech` 한 문장을 Pleos TTS로 말하고, TTS가 준비되지 않았으면 말하지 않는다. 판단은 `CoachTest` 45개로 검증한다.
- **화면 상태**: 안내, 확인("써 보셨네요"), 권할 것 없음, 주행 중, 도감 완성, 추천 기록 목록과 다시 보기가 있다. 안내 화면에는 첫 카드와 "여기를 누르세요" 카드가 있고, 눌러야 할 버튼은 이미 파랗게 깜빡인다. 운전이 시작되면 주행 화면으로 바뀐다.
- **차 지도**: 위에서 본 차와 구역을 그리고, 추천 구역에는 1.8초 주기의 작은 맥박이 있다. 추천 기록 카드에는 지도가 없고 익힌 기능에 "써 봄 ✓"만 붙는다.
- **저장**: 상태는 기기 안 SharedPreferences `progress`에 있다. 기록은 `History.encode` 형식이고 제어 문자 `\u001E`·`\u001F`가 들어간다. `MainActivity`는 `drivesSeen`으로 새 운전 결과를 알아챈다.
- **시연**: `scripts/demo.sh`의 장면(`climate`, `learn`, `reset` 등)이 Virtual Console로 차 신호를 넣는다. 디버그 빌드는 앱 시계가 10배 빠르다.
- 지금은 자막, 기록 카드 지도, 강조 링, 샘플 상태가 모두 없다.

## 지적과 처리

| # | 리뷰어 | 심각도 | 위치 | 요지 | 처리 | 근거·변경 |
|---|---|---|---|---|---|---|
| 1 | comprehension | Major | spec.md Input·US·Assumptions, quickstart | 같은편·첫 앱·다른 두 앱이 정의되지 않음 | 수정 | 리뷰 전 사용자 요청(2026-11-11: 같은편 요청 다섯 가지 + "나머지 두 앱도 비슷하게")에서 정한 것. 명세 머리에 용어와 요청-스토리 표, quickstart에 두 패키지 이름 |
| 2 | comprehension | Major | spec.md US3·FR-006, research R3 | "새 안내"가 언제인지 두 가지로 읽힘(drivesSeen 첫 render) | 수정(사용자 결정) | V1: 가장 최근 기록이 생긴 지 10초 안. US3 AS4, FR-006, R3, data-model |
| 3 | comprehension | Major | spec.md FR-008 | 익힘 2개 중 "하나는 기록에 있는 기능"이 계획과 다름 | 수정 | research R4 "둘 다 기록에 있음". FR-008을 공조 AUTO·시트 열선으로 |
| 4 | comprehension | Major | spec.md US4, quickstart seed | seed 뒤 첫 화면 기대가 없고 "도감"이 정의되지 않음 | 수정(사용자 결정) | V2: 마지막 기록 앰비언트 → 첫 화면 앰비언트 안내. US4 AS2, "도감" → 진행 원 정의 |
| 5 | comprehension | Major | research R4 | 수신기가 쓰는 키가 와일드카드뿐 | 수정(사용자 결정) | V2: data-model `progress` 키 표 |
| 6 | comprehension | Major | spec FR-005, research R2 | 회색이라 하고 `dot_idle`(밝은 테마 흰색)을 씀 | 수정 | colors.xml:29-30 확인. `dot_line`으로 |
| 7 | comprehension | Major | spec US3 AS1·FR-006, research R3 | "두 배 반지름"이 두 가지로 읽힘 | 수정 | research R3 `r × (1 + 2.4t)`, Views.kt 맥박 `r × (1 + 1.2t)`. r → 3.4r로 수치화 |
| 8 | comprehension | Nit | spec US1·FR-001 | "첫 카드"는 카드가 아닌 hero | 수정 | Screens.kt `hero`는 줄(row). "제목 영역(hero)"으로 |
| 9 | comprehension | Nit | spec FR-007 | 괄호가 막대에도 걸려 읽힘 | 기록 | #18 처리로 FR-007을 다시 쓰며 함께 풀림 |
| 10 | comprehension | Nit | spec FR-002 | "말한 뒤 6초" 기준이 R1(말하기 직전)과 다름 | 수정 | research R1과 맞춰 "말하기 시작한 때부터" |
| 11 | comprehension | Nit | research R1·R4 | "11/10 녹화"의 위치가 없음 | 기록 | #13·#19 처리로 R1·R4를 다시 쓰며 빠짐(로그 줄 형식으로 대체) |
| 12 | comprehension | Nit | spec Assumptions | D1~D4가 볼 수 없는 문서 번호 | 수정 | #1의 요청 표로 바꿈 |
| 13 | readiness | Major | tasks T003·T004, data-model Spoken | 6초 뒤 정지 방법이 없고 시계가 섞임, tip 인자 미정 | 수정(사용자 결정) | V3: `speak(): Boolean`, `Spoken.at` uptime, `caption(f, title, until)`, `SoundBars.set(until)`, `tip(…, caption)`. quickstart에 screenrecord |
| 14 | readiness | Major | contracts TIP, tasks T004 | 스피커 아이콘 리소스가 없음 | 수정 | drawable 폴더에 없음 확인. T004·plan에 `ic_speaker.xml`(새) |
| 15 | readiness | Major | tasks T003~T007, contracts | 막대·자막·링·이름표 그리기 값이 비어 있음 | 수정(사용자 결정) | V3: contracts/ui.md 표(막대 5/4dp, 900ms, 위상 1/3, 정지 40/70/50%, text3, 여백 24/18dp, 링 선 2dp, 굵기 800) |
| 16 | readiness | Major | research R2, tasks T005·T006 | 회색 색값, state 형, 우선순위, history 인자 미정 | 수정(사용자 결정) | colors.xml 사실 확인 + V4: `RowState` 재사용, 익힘 > 지원 안 함 > 아직, `history(…, unsupported, …)` |
| 17 | readiness | Major | research R3, tasks T008 | 강조를 켜는 경계(drivesSeen -1)와 시각 보관 자리 미정 | 수정(사용자 결정) | V1: 기록 시각 + 10초(벽시계)를 render마다 계산. Activity 필드 없음 |
| 18 | readiness | Major | research R3, FR-007, quickstart | 움직임 줄이기의 모양·끝·검증 없음 | 수정(사용자 결정) | V5: r + 6dp·4dp·blue, 굵기 800, `postInvalidateDelayed`, quickstart 움직임 줄이기 행 |
| 19 | readiness | Major | research R4, tasks T009~T011 | 샘플 값 대부분이 없고 `Feature.reason`을 못 부른다는 말이 틀림 | 수정(사용자 결정) | Coach.kt `DriveStats`는 공개 var 클래스(사실 오류 수정) + V2: data-model 기록·익힘 표, SeedTest에 `opportunity` |
| 20 | readiness | Major | research R4, spec US4 | 시각 배치와 마지막 기록이 정해지지 않아 seed 뒤 화면이 달라짐 | 수정(사용자 결정) | V2: `records(now, zone)`, 오늘 0시 기준 사흘 전~어제 09:00·18:00, 날짜 셋, 마지막 앰비언트 |
| 21 | readiness | Major | tasks T011·T013·T017 | 수신기 보호와 출시 빌드 확인 방법이 없음 | 수정(사용자 결정) | V6: `permission=DUMP`, 병합 매니페스트 확인(quickstart·T013) |
| 22 | readiness | Major | tasks T019 | 저장소 밖 작업의 대상·완료 기준이 없음 | 수정 | 리뷰 전 대화에서 만든 "안 써본 기능 주행 영상" 아티팩트(11/10). 주소, 장면, 형식, 자막, 확인 기준을 T019에 |

## 재리뷰

Blocker가 없어 하지 않았다.

## 집계

지적 22개 중 문서를 바꾼 것 20개
