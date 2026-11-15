# Implementation Plan: 안 써본 기능 7차 — 시연용 그림 바로잡기

**Branch**: `007-demo-pictures` (git 브랜치 `car-unused`, 되돌리기 태그 `backup/before-pictures-unused` = origin/main af01903) | **Date**: 2026-11-15 | **Spec**: [spec.md](./spec.md)

**Input**: `specs/007-demo-pictures/spec.md`, 설계 `docs/superpowers/specs/2026-11-14-demo-pictures-design.md`(U1)

## Summary

디버그 샘플의 김서림 기록에 점 하나를 더하고, 샘플 기록 6개의 그림과 문장을 견주는 테스트를 하나 더한다. 앱 코드(`app/src/main`)는 건드리지 않는다.

| 바꿀 것 | 파일 | 요구 |
|---|---|---|
| FRONT_DEFROST 항목 근거 끝에 `point(Kind.RECIRC, 12, "외기로 바꿈")`(research R1) | `app/src/debug/java/kr/skku/unusedfeature/Seed.kt`(`items`, 28~29행) | FR-001·002 |
| `@Test fun picturesMatchTheSentences()`(research R3 표) | `app/src/testDebug/java/kr/skku/unusedfeature/SeedTest.kt` | FR-003 |

## Technical Context

**Language/Version**: Kotlin(JDK 17). 1~6차와 같다

**Primary Dependencies**: 새 의존성 없음

**Storage**: 바뀌지 않음. 샘플은 `seed` 때 지금 저장 형식(`History.encode`)으로 쓴다

**Testing**: JUnit. 지금 56개(CoachTest 42, HistoryTest 3, LiveCoachTest 7, PickAreaTest 1, SeedTest 3) + 새 1개

**Target Platform**: Pleos Connect 에뮬레이터(AAOS API 34, 2560×1440, density 160), 디버그 빌드

**Project Type**: 차량용 안드로이드 앱(단일 모듈 `app`)

**Performance Goals**: 해당 없음

**Constraints**: 출시 앱과 화면 코드 그대로(spec FR-004). 제출 마감 2026-11-15 23:59

**Scale/Scope**: 디버그 샘플 한 줄, 테스트 하나

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| 원칙 | 판단 |
|---|---|
| I. 운전을 방해하지 않는다 | 해당 없음. 주행 중 화면과 권함이 그대로다 |
| II. 차 신호만 믿는다 | 해당 없음(신호 처리 그대로). 샘플 글자는 실제 기록 글자(`Tracker.recirc`의 "외기로 바꿈")와 같다 |
| III. 최소 구현 | 통과. 샘플 한 줄과 기존 테스트 파일의 테스트 하나다. 앱 코드에 세는 함수를 두지 않는다 |
| IV. 판단 로직은 테스트로 | 통과. 판단은 그대로이고 기존 테스트가 지킨다. 샘플의 그림·문장 맞춤을 새 테스트로 지킨다 |
| V. 기록은 차 안에만 | 해당 없음. 샘플은 디버그 빌드의 기기 안 저장소에만 쓴다(4차 그대로) |

Phase 1 뒤 다시 봐도 위반이 없다. Complexity Tracking은 비운다.

## Project Structure

### Documentation (this feature)

```text
specs/007-demo-pictures/
├── spec.md
├── plan.md              # 이 파일
├── research.md          # R1~R4
├── quickstart.md        # 테스트 먼저·빌드, 에뮬레이터 캡처
├── checklists/requirements.md
├── goldfish-review.md
├── verification.md      # T012
└── tasks.md             # /speckit-tasks
```

data-model.md와 contracts/는 만들지 않는다. 저장 형식과 화면 계약(4차 data-model Seed, 6차 contracts/ui.md)이 그대로이고 샘플 근거 표시 하나만 는다. 샘플 표 원본(`specs/004-visible-tips/data-model.md` 33행)은 6차처럼 고치지 않고, 바뀐 점은 이 폴더에 적는다.

### Source Code (repository root)

```text
app/src/debug/java/kr/skku/unusedfeature/Seed.kt          # FRONT_DEFROST 항목 근거
app/src/testDebug/java/kr/skku/unusedfeature/SeedTest.kt  # 새 테스트
```

**Structure Decision**: 1~6차 구조 그대로. 새 코드 파일은 없다. 테스트는 `app/src/test`가 아니라 기존 `SeedTest`가 있는 `app/src/testDebug`에 둔다(research R3).

## Phase 0: Research

[research.md](./research.md): R1 김서림 샘플 고치기, R2 타임라인이 같은 종류 점 둘을 그리는지, R3 새 테스트 자리와 세는 법, R4 확인 방법(impeccable audit 대신 캡처, 주행 영상 다시 녹화 안 함).

## Phase 1: Design

- [quickstart.md](./quickstart.md): 테스트 먼저(실패 확인), 빌드, 에뮬레이터 캡처(가로, 1:1 세로).
- 에이전트 컨텍스트 갱신: 이 저장소는 `CLAUDE.md`를 쓰지 않아 건너뛴다(1~6차와 같다).
