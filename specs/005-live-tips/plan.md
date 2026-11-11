# Implementation Plan: 안 써본 기능 5차 — 주행 중 한마디(주행 최적화)

**Branch**: `005-live-tips` (git 브랜치 `live-tips`, 되돌리기 태그 `backup/before-live-tips`) | **Date**: 2026-11-12 | **Spec**: [spec.md](./spec.md)

**Input**: `specs/005-live-tips/spec.md`, 설계 `docs/superpowers/specs/2026-11-12-live-tips-design.md`

## Summary

판단은 순수 코드에 더하고, 서비스가 운전 중 3초마다(지금 스위치 읽기 주기) 판단해 권함을 말하고 화면에 알린다. 주행 중 화면은 지금의 "운전 중" 화면 자리에서 카드 세 상태로 바뀐다.

| 기능 | 순수 코드(테스트) | 실행·화면 |
|---|---|---|
| 권함 고르기 | 새 `LiveCoach.next`(Coach.kt) | `WatchService.onChange`가 부른다 |
| 운전 중 누적값 | `Span.totalAt`, `Tracker.tick()`(비·비+내기 시간과 운전 시간을 stats에 지금까지 값으로 씀) | `CarWatcher.poll`(3초)이 `tick()` 뒤 `onChange()` |
| 방향지시등 | `Tracker.turnSignal(on)`, `signalOn` | `CarWatcher.watchTurnSignal`(DRIVE DNA와 같은 API) |
| 주행 중 문장 | `Feature.live`(16개, 2개는 null) | 음성 `speak(live)`, 카드 한 줄 |
| P에서 말하지 않기 | 없음(`Coach.finish`는 그대로) | `onDriveEnd`: 뽑힌 기능이 이번 운전 권함이면 말·알림 없이 `record(o, count = false)` |
| 안내 횟수 | 없음 | `Progress.countShown(f)`, `record(o, count)` |
| 주행 중 카드 | 없음 | `Ui.driving(recording, live, compact)` 세 상태, `Ui.where(w, blink = false)`, 세로 배치면 주행 중 지도 칸 숨김(research R8) |
| 기존 문구 | 없음 | contracts/ui.md §7 목록(첫 실행, 권할 것 없음, 상시 알림, 서비스 용도, 사용설명서, README) |
| 주행 최적화 | 없음 | 매니페스트 `distractionOptimized` |
| 헌법 | 없음 | `.specify/memory/constitution.md` 1.1.0 |
| 시연 | 없음 | `scripts/demo.sh live` |

## Technical Context

**Language/Version**: Kotlin(JDK 17), sh(시연). 1~4차와 같다
**Primary Dependencies**: 새 의존성 없음. Pleos Vehicle SDK의 방향지시등 API(이미 의존성에 있음)
**Storage**: SharedPreferences `progress`의 `shown_<기능>`만 더 쓴다. 권함 목록은 메모리(운전마다 새로)
**Testing**: 단위 테스트 48개(`CoachTest.kt` 45 + 디버그 `SeedTest.kt` 3) + 새 `LiveCoachTest.kt`
**Target Platform**: Pleos Connect 에뮬레이터(API 34, 2560×1440), 디버그 빌드(앱 시계 10배)
**Project Type**: 차량 앱(단일 모듈)
**Performance Goals**: 판단은 3초마다 기능 18개 조건 계산이라 무시할 만하다. 화면은 상태가 바뀔 때만 다시 그린다
**Constraints**
- 헌법 I(1.1.0으로 고침): 주행 중 화면은 움직이지 않고 누르기를 받지 않는다. 한 운전에 두 번까지 말한다.
- Pleos 주행 중 제한: 에뮬레이터 D에서 영상 금지·설정 금지(같은편 녹화 로그). 카드는 둘 다 지킨다.
- 앱은 백그라운드에서 화면을 띄우지 않는다.
**Scale/Scope**: 파일 7개 수정(Coach.kt, Feature.kt, CarWatcher.kt, WatchService.kt, Screens.kt, MainActivity.kt, Progress.kt), 매니페스트, 헌법, demo.sh, 새 테스트 1개

