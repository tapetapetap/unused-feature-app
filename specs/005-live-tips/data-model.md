# Data Model: 안 써본 기능 5차

1~4차 data-model 위에 더한다. 순수 계산은 `Coach.kt`에 두고 `LiveCoachTest.kt`가 지킨다(원칙 IV). 시각은 모두 앱 시계(디버그 10배)의 운전 시작부터 ms다(`Tracker`와 같다).

## §1 주행 중 문장(`Feature.live`)과 후보 16개

- `Feature`에 `val live: String?`를 더한다. 문구는 contracts/ui.md §3이다.
- `live != null`인 기능이 곧 주행 중 권함 후보다(FR-004). `REAR_SEAT_HEAT`(뒷좌석 시트 열선)와 `AMBIENT`(앰비언트 라이트)는 null이다.
- 후보의 기회 조건(`Feature.opportunity`, 1~2차 그대로)과 그 조건이 쓰는 누적값은 아래와 같다. 운전 중에 값이 자라는 칸은 굵게 쓴다.

| 기능 | 기회 조건 | 쓰는 값 | 운전 중에 자라는 때 |
|---|---|---|---|
| 공조 AUTO | 바람·온도를 손으로 3번 | `manualClimateChanges` | 신호마다(지금) |
| 오토 와이퍼 | 와이퍼 단계를 손으로 3번 | `wiperChanges` | 스위치 읽기(3초)마다 |
| 오토 라이트 | 전조등을 손으로 2번 | `manualLightToggles` | 스위치 읽기마다 |
| 하이빔 보조 | 하이빔을 손으로 3번 | `highBeamToggles` | 스위치 읽기마다 |
| 크루즈 컨트롤 | 시속 70km 이상 10분, 주행거리 신호가 없으면 운전 20분 | `highwayMillis`, **`driveMillis`** | 거리 신호마다 / **`tick()`** |
| 시트 열선 | 난방 26° 이상이고 시트 열선 안 켬 | `maxSetTemp`, `seatHeatOn` | 신호마다 |
| 운전대 열선 | 시트 열선 켬 | `seatHeatOn` | 신호마다 |
| 통풍 시트 | 에어컨 21° 이하, 시트 열선 안 켬 | `acOn`, `minSetTemp`, `seatHeatOn` | 신호마다 |
| 동승석 개별 온도 | 동승자 있고 온도를 1번 이상 바꿈 | `passengerSeated`, `tempChanges` | 신호마다 |
| 동승석 시트 열선·통풍 | 동승자 있고 운전석 열선이나 통풍 씀 | `passengerSeated`, `seatHeatOn`, `used` | 신호마다 |
| 앞유리 김서림 제거 | 비와 내기 순환이 겹친 시간 5분 | **`rainRecircMillis`** | **`tick()`** |
| 뒷유리 열선 | 비 5분 | **`rainMillis`** | **`tick()`** |
| 사이드미러 열선 | 비 5분 | **`rainMillis`** | **`tick()`** |
| 오토 내/외기 | 내기·외기를 손으로 2번 | `recircToggles` | 신호마다 |
| MAX A/C | 에어컨 17° 이하 | `acOn`, `minSetTemp` | 신호마다 |
| 주행 모드 | 시속 70km 이상 15분(주행거리 신호 있을 때) | `highwayMillis` | 거리 신호마다 |

- 밤 시간(`nightMillis`)은 앰비언트 라이트만 쓰므로 운전 중에 채우지 않는다.

## §2 권함(`LiveTip`)

```kotlin
data class LiveTip(val feature: Feature, val at: Long) // at = 권한 때의 운전 경과 ms
```

- `WatchService`가 운전마다 빈 목록으로 시작한다. 저장하지 않는다.
- "써 봤어요"는 따로 두지 않는다. 화면이 마지막 권함 기능이 익힘에 있는지로 정한다(§6).

## §3 권함 고르기(`LiveCoach.next`)

```kotlin
object LiveCoach {
    const val MAX_TIPS = 2
    const val GAP = 5 * MINUTE
    fun next(s: DriveStats, now: Long, tips: List<LiveTip>, shown: Map<Feature, Int>,
             learned: Set<Feature>, unsupported: Set<Feature>, busy: Boolean): Feature?
}
```

