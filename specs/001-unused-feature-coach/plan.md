# Implementation Plan: 안 써본 기능

**Branch**: `main` (git 저장소는 만들었지만 커밋은 하지 않음) | **Date**: 2026-11-08 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-unused-feature-coach/spec.md`

## Summary

포그라운드 서비스가 Pleos Vehicle SDK 신호(기어, 공조, 시트, 운전대, 전조등, 크루즈)를 구독해 한 번의 운전 동안 기능별 "기회"와 "사용"을 셉니다. 기어가 P가 되면 순수 Kotlin 판단 로직이 안내할 기능 하나와 새로 익힌 기능을 고르고, Pleos TTS로 한 문장을 말한 뒤 알림을 띄우고 SharedPreferences에 기록합니다. 앱 화면은 마지막 안내 카드와 기능 6개 상태를 보여 줍니다.

## Technical Context

**Language/Version**: Kotlin 2.0.21, JDK 17

**Primary Dependencies**: Pleos Vehicle SDK 2.3.2(`ai.pleos.playground:Vehicle`), Pleos TextToSpeech 3.3.1, AndroidX core-ktx. UI는 플랫폼 View(XML).

**Storage**: SharedPreferences(기기 안)

**Testing**: JUnit 4 (JVM 단위 테스트, `app/src/test`)

**Target Platform**: Pleos Connect 에뮬레이터 v2.1.0 (AAOS API 34, arm64), compileSdk 35, minSdk 28, targetSdk 34

**Project Type**: AAOS 차량용 안드로이드 앱 (단일 모듈)

**Performance Goals**: P 전환 후 3초 안에 안내 시작(SC-001)

**Constraints**: 주행 중 안내 0건, Vehicle SDK 조회 권한만 선언, 네트워크·서버 없음

**Scale/Scope**: 기능 6개, 화면 1개, 서비스 1개

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 원칙 | 판정 | 근거 |
|---|---|---|
| I. 정차 중에만 말한다 | 통과 | 안내는 `onDriveEnd`(기어 P 전환)에서만 호출. 주행 중에는 화면을 갱신할 이벤트가 없음 |
| II. 차 신호만 믿는다 | 통과 | 기회·사용 조건 6개 모두 SDK 2.3.2의 `register*` 구독 신호. 차속·시트 메모리는 신호가 없어 제외 |
| III. 최소 구현 | 통과 | Compose·Hilt·DataStore·DI 없음. 판단 로직은 파일 하나, 서비스 하나, 화면 하나 |
| IV. 판단 로직 테스트 | 통과 | `Coach`/`DriveStats`/`Feature`는 안드로이드 의존 없음. JUnit 테스트 1개 파일 |
| V. 기록은 차 안에만 | 통과 | SharedPreferences만. INTERNET 권한은 TTS SDK가 자체 선언(우리 코드는 전송 없음) |

설계 후 재확인(Phase 1 뒤): 변화 없음, 통과.

## Project Structure

### Documentation (this feature)

```text
specs/001-unused-feature-coach/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/ui.md
└── tasks.md
```

### Source Code (repository root)

```text
settings.gradle.kts
build.gradle.kts
gradle/libs.versions.toml
app/
├── build.gradle.kts
└── src/
    ├── main/
    │   ├── AndroidManifest.xml
    │   ├── java/kr/skku/unusedfeature/
    │   │   ├── Coach.kt          # Feature 6개, DriveStats, 고르기 로직 (순수 Kotlin)
    │   │   ├── CarWatcher.kt     # Pleos Vehicle SDK 구독 → DriveStats, 운전 시작·끝
    │   │   ├── WatchService.kt   # 포그라운드 서비스: CarWatcher + TTS + 알림 + Progress 저장
    │   │   ├── Progress.kt       # SharedPreferences 읽기·쓰기
    │   │   ├── MainActivity.kt   # 마지막 안내 카드 + 기능 목록
    │   │   └── BootReceiver.kt   # 부팅 후 서비스 시작
    │   └── res/ (layout/activity_main.xml, values/strings.xml, drawable/ic_notify.xml)
    └── test/java/kr/skku/unusedfeature/CoachTest.kt
```

**Structure Decision**: 단일 앱 모듈. 판단 로직(`Coach.kt`)만 안드로이드 의존 없이 분리해 JVM 테스트합니다. 설계 초안의 `DriveStats`/`Feature`/`Coach`는 서로만 쓰는 작은 타입이라 한 파일에 둡니다.

## Complexity Tracking

위반 없음.
