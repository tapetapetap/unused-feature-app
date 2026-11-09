# Data Model: 안 써본 기능 2차

> 구현 중 기능 21개 → 18개(오토 홀드·LFA·i-PEDAL 제외, 계기판 표시등 읽기에 내부 전용 권한 필요). 아래 표는 구현 기준이다.

시간 값은 모두 앱 시계(research R5, 디버그 10배) 기준 밀리초다. "분"은 그 시계의 분이다.

## Zone (구역, 7개 고정)

차 그림 좌표계는 300×560(앞이 위).

| id | 이름 | 아이콘 | 점 위치 (x, y) | 이름표 |
|---|---|---|---|---|
| WINDSHIELD | 앞유리·와이퍼 | 와이퍼 | (150, 160) | 오른쪽 |
| HEADLAMP | 전조등 | 전조등 | (88, 66) | 오른쪽 |
| MIRROR | 사이드미러 | 미러 | (24, 206) | 위 |
| CLIMATE | 공조 패널 | 바람 | (150, 214) | 오른쪽 |
| WHEEL | 운전대 | 운전대 | (105, 246) | 왼쪽 |
| SEAT | 시트 | 시트 | (105, 298) | 왼쪽 |
| CONSOLE | 콘솔·실내 | P 기어 | (150, 362) | 오른쪽 |

## Feature (기능, 18개 고정)

열거 순서 = 같은 안내 횟수일 때의 우선순위(FR-003). 체감이 크고 자주 오는 상황부터.

필드: 이름, 구역, 아이콘, 음성 문장, 이유 한 줄(이번 운전 값으로 채움), 근거 제목, 언제 쓰면 좋은지, 누를 곳(자리, 버튼들, 강조 번호), 근거 종류, 기회 조건. 문장은 `contracts/ui.md`.

| 순서 | id | 구역 | 기회 조건 (DriveStats) | 썼다고 보는 신호 (→ used) | 신호를 받았다고 보는 때 (→ heard) | 근거 종류 |
|---|---|---|---|---|---|---|
| 1 | AUTO_CLIMATE | CLIMATE | `manualClimateChanges ≥ 3` | 공조 AUTO true | AUTO 값 수신 | FAN, TEMP |
| 2 | AUTO_WIPER | WINDSHIELD | `wiperChanges ≥ 3` (AUTO가 아닌 두 값 사이 변경) | 와이퍼 스위치 AUTO | 스위치 값 수신 | WIPER |
| 3 | AUTO_LIGHT | HEADLAMP | `manualLightToggles ≥ 2` (1차와 같음) | 전조등 스위치 AUTOMATIC | 스위치 값 수신 | LIGHT |
| 4 | HIGH_BEAM_ASSIST | HEADLAMP | `highBeamToggles ≥ 3` | 하이빔 스위치 AUTOMATIC | 하이빔 스위치 값 수신 | HIGH_BEAM |
| 5 | CRUISE | WHEEL | 주행거리 신호를 받았으면 `highwayMillis ≥ 10분`, 아니면 `driveMillis ≥ 20분` | 크루즈 ACTIVATED | 크루즈 값 수신 | HIGHWAY (없으면 DRIVE) |
| 6 | SEAT_HEAT | SEAT | `maxSetTemp ≥ 26 && !seatHeatOn` | 운전석 시트 온도 > 0 | 운전석 시트 온도 수신 | HEAT |
| 7 | WHEEL_HEAT | WHEEL | `seatHeatOn` | 운전대 열선 > 0 | 운전대 열선 수신 | SEAT_HEAT |
| 8 | SEAT_VENT | SEAT | `acOn && minSetTemp ≤ 21 && !seatHeatOn` | 운전석 통풍 > 0 또는 운전석 시트 온도 < 0 | 통풍 또는 시트 온도 수신 | AC |
| 9 | DUAL_CLIMATE | CLIMATE | `passengerSeated && tempChanges ≥ 1` | DUAL true | DUAL 수신 | PASSENGER, TEMP |
| 10 | PASSENGER_SEAT | SEAT | `passengerSeated && (seatHeatOn || ventOn)` | 조수석 시트 온도 ≠ 0 | 조수석 시트 온도 수신 | PASSENGER, SEAT_HEAT, VENT |
| 11 | FRONT_DEFROST | WINDSHIELD | `rainRecircMillis ≥ 5분` (비 + 내기 순환이 겹친 시간) | 앞유리 제상 true | 앞유리 제상 수신 | RAIN, RECIRC |
| 12 | REAR_DEFROST | CLIMATE | `rainMillis ≥ 5분` | 뒷유리 제상 true | 뒷유리 제상 수신 | RAIN |
| 13 | MIRROR_HEAT | MIRROR | `rainMillis ≥ 5분` | 미러 열선 > 0 | 미러 열선 수신 | RAIN |
| 14 | AUTO_RECIRC | CLIMATE | `recircToggles ≥ 2` (오토 내/외기가 꺼진 동안) | 오토 내/외기 true | 오토 내/외기 수신 | RECIRC |
| 15 | MAX_AC | CLIMATE | `acOn && minSetTemp ≤ 17` | MAX A/C true | MAX A/C 수신 | AC |
| 16 | DRIVE_MODE | CONSOLE | `distanceHeard && highwayMillis ≥ 15분` | 주행 모드 ECO 또는 SPORT | 주행 모드 수신 | HIGHWAY |
| 17 | REAR_SEAT_HEAT | SEAT | `rearSeated && seatHeatOn` | 뒷좌석 시트 온도 > 0 | 뒷좌석 시트 온도 수신 | REAR, SEAT_HEAT |
| 18 | AMBIENT | CONSOLE | `nightMillis ≥ 10분` (전조등 켠 시간) | 앰비언트 ON | 앰비언트 수신 | NIGHT |

