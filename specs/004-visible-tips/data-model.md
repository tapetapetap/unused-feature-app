# Data Model: 안 써본 기능 4차

1~3차 data-model에 더한다.

## Spoken

```kotlin
data class Spoken(val feature: Feature, val at: Long /* uptimeMillis, 말하기 시작 */, val said: Boolean /* speak()가 true */)
// WatchService.Companion: var lastSpoken: Spoken? = null
```

- 막대가 움직이는 끝 시각: `until = if (lastSpoken.feature == 화면의 기능 && said) at + 6_000 else 0`
- 움직이는 조건: `uptimeMillis < until && ValueAnimator.areAnimatorsEnabled()`

## Seed(순수, 위임 결정 V2)

```kotlin
object Seed {
    val learned: Set<Feature>                          // AUTO_CLIMATE, SEAT_HEAT
    fun records(now: Long, zone: TimeZone): List<Record> // 6개, 오래된 것이 앞(History 저장 순서)
    fun learnedAt(records: List<Record>): Map<Feature, Pair<Long, String>> // 익힌 시각과 메모
}
```

**기록 6개**(D = `zone` 기준 오늘 0시. 근거 표시의 at·end는 운전 시작부터 지난 ms, 점은 end −1)

| # | 기능 | 시각 | 운전 | 통계 → 이유 문장(`f.reason`) | 근거 표시 |
|---|---|---|---|---|---|
| 1 | AUTO_CLIMATE | D − 3일 09:00 | 14분 | manualClimateChanges 3 → "바람과 온도를 손으로 3번 바꾸셨어요." | FAN 2분 "풍량 2→4", FAN 3분 "풍량 4→5", TEMP 5분 "온도 24→23" |
| 2 | AUTO_WIPER | D − 3일 18:00 | 22분 | wiperChanges 4 → "와이퍼 단계를 손으로 4번 바꾸셨어요." | WIPER 4분 "와이퍼 OFF→LO", 7분 "와이퍼 LO→HI", 12분 "와이퍼 HI→LO", 18분 "와이퍼 LO→OFF" |
| 3 | CRUISE | D − 2일 09:00 | 35분 | distanceHeard, highwayMillis 11분 → "시속 70km 넘게 11분 달리셨어요." | HIGHWAY 12분~23분 "시속 70km 이상" |
| 4 | SEAT_HEAT | D − 2일 18:00 | 9분 | maxSetTemp 27 → "난방을 27°로 트셨어요." | HEAT 1분 "난방 27°" |
| 5 | FRONT_DEFROST | D − 1일 09:00 | 18분 | rainRecircMillis 8분 → "비가 오는 동안 내기 순환을 8분 켜 두셨어요." | RAIN 3분~15분 "와이퍼", RECIRC 4분 "내기 순환" |
| 6 | AMBIENT | D − 1일 18:00 | 16분 | nightMillis 12분 → "전조등을 켜고 12분 달리셨어요." | NIGHT 2분~14분 "전조등" |

**익힘**

| 기능 | 익힌 시각 | 메모 |
|---|---|---|
| AUTO_CLIMATE | 기록 2(오토 와이퍼) 운전 시작 + 3분 | "운전을 시작하고 3분 뒤" |
| SEAT_HEAT | 기록 5(김서림) 운전 시작 | "운전을 시작하자마자" |

기록 시각(at)은 P에 놓은 때(운전 끝)이므로 운전 시작은 `at − 운전 길이`다.

**`progress` 키**(수신기가 비운 뒤 쓴다)

| 키 | 값 | 화면에서 |
|---|---|---|
| `history` | `History.encode(records)` | 추천 기록 6개 |
| `shown_<기능>` | 기록 6개 기능마다 1. 나머지는 쓰지 않음(0) | 구역 카드 "안내함 1회" |
| `learned_<기능>` | 익힘 2개 true | 진행 원 2, "써 봄 ✓" |
| `learned_at_<기능>`, `learned_note_<기능>` | 위 익힘 표 | 구역 카드 "익힘 · 날짜", 확인 화면 |
| `last_pick` | AMBIENT(기록 6) | 첫 화면 = 앰비언트 라이트 안내 |
| `last_reason`, `last_marks`, `last_drive_ms` | 기록 6의 이유·근거·운전 길이 | 안내 화면 이유·근거 카드·"방금 끝난 운전 · 16분" |
| `last_confirmed` | 쓰지 않음 | 확인 띠 없음 |
| `last_used` | 빈 값 | |
| `drives` | 6 | |
| `first_drive` | 기록 1 시각 | 도감 완성 화면의 "첫 운전" |

- 불변식(`SeedTest`)
  - `History.decode(History.encode(records(now, zone)))`가 6개이고 값이 같다.
  - 기능이 서로 다르다.
  - 모든 기록이 `D − 3일 ≤ at < D`이고, 날짜가 셋이다.
  - 모든 기록의 이유 문장이 `f.reason(stats)`이고 `f.opportunity(stats)`가 참이다.
  - 모든 기록에 근거 표시가 1개 이상이고, 종류가 `f.evidence` 안에 있다.
  - `learned`가 모두 기록 기능 안에 있다.

## CarMapView 강조(위임 결정 V1·V5)

- `set(spots, pulse, sparkle, selected, dim, spotlightUntil = 0L)`. `spotlightUntil`은 uptime이다. 기본값이라 1~3차 부르는 곳은 그대로다.
- 강조 조건: `pulse != null && !dim && SystemClock.uptimeMillis() < spotlightUntil`
- `MainActivity`: `spotlightUntil = lastSpoken?.takeIf { state == TIP && it.feature == pick }?.at?.plus(10_000) ?: 0`
