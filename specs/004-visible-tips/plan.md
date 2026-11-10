# Implementation Plan: 안 써본 기능 4차 — 음성 자막, 기록 카드 지도, 눈에 띄는 추천 구역

**Branch**: `004-visible-tips` (git 브랜치 `main`) | **Date**: 2026-11-11 | **Spec**: [spec.md](./spec.md)

**Input**: `specs/004-visible-tips/spec.md`, 설계 `docs/superpowers/specs/2026-11-11-visible-tips-design.md`

## Summary

판단(`Coach.kt`)은 그대로 두고 표현과 시연 데이터를 더한다.

| 기능 | 순수 코드(테스트) | 실행·화면 |
|---|---|---|
| 음성 자막 | 없음(문장은 `Feature.speech`) | `WatchService.lastSpoken`(기능·시각·말했는지), `Ui.caption`, `SoundBars`(Views.kt), `ic_speaker.xml` |
| 기록 카드 미니 지도 | 없음 | `ZoneThumb`(Views.kt), `Ui.history` 카드 오른쪽 |
| 추천 구역 10초 강조 | 없음 | `CarMapView.set(..., spotlightUntil)`, `MainActivity`가 가장 최근 기록 시각 + 10초를 준다 |
| 샘플 상태 | `Seed.records(now, zone)`, `Seed.learned`, `Seed.learnedAt`(Coach.kt 옆 순수) | `app/src/debug` `SeedReceiver`(DUMP 보호), `scripts/demo.sh seed` |

## Technical Context

**Language/Version**: Kotlin(JDK 17), sh(시연). 1~3차와 같다
**Primary Dependencies**: 새 의존성 없음
**Storage**: 1~3차 SharedPreferences `progress` 그대로(샘플도 같은 키와 `History.encode`)
**Testing**: JUnit `CoachTest.kt`(45개) + 새 `SeedTest.kt`
**Target Platform**: Pleos Connect 에뮬레이터(API 34), 디버그 빌드(앱 시계 10배)
**Project Type**: 차량 앱(단일 모듈)
**Constraints**: 헌법 I(정차 중에만 말한다: 자막은 P 화면에만), V(기록은 차 안에만: 샘플도 기기 안)

## Constitution Check

| 원칙 | 판정 |
|---|---|
| I. 정차 중에만 말한다 | 통과. 말하는 시점과 횟수는 그대로다. 자막·강조는 P 화면에만 있다 |
| II. 차 신호만 믿는다 | 통과. 판단을 바꾸지 않는다 |
| III. 최소 구현 | 통과. 새 그리기 View 두 개(`SoundBars`, `ZoneThumb`)는 기존 `Views.kt`에, 디버그 수신기 하나를 더한다. 버튼 깜빡임은 이미 있어 손대지 않는다 |
| IV. 판단 로직은 테스트로 | 통과. 샘플 상태 만들기(`Seed`)는 순수 함수로 두고 `SeedTest`를 먼저 쓴다. 판단 테스트 45개는 그대로다 |
| V. 기록은 차 안에만 | 통과. 샘플은 디버그 빌드에서 기기 안 저장소에만 쓴다 |

## Project Structure

```text
specs/004-visible-tips/  spec, plan, research(R1~R4), data-model, contracts/ui.md, quickstart, tasks
app/src/main/java/kr/skku/unusedfeature/
├── WatchService.kt   # lastSpoken
├── Views.kt          # SoundBars, ZoneThumb, CarMapView 강조
├── Screens.kt        # caption, tip·record 자막, history 미니 지도
├── MainActivity.kt   # 새 안내 시각 → 지도 강조, 다시 보기 자막
└── Seed.kt           # 새: 순수 샘플 상태
app/src/main/res/drawable/ic_speaker.xml   # 새: 자막 스피커 아이콘(Material Icons volume_up)
app/src/debug/AndroidManifest.xml, app/src/debug/java/kr/skku/unusedfeature/SeedReceiver.kt   # 새
app/src/test/java/kr/skku/unusedfeature/SeedTest.kt   # 새
scripts/demo.sh   # seed
```

## Complexity Tracking

위반 없음.
