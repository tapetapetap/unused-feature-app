# Goldfish Review — 안 써본 기능 6차 — 차 화면 개선

- 날짜: 2026-11-13
- 리뷰어 상태: comprehension 완료 · readiness NO-GO(첫 판정 NO-GO, 재리뷰 NO-GO: 새 Blocker 1개는 사용자 판단)

## Comprehension의 다시 쓰기

차량용(AAOS, Pleos Connect) 안드로이드 앱 "안 써본 기능"은 운전 신호를 보고, 운전자가 안 써 본 차 기능을 알려 준다. 주행 중에는 짧게, P에 놓으면 근거 타임라인과 누를 곳까지 보여 준다. 익힌 기능은 차 지도와 추천 기록에서 볼 수 있다.
6차는 디자인 점검에서 나온 P1·P2 문제를 고치는 화면 개선이다. 지금은 글자와 누르는 칸이 폰 크기다. 이것을 Android for Cars 기준(24sp, 76dp, 대비 4.5:1)으로 키우고, 회색·초록·파랑 글자를 진하게 한다.
시스템 뒤로 버튼은 앱을 바로 나가지 않고, 열린 칸(구역 카드, 추천 기록 목록, 기록 다시 보기)을 하나씩 닫는다. 낮·밤 전환처럼 액티비티가 다시 만들어질 때도 열린 칸과 스크롤 위치를 지킨다.
서로 어긋나는 문구와 숫자도 맞춘다. 대상은 주행 중 기다림 문구, 앞유리 김서림 근거 카드, 기능 없는 구역의 "0/0"이다. 판단 로직, 음성, 저장 형식은 바꾸지 않는다.

### 기존 시스템

- 앱은 Pleos Connect 에뮬레이터(2560×1440, density 160)에서 가로 전체(약 1,700dp)나 내비 옆 1:1 분할(약 850dp 폭)로 뜬다.
  - 지도 머리에는 "n개 익힘 · m개 남음"과 "추천 기록 n ›" 버튼이 있다.
  - 그 아래 차 지도에 구역 점과 이름표("전조등 0/0" 등)가 있고, 범례가 붙는다.
  - 세로 배치에는 구역 목록 줄이 더 있다.
- 정차 중에 여는 칸은 세 가지다. 구역 점이나 줄을 누르면 구역 카드, "추천 기록"을 누르면 목록, 기록 카드를 누르면 기록 다시 보기가 열린다.
  - 주행 중에는 이 칸들이 닫혀 있다.
  - 운전이 시작되거나 새 운전 결과가 생기면 칸이 닫힌다.
  - 다른 앱에 갔다 오면 상황 화면부터 보인다.
- 화면은 `Ui` 도우미로 코드에서 조립하고, 크기는 폰 기준이다.
  - 글자: small 22sp, chip 20sp, cardTitle 26sp, 그림 안 글자 18~20sp.
  - 누르는 칸: 닫기 칩 99×46dp, 추천 기록 버튼 154×51dp.
  - 각주는 text4라 대비가 3.2:1(밤 3.3:1)이다. 연한 바탕 위 초록·파랑 글자도 4.5:1에 못 미친다.
- 뒤로 버튼은 칸이 열려 있어도 바로 앱을 나간다.
- 낮·밤이 바뀌면 configChanges가 없어서 액티비티가 새로 만들어진다. 이때 onPause가 칸을 닫는다. 새 인스턴스는 drivesSeen = -1로 시작해서 "새 운전 결과면 닫기"에 한 번 더 걸린다.
- 주행 중 기다림 카드는 "한 번 알려 드릴게요"와 "두 번까지만, 짧게 알려요"를 함께 보인다.
- 근거 카드 오른쪽 숫자는 순간 표시가 있으면 "n번", 없으면 구간 합 "n분"이다. 그래서 앞유리 김서림 카드는 제목(겹친 구간), 숫자(1번), 막대(와이퍼 12분), 이유(8분)가 서로 다르다.
- 내기 순환 표시 글자는 "내기 순환"/"외기"로 기록에 저장된다.

## 지적과 처리

