# 안 써본 기능 Constitution

## Core Principles

### I. 운전을 방해하지 않는다
주행 중에는 손 닿는 기능만, 한 운전에 두 번까지, 짧은 문구 하나로 권한다. 주행 중 화면은 움직이지 않는 카드 하나이고 누르기를 받지 않는다. 근거와 자세한 안내는 P에서만 한다. Pleos 주의분산 심사 기준을 넘는 기능은 만들지 않는다. (1.1.0: 5차 주행 중 한마디로 고침. 1.0.0은 "정차 중에만 말한다")

### II. 차 신호만 믿는다
기능을 "썼다/안 썼다", "기회가 있었다"는 판단은 Pleos Vehicle SDK가 실제로 주는 신호로만 한다. 신호를 받을 수 없으면 추측하지 않고 침묵한다. 틀린 안내보다 침묵이 낫다.

### III. 최소 구현 (ponytail)
YAGNI → 표준 라이브러리 → 플랫폼 기능 → 이미 있는 의존성 → 한 줄 → 최소 구현 순서로 고른다. 쓰이지 않는 추상화, 설정, 확장 지점을 만들지 않는다.

### IV. 판단 로직은 테스트로 지킨다
어떤 기능을 알릴지 정하는 로직은 안드로이드 없이 돌아가는 순수 코드로 두고, 그 로직마다 실패하면 깨지는 JUnit 테스트를 하나 이상 둔다.

### V. 기록은 차 안에만
운전 기록과 기능 사용 기록은 기기 안에만 저장한다. 서버, 계정, 외부 전송을 두지 않는다.

## Constraints

- 플랫폼: Pleos Connect (AAOS, API 34 에뮬레이터), Kotlin, JDK 17
- Vehicle SDK 권한은 실제로 쓰는 조회 권한만 선언한다(App Market 사전 승인 대상).
- API 키나 비밀값을 코드에 넣지 않는다.

## Development Workflow

brainstorming → /speckit-specify → /speckit-plan → /speckit-tasks → 실사용 변화 제시(승인 게이트) → /speckit-implement(ponytail full) → ponytail-review → /code-review.

## Governance

이 문서는 plan의 Constitution Check 기준이다. 원칙을 어기는 설계는 plan의 Complexity Tracking에 이유를 적어야 한다.

**Version**: 1.1.0 | **Ratified**: 2026-11-08 | **Last Amended**: 2026-11-12