## Constitution Check

| 원칙 | 판정 |
|---|---|
| I. 정차 중에만 말한다 | **고침 필요**. 사용자 요청(11/12) 자체가 주행 중 권함이다. 1.1.0으로 "운전을 방해하지 않는다"로 고치고(FR-013), 고친 원칙의 규칙(손 닿는 기능, 두 번, 5분, 방향지시등, 움직임 없음, 누르기 없음)을 판단 코드와 테스트로 지킨다 |
| II. 차 신호만 믿는다 | 통과. 권함 조건은 P 안내와 같은 기회·사용 신호이고, 방향지시등도 차 신호다. 신호를 못 받으면 그 조건 없이 판단한다(침묵 아님: 방향지시등은 미루기 조건일 뿐이다) |
| III. 최소 구현 | 통과. 판단 주기는 지금 스위치 읽기(3초)를 쓰고 새 타이머를 두지 않는다. 화면은 지금 주행 화면 자리를 바꾸고 새 액티비티를 두지 않는다. 제한 플래그 구독 없이 기어로 판단한다 |
| IV. 판단 로직은 테스트로 | 통과. `LiveCoach.next`, `Span.totalAt`, `Tracker.tick`을 순수 코드로 두고 조건마다 테스트를 먼저 쓴다. P에서 말할지는 "권한 기능인가" 한 줄이라 테스트를 두지 않는다 |
| V. 기록은 차 안에만 | 통과. 안내 횟수만 기기 안에 늘리고, 권함 목록은 저장하지 않는다 |

**설계 뒤 다시 봄**: 위와 같다. 원칙 I 개정은 Complexity Tracking에 적는다.

## Project Structure

```text
specs/005-live-tips/  spec, plan, research(R1~R8), data-model, contracts/ui.md, quickstart, tasks
.specify/memory/constitution.md          # 1.1.0: 원칙 I
app/src/main/AndroidManifest.xml         # MainActivity distractionOptimized
app/src/main/java/kr/skku/unusedfeature/
├── Feature.kt       # live 문장
├── Coach.kt         # Span.totalAt, Tracker.elapsed·tick·turnSignal·signalOn, LiveCoach
├── CarWatcher.kt    # watchTurnSignal, poll에서 tick + onChange
├── WatchService.kt  # 권함 판단·발화·횟수, tips(companion), onDriveEnd에서 권한 기능이면 말하지 않음, 상시 알림 문구
├── Progress.kt      # countShown, record(o, count)
├── Screens.kt       # Ui.driving(recording, live) 세 상태, where(blink), 첫 실행·권할 것 없음 문구
└── MainActivity.kt  # 주행 중에도 권함 상태가 바뀌면 다시 그림, 세로 배치 주행 중 지도 칸 숨김
사용설명서.md, README.md   # contracts/ui.md §7 문구, README 5차 절
app/src/test/java/kr/skku/unusedfeature/LiveCoachTest.kt   # 새
scripts/demo.sh   # live
```

## 순서와 시험

1. 주행 최적화 시험(research R1·R2): 매니페스트 선언만 넣은 빌드로 에뮬레이터에서 확인한다. 결과가 FR-014 분기를 정한다.
2. 테스트 먼저(`LiveCoachTest`) → 순수 코드 → 서비스·신호 → 화면 → 시연 → 에뮬레이터 확인

## Complexity Tracking

| 위반 | 왜 필요한가 | 더 단순한 대안을 버린 까닭 |
|---|---|---|
| 헌법 원칙 I 개정(주행 중에 말함) | 사용자 요청(11/12)이 주행 중 실시간 추천이다 | 음성 없이 카드만: 사용자가 "화면 카드 + 음성"을 골랐다. 운전 중에는 화면보다 소리가 시선을 덜 뺏는다 |
