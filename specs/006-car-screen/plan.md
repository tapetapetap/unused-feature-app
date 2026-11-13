# Implementation Plan: 안 써본 기능 6차 — 차 화면 개선

**Branch**: `006-car-screen` (git 브랜치 `car-unused`, 되돌리기 태그 `backup/before-car-unused` = main 74f28fa) | **Date**: 2026-11-13 | **Spec**: [spec.md](./spec.md)

**Input**: `specs/006-car-screen/spec.md`, 설계 `docs/superpowers/specs/2026-11-13-car-screen-design.md`(C1~C5, U1~U5)

## Summary

화면 도우미(`Ui`)와 색 토큰을 먼저 바꿔 화면 대부분을 한 번에 맞추고, 그림 글자·뒤로 버튼·화면 상태 보존·문구는 해당 자리만 고친다. 판단 로직과 저장 형식은 건드리지 않는다.

| 바꿀 것 | 파일 | 요구 |
|---|---|---|
| `small` 24sp, `chip` 24sp, `cardTitle` 28sp, 20·22sp 고정 글자 → 24sp | `Screens.kt` | FR-001 |
| 누르는 칩 도우미 `Ui.action`(76dp, 가운데) — 닫기, ← 추천 기록 | `Screens.kt`, `MainActivity.kt` | FR-003·004 |
| "추천 기록 n ›" 76dp, `mapSub` 24sp | `map_head.xml`, `MainActivity.kt` | FR-001·003 |
| 세로 구역 줄 76dp, 범례 24sp | `MainActivity.kt` | FR-001·003 |
| 차 지도 이름표 22sp, 타임라인 글자 22sp, 그림 안 파랑 글자 → `blue_text` | `Views.kt` | FR-002·005 |
| 누를 곳 버튼 그림 24sp | `Screens.kt`(`where`) | FR-002 |
| `text3` 낮 #62656D, 새 `blue_text`, `green_text` 낮 #017A40 | `values/colors.xml`, `values-night/colors.xml` | FR-005 |
| 각주 `text4` → `text3`, 연한 바탕 위 초록·파랑 글자 → `green_text`·`blue_text` | `Screens.kt`, `MainActivity.kt` | FR-005 |
| 뒤로 버튼 | `MainActivity.onBackPressed` | FR-006 |
| 설정 변화 때 열린 칸 보존 | `MainActivity`(`onSaveInstanceState`, `onCreate`, `onPause`) | FR-007 |
| 주행 중 기다림 문구 | `Screens.kt`(`driving`) | FR-008 |
| 근거 카드 숫자 규칙 | 새 순수 함수 `evidenceCount`(`Coach.kt`) + 테스트, `Screens.kt`(`tip`) | FR-009 |
| 앞유리 김서림 제목, 내기 순환 표시 글자 | `Feature.kt`, `Coach.kt`(`Tracker.recirc`) | FR-010 |
| 기능 없는 구역 이름표·세로 줄·구역 카드 문구 | `Views.kt`(`drawLabel`), `MainActivity.zoneRow`, `Screens.kt`(`zone`) | FR-011 |
| 세로 구역 줄 사이·추천 기록 카드 사이 24dp | `MainActivity.kt`(`render`의 `list.addView(…, lp(bottom = 12))`), `Screens.kt`(`history`) | FR-004 |
| 차 지도 이름표를 뷰 안으로(양쪽), 점을 덮으면 위로 | `Views.kt`(`drawLabel`) | FR-002 |
| 세로 화면 칩 격자 2열 | `Screens.kt`(`Ui.grid`) | spec Edge Cases |
| 디버그 샘플 기록의 내기 순환 글자 | `app/src/debug/.../Seed.kt` | FR-010(시연이 새 글자를 보이게) |

## Technical Context

**Language/Version**: Kotlin(JDK 17), Android View. 1~5차와 같다

**Primary Dependencies**: 새 의존성 없음

**Storage**: 바뀌지 않음. 열린 칸 상태는 설정 변화 동안만 `Bundle`(onSaveInstanceState)에 둔다

