# Research: 안 써본 기능 7차 — 시연용 그림 바로잡기

## R1. 김서림 샘플 기록 고치기

- **Decision**: `app/src/debug/java/kr/skku/unusedfeature/Seed.kt`의 `items`에서 FRONT_DEFROST 항목(28~29행) 근거 끝에 `point(Kind.RECIRC, 12, "외기로 바꿈")`을 더한다. 통계 `rainRecircMillis = min(8)`, 운전 18분, 비 구간 `Mark(Kind.RAIN, min(3), min(15), "와이퍼")`, 4분 "내기 순환 켬"은 그대로다.
- **Rationale**:
  - 이유 문장은 `Feature.FRONT_DEFROST.reason`의 "비가 오는 동안 내기 순환을 ${rainRecircMillis / MINUTE}분 켜 두셨어요."다(Feature.kt 137행).
  - 실제 운전에서 이 값은 "비 && 내기 순환 켜짐" 구간의 누적이고, 구간 자체는 표시를 남기지 않는다(Coach.kt 131·175행 `rainRecirc = Span(null, "")`). 화면에는 비 막대와 내기 순환 점만 보인다.
  - 내기 순환 점 글자는 켤 때 "내기 순환 켬", 끌 때 "외기로 바꿈"이다(Coach.kt 276행 `Tracker.recirc`, CoachTest.kt 253행 `recirculationToggles`). 끈 점이 없으면 운전 끝(18분)까지 켠 것으로 읽혀 비 3~15분과 11분 겹친다. 12분에 끄면 4~12분, 8분이다.
  - 문장에 쓰지 않는 통계(내기 순환 바꾼 횟수 등)는 화면에 나오지 않아 맞추지 않는다.
  - `Seed.kt`는 디버그 소스셋이다. `app/build.gradle.kts`에 소스셋 설정이 없어 출시 APK에 들어가지 않는다(빌드 산출물 `Seed.class`도 `kotlin-classes/debug`에만 있다).
- **Alternatives considered**: 통계를 11분으로 바꾸기(이유 문장이 바뀜. 설계 U1은 문장을 두고 그림을 맞춘다), 비 구간을 4~12분으로 줄이기(막대 글이 "와이퍼 8분"으로 바뀌고, 내기 순환이 끝까지 켜진 것처럼 보이는 건 그대로).

## R2. 타임라인이 같은 종류 점 둘을 그리는가

- **Decision**: 그리기 코드(`Views.kt` `TimelineView`)를 고치지 않는다. 캡처로 확인한다(quickstart §3).
- **Rationale**:
  - 순간 점이 6개 이하면 점마다 원(반지름 20dp)·종류 아이콘·글자 "${elapsed(at)} · ${text}"를 그린다. 글자는 두 줄 중 앞 글자와 12dp 넘게 떨어지는 첫 줄에 놓고, 두 줄이 다 차면 글자만 뺀다(Views.kt 98~130행). 같은 종류라고 합치지 않는다.
  - 타임라인 폭은 카드 안쪽 폭이다. 가로 약 875dp(6차 verification 기록 카드 939dp − 안쪽 여백 64), 1:1 세로 약 695dp(6차 research R4). 점 자리 x = 28 + (폭 − 56) × 분 / 18이라 4분·12분 점은 가로 약 364dp, 세로 약 284dp 떨어진다.
  - 22sp 글자 "4분 · 내기 순환 켬"(약 172dp)과 "12분 · 외기로 바꿈"(약 179dp)이 한 줄에 같이 들어가려면 약 188dp면 된다. 두 화면 모두 첫 줄에 나란히 놓인다.
  - 막대 글 "와이퍼 12분"은 선 위(y − 24dp), 점 글자는 선 아래(첫 줄 바탕선 118dp)라 만나지 않는다. "출발"·"도착 18분"은 맨 아래(188dp)다.
  - 카드 오른쪽 숫자는 근거 종류가 구간(비)과 순간(내기 순환)을 섞어 6차처럼 비어 있다(Coach.kt 16~23행 `evidenceCount`).
