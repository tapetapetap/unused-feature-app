# Data Model: 안 써본 기능

## Feature (고정 6개, 순서 = 우선순위)

| id | 이름 | 기회 조건 (DriveStats) | 사용 조건 |
|---|---|---|---|
| AUTO_CLIMATE | 공조 AUTO | `manualClimateChanges >= 3` | `autoOn` |
| DUAL_CLIMATE | 동승석 개별 온도 | `passengerSeated && tempChanges >= 1` | `dualOn` |
| WHEEL_HEAT | 운전대 열선 | `seatHeatOn` | `wheelHeatOn` |
| SEAT_VENT | 통풍 시트 | `acOn && minSetTemp <= 21 && !seatHeatOn` | `ventOn` |
| AUTO_LIGHT | 오토 라이트 | `manualLightToggles >= 2` | `autoLightOn` |
| CRUISE | 크루즈 컨트롤 | `driveMillis >= 20분` | `cruiseOn` |

각 Feature는 화면 이름, 안내 문장(음성), 이유 문장(카드), 쓰는 법(카드)을 가집니다. 문장은 [contracts/ui.md](contracts/ui.md).

## DriveStats (운전 하나, 메모리에만)

| 필드 | 뜻 | 갱신 |
|---|---|---|
| manualClimateChanges | 풍량·설정 온도 변화 횟수(AUTO 꺼진 상태에서) | 풍량/온도 콜백, 값이 직전과 다를 때 |
| tempChanges | 설정 온도 변화 횟수 | 온도 콜백 |
| minSetTemp | 이번 운전 최저 설정 온도 | 온도 콜백 |
| autoOn / dualOn / acOn | 이번 운전 중 한 번이라도 켜짐 | 해당 콜백 true |
| passengerSeated | 조수석 착좌가 한 번이라도 true | 착좌 콜백 |
| seatHeatOn / ventOn / wheelHeatOn | 값 > 0이 한 번이라도 | 해당 콜백 |
| manualLightToggles | 전조등 ON↔OFF 변화 중 스위치가 AUTO가 아니었던 횟수 | 전조등 상태 콜백 + 스위치 조회 |
| autoLightOn | 스위치가 AUTOMATIC으로 확인됨 | 스위치 조회 |
| cruiseOn | 크루즈 ACTIVATED(ENABLED는 대기라 제외) | 크루즈 콜백 |
| driveMillis | 운전 길이 | 운전 끝에 채움 |

"직전 값"은 운전 시작 전 마지막 값도 포함합니다. 첫 콜백(구독 직후 현재값)은 변화로 세지 않습니다.

`heard`: 사용 신호를 한 번이라도 받은 기능. 여기에 없는 기능은 안내하지 않습니다(써도 확인할 수 없으므로).

확인 문장(`confirmed`)은 "지난 운전 끝 이후 새로 익힌 기능 중 안내한 적 있는 것"입니다. 정차 중에 써 본 것도 포함합니다.

## Progress (기기 저장)

| 키 | 값 |
|---|---|
| `shown_<id>` | 안내 횟수(Int, 0~3) |
| `learned_<id>` | 익힘 여부(Boolean) |
| `last_pick` | 마지막 운전에서 안내한 기능 id(없으면 비움). 카드 문장은 Feature에서 꺼냄 |
| `last_confirmed` | 마지막 운전에서 써 본 것으로 확인한 기능 id(없으면 비움) |
| `drove` | 운전 끝 판단을 한 번이라도 했는지(빈 카드 문구 구분) |

## 상태 전이

- 운전: `PARKED --(기어 != P)--> DRIVING --(기어 == P)--> PARKED`. DRIVING에서 PARKED로 갈 때 길이 ≥ 1분이면 `Coach.finish` 호출.
- 기능: `아직 --(안내)--> 안내함(n) --(사용 신호)--> 익힘`. `아직 --(사용 신호)--> 익힘`도 가능. n=3이면 더 안내하지 않음. 익힘은 끝 상태.
- 사용 신호는 운전 중이 아니어도(정차 중 조작) 익힘으로 기록합니다.