공통 규칙(1차 유지): 후보 = `heard`에 있고, 지원 기능이고(아래 "지원 기능", `Coach.finish`의 `unsupported`), 기회가 있고, 이번 운전에 쓰지 않았고, 익힘이 아니고, 안내 3번 미만. 그중 안내 횟수가 가장 적은 것, 같으면 위 순서.

## DriveStats (한 번의 운전, 메모리)

- `driveMillis`
- 횟수: `manualClimateChanges`, `tempChanges`, `manualLightToggles`, `highBeamToggles`, `wiperChanges`, `recircToggles`
- 설정 온도: `minSetTemp`, `maxSetTemp`
- 시간: `rainMillis`, `rainRecircMillis`, `nightMillis`, `highwayMillis`
- 상태: `acOn`, `seatHeatOn`, `ventOn`, `passengerSeated`, `rearSeated`, `distanceHeard`
- `used: MutableSet<Feature>` — 이번 운전에서 쓴 기능. 운전 시작 때 이미 켜져 있던 것도 포함한다
- `heard: MutableSet<Feature>` — 서비스가 시작된 뒤 사용 신호 값을 한 번이라도 받은 기능. 새 운전을 시작할 때 그때까지 받은 값으로 다시 채우므로 운전이 바뀌어도 이어진다
- `marks: MutableList<Mark>` — 근거 기록(아래)

횟수 세는 법: 운전 중(P가 아닐 때)에만 센다. 직전 값(운전 전에 받은 값 포함)과 달라진 상태 변화 한 번을 1로 센다. 켬과 끔은 각각 1번이다. 구독 직후 처음 받은 값은 기준점이라 세지 않는다.

| 필드 | 1로 세는 것 | "손으로"의 조건 |
|---|---|---|
| `manualClimateChanges` | 풍량 변경 1번, 설정 온도 변경 1번 | 풍량은 공조 AUTO가 켜져 있지 않을 때만. 설정 온도는 늘 센다 |
| `tempChanges` | 설정 온도 변경 1번 | — |
| `manualLightToggles` | 전조등 켜짐↔꺼짐 1번 | 전조등 스위치가 AUTO가 아니라고 확인된 때만 |
| `highBeamToggles` | 하이빔 켜짐↔꺼짐 1번 | 하이빔 스위치가 AUTO가 아니라고 확인된 때만 |
| `wiperChanges` | 와이퍼 스위치 단계 변경 1번 | AUTO가 아닌 두 단계 사이(OFF↔LO 포함) |
| `recircToggles` | 내기 순환 켜짐↔꺼짐 1번 | 오토 내/외기가 켜져 있지 않을 때만 |

