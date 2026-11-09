# Implementation Plan: 안 써본 기능 2차 — 보이는 화면과 기능 18개

**Branch**: `002-redesign-21-features` | **Date**: 2026-11-08 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/002-redesign-21-features/spec.md`

## Summary

권할 기능을 18개로 늘리고(새 차 신호 구독 + 와이퍼 스위치 3초 주기 읽기 + 주행거리로 고속 구간 계산. 처음 계획한 표시등 읽기는 내부 전용 권한이 필요해 구현 중 빼고 그 기능 3개도 뺌), 운전 중 근거를 시각과 함께 기록한다. 화면은 시안대로 왼쪽 차 지도(7구역)·진행 원, 오른쪽 상황 화면(첫 실행/주행 중/안내/확인/권할 것 없음/도감 완성/구역 카드)으로 다시 만든다. 라이트·다크, 가로·세로를 지원하고 아이콘을 새로 그린다. 시연용(디버그) 빌드는 앱 시계를 10배 빠르게 한다.

## Technical Context

**Language/Version**: Kotlin 2.0.21, JDK 17

**Primary Dependencies**: Pleos Vehicle SDK 2.3.2, Pleos TextToSpeech 3.3.1, androidx.core-ktx 1.13.1 (새 의존성 없음)

**Storage**: SharedPreferences "progress" (기기 안)

**Testing**: JUnit 4 (`app/src/test`), Pleos Connect 에뮬레이터 + Virtual Console(`scripts/demo.sh`)

**Target Platform**: Pleos Connect (AAOS, API 34 에뮬레이터), minSdk 28, 160dpi, 앱 창 1682×1210dp

**Project Type**: Android 차량 앱(단일 모듈 `app`)

**Performance Goals**: P 전환 뒤 3초 안에 안내(SC-005). 지도 깜빡임 60fps를 해치지 않는 단순 Canvas 그리기

**Constraints**: 주행 중 안내 0건(원칙 I), 조회 권한만(내부 전용 권한 금지), 새 의존성 없음(원칙 III), 기록은 기기 안(원칙 V)

**Scale/Scope**: 기능 18개, 구역 7개, 상황 화면 7종 × 가로·세로 × 라이트·다크

## Constitution Check

| 원칙 | 판정 | 근거 |
|---|---|---|
| I. 정차 중에만 말한다 | 통과 | 음성·알림은 1차처럼 P 전환 때만. 주행 중 화면은 안내가 아닌 고정 문구이고 운전이 끝날 때까지 바뀌지 않으며 누르기를 막는다(FR-010) |
| II. 차 신호만 믿는다 | 통과 | 18개 모두 SDK 신호로 기회·사용을 판단(research R1). 신호를 받은 적 없는 기능은 권하지 않는다. 진행 원 분모에서는 차에 없다고 확인된 기능만 뺀다(data-model "지원 기능") |
| III. 최소 구현 | 통과 | 새 의존성 없음. Compose 대신 View + Canvas View 3개(R9). 글꼴은 시스템 것(R6). 21개 버튼 그림은 한 가지 틀(자리 + 버튼 줄)로 해결 |
| IV. 판단 로직은 테스트 | 통과 | 기회·사용·근거·고속 계산·근거 저장 형식은 안드로이드 없는 `Coach.kt`/`Feature.kt`에 두고 가짜 시계로 테스트 |
| V. 기록은 차 안에만 | 통과 | 새 기록도 SharedPreferences. 외부 전송 없음 |
| Constraints: 조회 권한만 | 통과 | 새 권한 `CAR_WINDOWS`, `CAR_MIRRORS` 모두 조회. 표시등은 읽기에도 `PRIVATE_CAR_SAFETY`가 필요해 쓰지 않음(R2) |

설계 후 재확인: data-model과 contracts에서 위 판정이 바뀌는 항목 없음.

## Project Structure

### Documentation (this feature)

```text
specs/002-redesign-21-features/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/ui.md
├── checklists/requirements.md
└── tasks.md            # /speckit-tasks
```

### Source Code (repository root)

```text
app/src/main/
├── AndroidManifest.xml          # 권한 2개 추가, 적응형 앱 아이콘
├── java/kr/skku/unusedfeature/
│   ├── Feature.kt               # 새로: Zone, Feature(18), Where, Kind
│   ├── Coach.kt                 # DriveStats, Mark, Span, Tracker(시계 주입), Outcome, Coach, 근거 저장 형식
│   ├── CarWatcher.kt            # 새 신호 구독 + 와이퍼 스위치 3초 주기 읽기 + 앱 시계(10배)
│   ├── WatchService.kt          # 주행 중 여부, 지원 안 되는 기능, 처음 쓴 순간, 결과 저장
│   ├── Progress.kt              # 새 키(data-model)
│   ├── MainActivity.kt          # 상황 판단, 지도·구역 연결
│   ├── Screens.kt               # 새로: 상황 화면·구역 카드를 코드로 조립
│   ├── Views.kt                 # 새로: CarMapView, TimelineView, RingView
│   └── BootReceiver.kt          # 그대로
├── res/
│   ├── values/colors.xml, values-night/colors.xml   # Pleos 토큰
│   ├── layout/activity_main.xml, map_head.xml       # 가로: 지도 | 상황 화면 (3:5)
│   ├── layout-port/activity_main.xml                # 세로: 상황 화면 / 지도 + 구역 목록
│   ├── drawable/ic_f_*.xml(18), ic_z_*.xml(구역), ic_k_*.xml(근거), car_top.xml, ic_launcher_*.xml, ic_notify.xml
│   └── mipmap-anydpi-v26/ic_launcher.xml
app/src/test/java/kr/skku/unusedfeature/CoachTest.kt  # 18개 기능 + 시각 판단 + 저장 형식
scripts/demo.sh                                        # 시나리오 추가(quickstart 표)
```

**Structure Decision**: 1차의 단일 모듈을 유지한다. 판단(순수 Kotlin)과 화면(Android)을 파일로 나눠 테스트 경계를 지킨다. 기능 표가 커져 `Feature.kt`를 `Coach.kt`에서 떼어 낸다.

## 핵심 설계

1. **시계**: `Tracker(clock: () -> Long)`. 서비스는 `elapsedRealtime() × k`(디버그 k=10)를 넘기고 테스트는 가짜 시계를 넘긴다.
2. **used/heard**: 1차의 기능별 Boolean을 `DriveStats.used`, `heard` 집합으로 바꿔 21개를 같은 방식으로 다룬다. 신호 하나가 기능 여럿의 근거가 될 수 있다(예: 와이퍼 → 뒷유리·미러·김서림).
3. **구간(Span)**: 비, 비+내기, 밤, 고속을 켜짐 시각과 누적 시간, 근거 막대로 함께 관리하는 작은 클래스 하나.
4. **주기 읽기**: 운전이 시작되면 메인 스레드 Handler로 3초마다 와이퍼·전조등·하이빔 스위치를 읽고, 운전이 끝나면 멈춘다(정차 중에는 읽지 않음).
5. **결과**: `Coach.finish`가 `Outcome(pick, confirmed, reason, marks, used, driveMillis)`를 돌려주고 서비스는 저장·발화만 한다.
6. **화면**: XML은 뼈대(지도 칸, 상황 칸)만 두고 상황 화면은 `Screens.kt`의 함수들이 만든다. 색은 color 리소스라 다크에서 액티비티가 다시 만들어질 때 자동으로 바뀐다.
7. **지도**: `CarMapView`가 `car_top` 벡터를 그리고 구역 7개를 좌표(data-model)에 원·아이콘·진행 링·이름표로 그린다. 파랑 깜빡임은 무한 반복, 초록 반짝임은 3번. 누르면 구역 콜백.

## Complexity Tracking

헌법 위반 없음.