- 아래 셋 가운데 하나라도 맞으면 null이다.
  - `tips.size >= MAX_TIPS`
  - 마지막 권함 뒤 `now − tips.last().at < GAP`
  - `now < FIRST`(1분)
  - `busy`(방향지시등 켜짐 또는 기어 R. 부르는 쪽이 `tracker.signalOn || watcher.reverse`로 넘긴다)
- 후보는 `Coach.candidates(s, shown, learned, unsupported)`(P 안내와 같은 조건: 신호 받음, 이 차에 있음, 기회, 안 씀, 안 익힘, 안내 3번 미만) 가운데 아래를 맞는 기능이다(코드 리뷰 반영: 조건을 한 곳에 둔다).
  - `live != null`
  - `tips.none { it.feature == f }`
- 고르기: `minByOrNull { shown[it] ?: 0 }`다. 같으면 `Feature` 열거 순서(= contracts/ui.md §3 표 순서)이고, `Coach.finish`와 같다.
- "기록하는 운전" 조건(FR-005 여덟째)은 부르는 쪽이 지킨다. `tracker.driving`일 때만 부른다.

## §4 `Tracker`·`Span`에 더하는 것

- `Span.totalAt(t: Long) = total + (if (since >= 0) t − since else 0)`
- `Tracker.elapsed(): Long`: 운전 경과 ms. 지금 쓰던 private `t()`를 이 이름 하나로 합친다(코드 리뷰 반영).
- `Tracker.tick()`: 운전 중이면 아래를 지금까지 값으로 쓴다. `endDrive`는 지금처럼 최종값을 다시 쓴다.
  - `stats.rainMillis = rain.totalAt(t)`
  - `stats.rainRecircMillis = rainRecirc.totalAt(t)`
  - `stats.driveMillis = t`
- `Tracker.signalOn`(공개 var): 마지막 방향지시등 상태다. 받은 적이 없으면 false, 운전 끝(`endDrive`)에 false로 돌린다.
  - 운전 기록(`stats`)에는 넣지 않는다. 판단 미루기에만 쓴다.

## §5 P 안내

- `Coach.finish`는 바꾸지 않는다. 고르기와 확인 규칙이 1~4차와 같다.
  - 확인(`confirmed`): 안내 횟수 > 0이고 지난 운전 끝 뒤에 새로 익힌 기능이다. 권함도 안내 횟수를 늘리므로(§7), 권한 뒤 운전 중에 쓴 기능도 확인이 된다.
- `WatchService.onDriveEnd`
  - `Progress.record(outcome)`는 안내 횟수를 올리지 않는다(코드 리뷰 반영: 횟수 올리기를 `countShown` 하나로).
  - 뽑힌 기능(`pick`)이 이번 운전 권함에 있으면 말하지 않고 알림도 띄우지 않으며 `lastSpoken = null`이다. 횟수는 이미 셌다.
  - 없으면 1~4차와 같이 말하고 `countShown(pick)`이다.

## §6 화면이 읽는 값(`WatchService` companion)

```kotlin
@Volatile var tips: List<LiveTip> = emptyList() // 이번 운전 권함(마지막이 카드)
```

- 주행 화면 상태는 아래로 정한다.
  - 기다림: `tips`가 비었다.
  - 권함: 마지막 권함 기능이 익힘(`Progress.learned()`)에 없다.
  - 써 봤어요: 있다. 쓰는 순간 `WatchService.onChange`가 바로 익힘으로 저장하므로, 화면은 익힘으로 본다. 권함 후보는 익힌 기능이 아니므로, 권함 뒤 익힘에 있으면 권함 뒤에 쓴 것이다.
- 다시 그리기 키: `(driving, recording, 마지막 권함 기능, 써 봤는지)`. 키가 같으면 주행 중에 다시 그리지 않는다(FR-003).
- 운전이 새로 시작되면(`recording`이 false → true) `tips`를 비운다.

## §7 안내 횟수(`Progress`)

- `countShown(f)`: `shown_<기능>`을 1 늘린다. 주행 중 권함마다, 그리고 P에서 말한 안내마다 부른다.
- `record(o)`: 추천 기록, 운전 수, 마지막 결과만 남긴다(1~4차에서 하던 횟수 올리기는 `countShown`으로 옮김).