- **Alternatives considered**: 내기 순환 켬~끔을 막대로 그리기(실제 운전 기록과 다르게 그려지고 화면 코드가 바뀜, spec FR-004).

## R3. 새 테스트 자리와 세는 법

- **Decision**: `app/src/testDebug/java/kr/skku/unusedfeature/SeedTest.kt`에 `@Test fun picturesMatchTheSentences()` 하나를 더한다. `Seed.records(now, Asia/Seoul)`의 기록마다 근거 표시에서 센 값과 `Seed.stats(pick)`의 값을 사람이 읽는 글("3번", "11분", "난방 27°")로 만들고, 기능 → 글 map 둘을 `assertEquals` 한 번으로 견준다. 실패 메시지에 어긋난 기록이 모두 보인다.

| 기록 | 근거 표시에서 센 값 | `Seed.stats` 값 |
|---|---|---|
| AUTO_CLIMATE · AUTO_WIPER | 순간 점 수 | `manualClimateChanges` · `wiperChanges` |
| CRUISE · AMBIENT | HIGHWAY · NIGHT 막대 길이 합 | `highwayMillis` · `nightMillis` |
| SEAT_HEAT | HEAT 점의 글 | "난방 ${deg(maxSetTemp)}°" |
| FRONT_DEFROST | "내기 순환 켬" 점부터 다음 "외기로 바꿈" 점(없으면 운전 끝 `driveMs`)까지와 RAIN 막대가 겹친 길이 | `rainRecircMillis` |
| 그 밖의 기능 | 실패(샘플을 더하면 세는 법도 정하게) | |

- **Rationale**:
  - `Seed`는 `app/src/debug`에만 있다. 기존 `SeedTest`도 `app/src/testDebug`에 있어 `testDebugUnitTest`에서만 컴파일된다(4차 구현 중 옮김). `app/src/test`는 출시용 단위 테스트(`testReleaseUnitTest`, `./gradlew test`)에도 컴파일되므로 거기 두면 컴파일이 깨진다.
  - 고치기 전 값: 공조 3 = 3, 와이퍼 4 = 4, 크루즈 11분 = 11분, 시트 열선 "난방 27°" = "난방 27°", 앰비언트 12분 = 12분, 김서림 11분 ≠ 8분. 김서림 하나만 실패해야 한다.
  - 문장 = `f.reason(Seed.stats(f))`는 기존 `reasonsAreTheAppsOwnSentences`가 이미 지킨다. 그래서 통계 값과 견주면 문장과 견주는 것과 같다.
  - 켬·바꿈은 글자로 짝짓는다. 샘플 글자가 실제 기록 글자(`recirculationToggles`가 지키는 "외기로 바꿈")와 달라지면 끈 점을 못 찾아 11분이 되어 실패한다(FR-002).
- **Alternatives considered**: 기능마다 테스트 6개(같은 틀 반복), 앱 코드에 "근거에서 통계 세기" 함수(앱이 쓰지 않는 코드, 헌법 III).

## R4. 확인 방법

- **Decision**: `/impeccable audit`는 돌리지 않고 김서림 다시 보기 캡처 두 장(가로, 1:1 세로)의 확인으로 갈음한다. 내부 확인용 주행 영상은 다시 녹화하지 않는다(가정, 2-1 승인 2026-11-15. 2-1과 두 영상의 뜻은 spec Assumptions).
- **Rationale**: 바뀌는 것은 디버그 샘플 데이터 한 줄이다. 화면 코드(`app/src/main`)가 그대로라 audit가 매기는 접근성·테마·성능·적응성이 바뀔 수 없다(6차 audit 16/20). 새로 생길 수 있는 문제는 점 글자 겹침 하나이고, 캡처가 그것을 바로 본다. 설계도 바뀐 그림은 캡처로 확인하고 제출 영상은 새로 찍는다고 추천한다.
- **Alternatives considered**: audit 돌리기(같은 점수, 마감 2026-11-15 23:59 전 시간만 듦), 주행 영상 다시 녹화(설계 "영향": 6차 영상과 달라지는 곳은 `seed`에서 김서림 기록을 열었을 때의 타임라인뿐).