와이퍼·전조등·하이빔 스위치 값은 운전 중 3초마다 읽는다(plan 핵심 설계 4).

1차의 기능별 Boolean 묶음(`autoOn` 등)은 `used` 집합 하나로 바꾼다.

## Mark (근거)

| 필드 | 뜻 |
|---|---|
| `kind` | FAN, TEMP, PASSENGER, REAR, AC, HEAT, SEAT_HEAT, VENT, LIGHT, HIGH_BEAM, WIPER, RAIN, RECIRC, NIGHT, HIGHWAY, DRIVE |
| `at` | 운전 시작부터 몇 ms 뒤 |
| `end` | 구간이면 끝(ms), 순간이면 -1 |
| `text` | 표시 글자. 예: "풍량 2→4", "와이퍼 LO→HI", "에어컨 20°" |

- 구간(RAIN, NIGHT, HIGHWAY, DRIVE)은 켜짐→꺼짐마다 하나. 운전이 끝날 때 열려 있던 구간은 끝 시각으로 닫는다.
- 근거는 운전 중에만 남긴다. 운전을 시작할 때 이미 켜져 있던 상태는 0분 근거로 남기고, 이미 켜져 있던 구간은 0분에서 시작한다. 그래서 출발 전부터 켜 둔 상태로 기회가 생긴 기능도 근거가 1개 이상이다.

| 종류 | 남기는 때 | 운전 시작 때 |
|---|---|---|
| FAN | 공조 AUTO가 켜져 있지 않을 때 풍량이 바뀜 | — |
| TEMP | 설정 온도가 바뀜 | — |
| AC | 에어컨을 켤 때 설정 온도가 21° 이하, 또는 에어컨이 켜진 채 설정 온도가 21°나 17° 아래로 내려감 | 에어컨이 켜져 있고 21° 이하면 0분 |
| HEAT | 설정 온도가 26° 이상으로 올라감 | 26° 이상이면 0분 |
| SEAT_HEAT | 운전석 시트 열선이 꺼짐→켜짐 | 켜져 있으면 0분 |
| VENT | 운전석 통풍이 꺼짐→켜짐(시트 온도 음수 포함) | — |
| PASSENGER / REAR | 조수석 / 뒷좌석이 비어 있음→앉음 | 앉아 있으면 0분 |
| LIGHT / HIGH_BEAM / WIPER / RECIRC | DriveStats 표의 "손으로" 조작 1번마다 | — |
| RAIN / NIGHT (구간) | 비(와이퍼가 움직임) / 전조등 켬 | 이미 켜져 있으면 0분에서 시작 |
| HIGHWAY (구간) | 아래 "고속 구간 계산" | — |
| DRIVE (구간) | 주행거리 신호를 받지 못한 운전이 끝날 때 0분~끝 막대 하나(크루즈 대체 근거) | — |
- 저장 형식: 한 줄에 하나, `kind|at|end|text`. 글자에는 `|`와 줄바꿈을 쓰지 않는다. 최대 60개.

## 고속 구간 계산

주행거리 표본 `(t, km)`을 받을 때마다: `t - t0 ≥ 60초`인 가장 최근 표본 `(t0, km0)`를 찾아 `(km - km0) / (t - t0)`가 시속 70km 이상이면 직전 표본 시각부터 `t`까지를 고속으로 더한다. 연속한 고속 구간은 HIGHWAY 근거 하나로 합친다.

## Outcome (운전 끝 판단 결과)