**Testing**: 기존 단위 테스트(`CoachTest.kt` 45, `LiveCoachTest.kt` 7, 디버그 `SeedTest`) + `CoachTest`에 근거 카드 숫자 규칙 테스트 1개

**Target Platform**: Pleos Connect 에뮬레이터(AAOS API 34, 2560×1440, density 160)

**Project Type**: 차량용 안드로이드 앱(단일 모듈 `app`)

**Performance Goals**: 바뀌지 않음(주행 중 다시 그리기 규칙 5차 그대로)

**Constraints**: 앱 칸 가로 약 1,700dp(전체), 세로 1:1 분할 약 850dp 폭. 차 기준 76dp·24sp·4.5:1(spec 용어)

**Scale/Scope**: 화면 코드 3개 파일(Screens.kt, MainActivity.kt, Views.kt), 색 2개 파일, 레이아웃 1개, 순수 코드 2개 파일(Coach.kt, Feature.kt), 디버그 샘플 1개(Seed.kt)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 원칙 | 판단 |
|---|---|
| I. 운전을 방해하지 않는다 | 통과. 주행 중 카드는 글자 크기만 커지고, 움직임·누르기 규칙은 그대로다 |
| II. 차 신호만 믿는다 | 해당 없음(신호 처리 그대로) |
| III. 최소 구현 | 통과. 도우미와 토큰을 바꿔 고칠 곳을 줄이고, 새 의존성·추상화가 없다. 누르는 칩 도우미 하나(`action`)만 더한다(쓰는 곳 3곳 이상) |
| IV. 판단 로직은 테스트로 | 통과. 새 판단(근거 카드 숫자 규칙)은 순수 함수와 테스트로 둔다 |
| V. 기록은 차 안에만 | 해당 없음(저장 형식 그대로) |

Phase 1 뒤 다시 봐도 위반이 없다. Complexity Tracking은 비운다.

## Project Structure

### Documentation (this feature)

```text
specs/006-car-screen/
├── spec.md
├── plan.md              # 이 파일
├── research.md          # R1~R5
├── contracts/ui.md      # 요소별 글자·칸·색·문구 값
├── quickstart.md        # 에뮬레이터 확인 순서
├── checklists/requirements.md
└── tasks.md             # /speckit-tasks
```

data-model.md는 만들지 않는다. 저장 데이터가 바뀌지 않고, 설정 변화 동안 보존할 화면 상태는 contracts/ui.md §5에 적는다.

### Source Code (repository root)

```text
app/src/main/
├── java/kr/skku/unusedfeature/
│   ├── Screens.kt        # Ui 도우미, 화면 조립(driving·tip·whereCard·history·zone·none·confirm·complete)
│   ├── MainActivity.kt   # 추천 기록 버튼, 범례, 세로 구역 줄, 뒤로, 상태 보존
│   ├── Views.kt          # TimelineView, CarMapView 글자
│   ├── Coach.kt          # Tracker.recirc 표시 글자, evidenceCount(새)
│   └── Feature.kt        # FRONT_DEFROST.evidenceTitle
├── res/values/colors.xml, res/values-night/colors.xml
└── res/layout/map_head.xml
app/src/test/java/kr/skku/unusedfeature/CoachTest.kt
```

**Structure Decision**: 1~5차 구조 그대로. 새 파일은 없다.

## Phase 0: Research

[research.md](./research.md): R1 차 기준 값, R2 설정 변화 때 상태 보존 방법, R3 근거 카드 숫자 규칙, R4 세로 칸 폭 안에 커진 요소가 들어가는지, R5 색 값.

## Phase 1: Design

- [contracts/ui.md](./contracts/ui.md): 공통 도우미 값(§1), 요소별 값(§2), 색(§3), 문구(§4), 뒤로·상태 보존(§5).
- [quickstart.md](./quickstart.md): 빌드·테스트, 화면별 캡처와 UI 덤프, 뒤로·낮밤 확인, 장면 회귀.
- 에이전트 컨텍스트 갱신: 이 저장소는 `CLAUDE.md`를 쓰지 않아 건너뛴다(1~5차와 같다).
