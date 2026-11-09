# Data Model: 추천 기록 페이지

002 data-model(`specs/002-redesign-21-features/data-model.md`)에 더하는 것만 적는다. 그 문서의 Feature, Mark, Outcome, Progress, 화면 상태를 그대로 쓴다.

## Record (추천 기록 1개)

| 필드 | 형 | 뜻 | 검사 |
|---|---|---|---|
| `at` | Long | 운전이 끝나 기록한 시각(벽시계 ms) | 숫자여야 함 |
| `pick` | Feature | 안내한 기능 | 지금 앱에 있는 기능 이름이어야 함 |
| `reason` | String | 이유 한 줄(`Outcome.reason`) | — |
| `driveMs` | Long | 운전 길이(`Outcome.driveMillis`, 앱 시계 ms) | 숫자여야 함 |
| `marks` | List<Mark> | 그 기능의 근거(`Outcome.marks`, 최대 60개) | `Marks.decode` 규칙. 읽을 수 없는 줄은 빠짐 |

- 안내가 나간 운전(`Outcome.pick != null`)마다 1개를 만든다. 권할 것이 없던 운전은 만들지 않는다.
- 최대 54개다(18개 기능 × 최대 3번).
- 검사에 실패한 기록은 읽을 때 건너뛴다(FR-009).

## 저장 (Progress)

| 키 | 값 | 비고 |
|---|---|---|
| `history` | String | 새로 추가. `History.encode` 형식 |

- **형식**: `기록1 \u001E 기록2 \u001E …`(오래된 것이 앞). 기록은 `at \u001F pick \u001F reason \u001F driveMs \u001F marks`다.
  - `marks`는 `Marks.encode` 형식(`kind|at|end|text`를 줄바꿈으로 이음)이다.
- **덧붙이기**: `History.append(old, record)`
  - `old`가 비어 있으면 `encode(listOf(record))`를 돌려준다.
  - 그 밖에는 `old + "\u001E" + encode(listOf(record))`를 돌려준다.
- **읽기**: `History.decode(s)`는 기록마다 필드가 정확히 5개이고 숫자와 기능 이름을 읽을 수 있는 것만 남긴다. `Progress.history()`는 그 순서를 뒤집어 최신을 앞에 둔다.
- 기존 키(`shown_`, `learned_`, `last_*` 등)는 바꾸지 않는다.

## "써 봄"

`learned_<pick>`이 참이면 "써 봄"이다. 날짜는 `learned_at_<pick>`(0이면 날짜를 빼고 "써 봄 ✓"만 쓴다).

## 화면 상태 (MainActivity, 002에 더함)

우선순위는 위에서부터다.

1. **DRIVING**: 기어가 P가 아님(002와 같음)
2. **RECORD**: 기록 다시 보기(`openHistory = n`, n은 `history()`의 번호)
3. **HISTORY**: 기록 목록(`openHistory = -1`)
4. **ZONE**: 구역 카드(002와 같음)
5. 이하 FIRST, COMPLETE, TIP, CONFIRM, NONE(002와 같음)

| 지금 | 사건 | 다음 |
|---|---|---|
| 상황 화면 또는 ZONE | "추천 기록" 버튼(정차 중) | HISTORY. 구역 카드는 닫힘 |
| HISTORY | 항목 누름 | RECORD |
| RECORD | "← 추천 기록" | HISTORY |
| HISTORY 또는 RECORD | "추천 기록" 버튼 | HISTORY. 늘 목록으로 간다(목록이면 그대로). 닫기는 "✕ 닫기"만 한다 |
| HISTORY | "✕ 닫기" | 상황 화면 |
| HISTORY 또는 RECORD | 구역 점(정차 중) | ZONE. 기록 페이지는 닫힘 |
| HISTORY, RECORD, ZONE | 기어가 P가 아님 | DRIVING. 페이지는 닫힘 |
| 어느 상태든 | 운전 횟수가 바뀜(운전 끝) | 상황 화면. 페이지는 닫힘 |

- RECORD의 번호가 `history()` 범위 밖이면 HISTORY로 본다.
- **지도**: RECORD에서는 그 기능의 구역을 선택 링으로 그리고, 파란 깜빡임과 초록 반짝임은 없다. HISTORY에서는 지도 표시가 없다(구역 진행 링만).
- **선택 링**: 구역 카드를 열 때 그 구역에 그리는 것과 같은 표시다. 구역 점 바깥, 초록 진행 링보다 더 바깥에 굵은 글자색 원을 그린다(`CarMapView`의 `selected`).