| # | 리뷰어 | 심각도 | 위치 | 요지 | 처리 | 근거·변경 |
|---|---|---|---|---|---|---|
| 1 | comprehension | Major | spec FR-005·US2-1·SC-002·Assumptions | "큰 글자" 기준이 문서마다 달라 밤 화면의 두 요소가 FR-005를 어김 | 수정 | 리뷰 전 설계(C3)와 contracts §3의 예외를 용어 "큰 글자 예외" 한 곳에 정의(40sp 이상 제목, 파랑 바탕 흰 굵은 글자). FR-005·US2-1·SC-002가 그것을 가리키게, SC-002의 표는 research R5 |
| 2 | comprehension | Major | spec FR-004 | 24dp를 요구하는 쌍이 없고 측정도 없음 | 수정 | 리뷰 전 설계(C2 "이웃한 누르는 칸 사이 24dp")대로 세로 구역 줄·추천 기록 카드 간격 24dp를 contracts §2·T006·T007에, 측정을 SC-001·quickstart §2의 5에 |
| 3 | comprehension | Major | spec Key Entities·FR-012·US3 | "화면 상태"가 두 뜻, "상황 화면" 정의 없음 | 수정 | 용어에 상황 화면과 우선순위(출처 specs/002·003 data-model "화면 상태"), Key Entity 이름을 "열린 칸 상태"로 |
| 4 | comprehension | Major | spec 용어·FR-002 | 그림 안 글자 22sp가 차 기준보다 작은 이유가 없음 | 수정 | 리뷰 전 설계(C1 "그림 안 글자 20sp 이상")의 이유(차 그림 둘레의 좁은 자리, 높이 196dp 고정)를 용어에 |
| 5 | comprehension | Major | spec Edge Cases(차 지도 이름표) | "칸"과 "지금 규칙"이 모호, 오른쪽 넘침 | 수정 | #13과 같은 변경 |
| 6 | comprehension | Major | spec Input·US1 | 같은편·DRIVE DNA가 정의 없음, 도우미 공유로 읽힘 | 수정 | "점검과 범위"에 두 앱이 다른 저장소의 다른 앱이고 도우미는 앱마다 복사, 이 명세는 이 앱만 바꾼다고 |
| 7 | comprehension | Major | spec Input·SC-005 | P1·P2 목록·impeccable 설명이 없고, "28점 이상"이 모호 | 수정 | P1·P2 항목 표(항목 → US·FR), impeccable 설명, SC-005를 "29점 이상(28점보다 높게)"으로 |
| 8 | comprehension | Nit | spec 용어 "칸" | 앱 창도 "칸"이라 부름 | 기록 | |
| 9 | comprehension | Nit | spec US1-2 | Given에 "← 추천 기록"이 있는 화면이 없음 | 수정 | 사실 오류. Given에 기록 다시 보기 |
| 10 | comprehension | Nit | spec Key Entities·US3-4 | 보존하는 스크롤 값이 문서마다 다름 | 수정 | Key Entity에 listY·scrollY, quickstart §4에 다시 보기 줄 |
| 11 | comprehension | Nit | spec US4·FR-008 | 같은 카드를 두 이름으로 | 기록 | |
| 12 | comprehension | Nit | plan Scale/Scope | 화면 코드 파일 수 4 → 3 | 수정 | 사실 오류 |
| 13 | readiness | Blocker | spec Edge Cases, contracts §2, T009·T020 | 세로 1:1에서 22sp 이름표가 오른쪽으로 넘치는데 지금 코드는 왼쪽만 밀어 넣음 | 수정 | Views.kt 277행 확인. 리뷰 전 뜻(이름표는 지도 뷰 안)대로 양쪽 밀기 + 점을 덮으면 위(ABOVE)로 옮기는 규칙을 적음. 재리뷰에서 이 규칙도 세로 화면에서 겹친다는 지적 → #26 |
| 14 | readiness | Major | spec FR-004 | 세로 구역 줄 12dp·추천 기록 카드 20dp | 수정 | #2와 같은 변경(MainActivity.kt 167행, Screens.kt 370행 확인) |
| 15 | readiness | Major | spec FR-005·SC-002 ↔ contracts §3·R5 | 큰 글자 기준 불일치, 흐린 상태의 대비가 없음 | 수정 | #1과 같은 변경. 흐린 비활성 요소(alpha 0.5, 90/255)는 WCAG 1.4.3 예외로 용어와 contracts §3에 |
| 16 | readiness | Major | contracts §2 | 진행 원 가운데 숫자(약 20sp)가 정해져 있지 않음 | 수정 | Views.kt 50행 확인. 리뷰 전 설계 C1(그림 안 글자 20sp 이상)에 맞으므로 그대로라고 적음(같은 수를 mapSub 24sp가 보임) |
| 17 | readiness | Major | research R4 | 세로에서 3열 칩 격자가 넘침, 가장 긴 그림 확인 절차 없음 | 수정 | 리뷰 전 요구(spec Edge "세로에서도 잘리지 않아야")에 따라 세로 2열(T006), quickstart §3의 3에 `rain`으로 앞유리 김서림 권함 캡처 |
| 18 | readiness | Major | quickstart §5.2 | 적힌 절차로는 앞유리 김서림 근거 카드를 볼 수 없음, seed 글자 미정 | 수정 | 5차 verification `rain` 줄 확인. seed 기록으로 확인하고 Seed.kt 글자도 새 글자로(T019) |
| 19 | readiness | Major | quickstart §2.4 | grep이 괄호 있는 호출을 놓침 | 수정 | 사실 오류. 후보를 모두 뽑고 글자 크기 인자만 보는 절차 |
| 20 | readiness | Major | tasks Phase 2·3 Checkpoint | 그 시점에 통과할 수 없는 확인을 요구 | 수정 | 사실 오류. Phase 2는 테스트 컴파일 실패, Phase 3는 크기·간격·그림만, 글자 검사·문구는 Phase 7. 기다림 카드 캡처 절차를 quickstart §3의 2에 |
| 21 | readiness | Major | tasks T020 | "기능 없음" 형식이 문서끼리 엇갈리고 구역 카드에 "0 / 0 익힘"이 남음 | 수정 | MainActivity.kt 191·198행, Screens.kt 393행 확인. US4의 리뷰 전 뜻("숫자가 서로 맞는다")대로 지도 이름표·세로 줄·구역 카드 문구를 각각 정함 |
| 22 | readiness | Major | tasks T022·Implementation Strategy | 저장소 밖 경로가 상대 경로 | 수정 | 절대 경로 |
| 23 | readiness | Nit | tasks T018·T019 | [P]가 같은 파일과 맞지 않음 | 수정 | 사실 불일치. [P] 빼고 둘끼리만 동시에 |
| 24 | readiness | Nit | plan Scale/Scope | 파일 수 | 수정 | #12와 같은 변경 |
| 25 | readiness(재) | Blocker | spec FR-003, contracts §2 | 세로 1:1 화면의 차 지도 구역 점은 누름 지름이 약 46dp라 76dp를 못 맞춤(점 사이 약 26dp) | 수정(사용자 결정) | 세로(1:1) 화면의 차 지도를 어떻게 할까요? (가) 세로에서는 지도를 그림으로만 둔다: 이름표를 숨기고 점 누르기를 끄며, 바로 옆 구역 목록(76dp 줄, 이름·익힌 수)으로 구역 카드를 연다. (나) 지도 점은 76dp 예외로 두고 지금처럼 누르게 하고, 이름표는 다른 점·이름표와의 겹침 검사 규칙을 새로 만든다 |
| 26 | readiness(재) | Major | tasks T009, contracts §2, research R4, quickstart §3.5 | 밀기·ABOVE 규칙대로 그리면 세로에서 이름표가 다른 점·이름표와 겹치고, 그다음 규칙이 없음 | 수정(사용자 결정) | #25와 같은 질문. (가)를 고르면 세로에서 이름표가 없어 이 규칙이 필요 없다 |
| 27 | readiness(재) | Major | quickstart §2·§3, tasks T005·T006·T011 | 세로 칩 2열과 새 칩 테두리를 반드시 보게 하는 절차가 없음 | 수정 | demo.sh 9행 확인. `highbeam`(권할 것 없음)과 `climate` → `learn`(써 보셨네요)을 분할·가로에서 캡처하는 quickstart §3의 5-1 |
| 28 | readiness(재) | Major | tasks T019 | "외기로 바꿈"을 확인할 방법이 없고, grep이 바꾸면 안 되는 신호 이름도 찾음 | 수정 | CoachTest.kt 240행, CarWatcher.kt 241·245행 확인. `recirculationToggles`에 표시 글자 단언, 신호 이름은 그대로라고 명시 |
| 29 | readiness(재) | Nit | plan Summary | 세로 구역 줄 여백 위치 | 수정 | MainActivity.kt 167행(`render`)으로 |
| 30 | readiness(재) | Nit | tasks T022·T027 | verification.md를 쓰는 차례 | 수정 | T022에서 만들고 T027에서 채움 |

## 재리뷰

- 대상: readiness. 첫 판정 NO-GO → 재리뷰 NO-GO.
- 새 지적: #25~#30. #25(Blocker)와 #26은 사용자 판단으로 둔다(재리뷰는 한 번뿐).

## 집계

지적 30개 중 문서를 바꾼 것 28개

게이트(2026-11-13 21:40) 사용자 결정: #25·#26은 (가) "세로에서는 지도를 그림으로만" → spec Edge·FR-003, contracts §2, research R4, T009, quickstart §3의 5에 반영.