| 필드 | 뜻 |
|---|---|
| `pick` | 안내할 기능(없으면 null) |
| `confirmed` | 안내했던 기능 중 지난 운전 끝 뒤에 익힌 것(1차와 같음) |
| `reason` | `pick`의 이유 한 줄(이번 운전 값으로 채움) |
| `marks` | `pick`의 근거 종류에 맞는 근거만 |
| `used` | 이번 운전에서 쓴 기능 |
| `driveMillis` | 운전 길이 |

## Progress (기기 저장, SharedPreferences "progress")

| 키 | 값 | 비고 |
|---|---|---|
| `shown_<id>` | Int | 1차 |
| `learned_<id>` | Boolean | 1차. 익힘(spec Key Entities). 사용 신호가 처음 참이 되는 순간 저장하고 되돌리지 않는다 |
| `learned_at_<id>` | Long(벽시계 ms) | 새로. 도감 날짜, 처음 쓴 순간 |
| `learned_note_<id>` | String | 새로. "운전을 시작하고 2분 뒤" 또는 "정차 중" |
| `last_pick`, `last_confirmed` | String? | 1차 |
| `last_reason`, `last_marks`, `last_used` | String | 새로 |
| `last_drive_ms` | Long | 새로 |
| `drives` | Int | 새로. 1차의 `drove`를 대신함 |
| `first_drive` | Long(벽시계 ms) | 새로 |

## 화면 상태 (MainActivity)

우선순위 위에서부터:

1. **DRIVING**: 기어가 P가 아님(`WatchService.driving`). 서비스가 그 운전을 기록하는지와 상관없다. 기록 여부(`WatchService.recording`)는 주행 중 화면의 아랫줄만 바꾼다.
2. **ZONE**: 사용자가 구역을 누름(정차 중만). 닫으면 아래 상태로.
3. **FIRST_RUN**: `drives == 0`.
4. **COMPLETE**: 지원 기능을 모두 익힘.
5. **TIP**: `last_pick`이 있고 아직 익히지 않음. `last_confirmed`가 있으면 위에 초록 띠.
6. **CONFIRM**: `last_confirmed`가 있거나 `last_pick`을 이미 익힘.
7. **NONE**: 그 밖.

**지원 기능**(진행 원 분모, 구역 카드 "이 차에 없음", COMPLETE 판단): 18개에서 차가 그 기능을 갖고 있지 않다고 확인된 기능을 뺀 것(`WatchService.unsupported`, 서비스 메모리).

| 경우 | 지원 수에서 | 권하기 |
|---|---|---|
| 능력 확인(`check…Capabilities`)이 실패함(예: `ERROR_NOT_SUPPORTED_PROPERTY`) | 뺀다 | 안 함 |
| 능력 확인은 됐지만 대상 자리(좌석·유리·미러)가 없음 | 뺀다 | 안 함 |
| 전조등·하이빔 스위치 선택지에 AUTO가 없음 | 오토 라이트·하이빔 보조를 뺀다 | 안 함 |
| 통풍 능력 확인이 실패함 | 빼지 않는다(시트 온도 음수로도 통풍을 알 수 있음) | 신호를 받으면 함 |
| 구독 호출이 예외를 던지거나 구독 중 일시적으로 실패함 | 빼지 않는다(로그만 남김) | 값을 받기 전까지 안 함 |
| 값을 아직 한 번도 받지 못함 | 빼지 않는다 | 안 함(`heard`에 없음) |

에뮬레이터는 14개다. MAX A/C·주행 모드는 능력 확인이 실패하고, 전조등·하이빔 스위치 선택지는 둘 다 `[OFF, ON]`이라 AUTO가 없다(앱 로그 "전조등 스위치 선택지", "하이빔 스위치 선택지"). 오토 라이트·하이빔 보조는 스위치 값이 계속 들어와 `heard`에는 있지만, 지원 기능이 아니므로 권하지 않는다.

지도 구역 점 상태: TIP의 기능이 속한 구역 = 파랑 깜빡임, CONFIRM의 기능 구역 = 초록 반짝임 3번, 구역 기능을 모두 익힘 = 초록, 그 밖 = 흰 점 + 진행 링(익힌 수/지원 수). DRIVING이면 지도 전체를 흐리게 하고 누르기를 막는다.
