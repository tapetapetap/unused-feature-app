# Verification — 안 써본 기능 7차 시연용 그림 바로잡기

- 날짜: 2026-11-15, Pleos 에뮬레이터(2560×1440, density 160), 브랜치 `car-unused`(출발 af01903 = `backup/before-pictures-unused`)
- 캡처: `/Users/jiwon/Desktop/현대자동차 공모전/화면 점검 1115/안 써본 기능/`

## 테스트 먼저와 빌드 (SC-002, SC-003)

- 고치기 전(T003): 새 `SeedTest.picturesMatchTheSentences`만 실패했다. 나머지 56개는 통과했다.

  ```
  expected:<{AUTO_CLIMATE=3번, AUTO_WIPER=4번, CRUISE=11분, SEAT_HEAT=난방 27°, FRONT_DEFROST=8분, AMBIENT=12분}>
  but was:<{AUTO_CLIMATE=3번, AUTO_WIPER=4번, CRUISE=11분, SEAT_HEAT=난방 27°, FRONT_DEFROST=11분, AMBIENT=12분}>
  ```

  expected는 문장 통계이고 was는 그림에서 센 값이다. 김서림만 8분 대 11분으로 달랐다.
- 고친 뒤(T005): `./gradlew :app:testDebugUnitTest :app:assembleDebug` 통과, 테스트 57개, 실패 0.
- `git diff --stat backup/before-pictures-unused -- app/src/main`은 빈 출력이다. 출시 앱 코드, 디버그 매니페스트, `app/build.gradle.kts`도 그대로다.

## 에뮬레이터 (SC-001, FR-004)

`scripts/demo.sh seed` → "추천 기록 6" → 앞유리 김서림 제거(11월 14일 오전 9:00 운전 · 18분)

| 캡처 | 본 것 |
|---|---|
| `01-김서림-가로.png` | 비 막대 "와이퍼 12분" 안에 "4분 · 내기 순환 켬", "12분 · 외기로 바꿈" 두 점이 있다. 글자는 한 줄에 나란히 있고 막대 글, "출발", "도착 18분"과 겹치지 않는다. 이유는 "비가 오는 동안 내기 순환을 8분 켜 두셨어요.", 카드 오른쪽 숫자는 없다 |
| `02-김서림-세로.png` | 1:1 분할(앱 왼쪽 칸). 다시 보기가 그대로 남았다(설정 변화 보존). 두 점 글자가 한 줄에 겹치지 않는다 |

- 회귀: `seed` 직후 첫 화면은 앰비언트 라이트 안내였고 버튼은 "추천 기록 6  ›"였다. 다른 샘플 기록 다섯도 그대로였다.
- impeccable audit는 돌리지 않았다(2-1 결정, research R4). 화면 코드가 그대로이고, 바뀐 것은 이 기록의 점 하나라서 캡처로 확인했다.

## 리뷰

- ponytail-review: "Lean already. Ship." 반영한 것은 없다.
- /code-review(high): 출시 코드의 정확성 문제는 없었다. 지적 9건은 모두 새 테스트와 샘플 데이터의 견고성 쪽이고, 하나도 반영하지 않았다.
  - **지금 샘플에 없는 경우**(시작부터 내기 순환 켬, "켬" 두 번 연속, 1분보다 작은 차이): 이 테스트는 샘플 여섯을 위한 것이다. 그런 샘플이 생기면 테스트가 실패해 알려 준다.
  - **본 코드를 바꿔야 하는 것**(Coach.kt 글자를 상수로 빼서 견주기, `evidenceCount` 주석 고치기): 이번 차수는 `app/src/main` diff 0이 조건이다(SC-003). 실제 글자는 `CoachTest` 253행이 지킨다.
  - **샘플을 실제 Tracker로 만들기**(손으로 쓴 통계와 표시가 어긋날 수 있다는 구조 지적): 범위 밖이다. 이번 테스트가 그 어긋남을 잡는다.
  - **김서림 샘플 통계의 `recircToggles`·`rainMillis`가 0인 것**: 이유 문장에 쓰이지 않아 화면에 영향이 없다. 바꾸면 범위만 커진다.
  - **카드 숫자를 두 갈래에서 안 쓰는 것**: 한 기록당 한 번 세는 값이라 두었다.
- 보안 점검: `/security-review`는 작업 디렉터리가 git 저장소가 아니라 돌지 않는다. 같은 관점으로 diff를 직접 봤다.
  - 바뀐 곳은 디버그 소스셋 샘플 한 줄과 디버그 단위 테스트뿐이다.
  - 새 권한, 네트워크, 로그, 인텐트, 내보낸 구성 요소가 없다. 출시 APK에는 들어가지 않는다.

## 승인 뒤 바뀐 것

- 테스트의 "그림에서 센 값"은 다음처럼 센다.
  - 공조·와이퍼·크루즈·앰비언트: research R3처럼 손으로 세지 않고, 앱이 카드 오른쪽 숫자로 쓰는 `evidenceCount`를 그대로 불렀다. 값은 R3과 같다. 화면에 실제로 나오는 숫자를 견준다.
  - 시트 열선: 표시 글 "난방 27°" 전체를 견준다(research R3).
