# Verification: 안 써본 기능 (2026-11-08)

환경: MacBook Pro(Apple M5 Pro), Pleos Connect Emulator v2.1.0 arm64(API 34, 직접 다운로드한 시스템 이미지), JDK 17, AGP 8.8.2.
차 신호는 Virtual Console HTTP(`PUT http://127.0.0.1:8088/vehicle/...`)로 넣었습니다. 스크립트: `scripts/demo.sh` (climate·learn·multi).

## 자동 테스트

| 명령 | 결과 |
|---|---|
| `./gradlew :app:testDebugUnitTest` | `CoachTest` 16개 + `PickSeatTest` 1개, 실패 0 |
| `./gradlew :app:assembleDebug` | 성공, `app-debug.apk` 약 28MB |

## 에뮬레이터 확인

| 시나리오 | 기대 | 결과 | 근거 |
|---|---|---|---|
| 처음 설치 후 앱 열기 (US3-1) | 기능 6개 "아직", 빈 카드 문구 | 통과 | 설치 직후 화면 |
| D → 풍량 2→4→5, 온도 23 → 61초 → P (US1-1) | 공조 AUTO 음성·알림·카드, "안내함 1회" | 통과 | TTS 로그 `doSpeak … 공조 AUTO를 켜 보세요`, `docs/screenshots/1-auto-climate-tip.png` |
| 주행 중 조작 (US1-4, SC-002) | P 전 음성·알림 없음 | 통과 | 61초 동안 `doSpeak` 로그 없음, P 직후 1회 |
| 다음 운전에 AUTO 켬 → P (US2-1) | "공조 AUTO, 써 보셨네요", 목록 "익힘" | 통과 | `docs/screenshots/4-tried-while-parked.png` (같은 확인 카드. 처음 찍은 화면은 고치기 전 문구라 지움) |
| 조수석 착좌 + 온도 변경 + 시트 열선 + 전조등 수동 2회, 같은 운전 3번 반복 | 한 번에 하나씩, 안내 횟수 적은 것부터 | 통과 | 동승석 개별 온도 → 운전대 열선 → (수정 후) 오토 라이트 순서로 음성 |
| 안내 알림 | 채널 tip, 중요도 높음 | 통과 | `dumpsys notification`: `id=2 channel=tip importance=4` |
| 크루즈 사용 신호 | ACTIVATED에서만 익힘 | 통과 | VC `full_autonomous` → SDK `ACTIVATED` → 익힘. `ready`는 `ENABLED`라 익힘 아님 |
| 크루즈 기회(20분 운전) | — | 단위 테스트로만 확인 | 에뮬레이터에서 20분 운전은 돌리지 않음 |
| 안내 직후 정차 중에 AUTO를 눌러 봄 | 카드가 바로 확인 문장으로 바뀜, 목록 "익힘" | 통과 | `docs/screenshots/4-tried-while-parked.png` |
| 부팅 시 자동 시작 (FR-009) | 앱을 열지 않아도 감시 서비스가 켜짐 | 통과 (11-08 아침) | 앱 강제 종료 → 에뮬레이터 cold boot → `dumpsys activity services`: `WatchService isForeground=true` |

## 검증 중 발견해 고친 것

1. **크루즈가 설치 직후 바로 "익힘"이 됨**: 에뮬레이터 기본 상태가 `ENABLED`(켜 두고 대기)였다. `ACTIVATED`(속도 유지 중)만 사용으로 보도록 고쳤다.
2. **공조가 꺼져 있으면 "설정 온도 신호 없음"이 계속 표시됨**: 현재값 읽기 실패는 일시적이라 로그만 남기고, 구독 자체가 실패할 때만 화면에 알리도록 바꿨다.
3. **이 차는 통풍 시트를 "시트 온도 음수"로 표현**: Virtual Console 값 정의(-3~-1 = Ventilation)를 보고, 시트 온도가 음수여도 통풍 사용으로 본다.
4. **시트 열선을 켠 운전에서 통풍 시트를 권함**: 열선을 켠 운전은 통풍 기회에서 뺐다.
5. **운전했는데 놓친 기능이 없을 때 "첫 운전이 끝나면…" 문구가 남음**: "이번 운전에서는 따로 알려 드릴 기능이 없었어요."로 바꿨다.
6. **확인 문장의 조사**: "공조 AUTO을(를)" → "공조 AUTO, 써 보셨네요."

## 확인한 SDK 사실 (다음 작업에 쓸 것)

- Virtual Console을 스크립트로 쓰려면 `POST /browser/session?client=<id>`를 3초 안쪽 간격으로 계속 보내야 한다. 안 보내면 `{"error":"emulator_inactive"}`.
- 에뮬레이터는 user 빌드라 `cmd car_service` 속성 주입이 막혀 있다.
- VC `ad_mode_state`의 `lateral`·`longitudinal`은 `CRUISE_CONTROL_STATE`를 바꾸지 않았다. `full_autonomous` → 2(ACTIVATED), `ready` → 1(ENABLED), `not_ready` → -6(SDK가 null로 전달).
- 공조가 꺼져 있으면 `HVAC_TEMPERATURE_SET` 읽기가 `PropertyNotAvailableException`.

## 리뷰 반영 (ponytail-review → /code-review high)

**ponytail-review** (과설계): 3건 반영.
- `vehicle-data` 직접 의존 삭제(Vehicle이 끌어옴)
- 시연 스크립트 2개를 `scripts/demo.sh` 하나로 합침
- Tracker 필드 1개 정리

**/code-review high** (정확성): 10건 중 10건 반영 또는 해소.

| # | 지적 | 조치 |
|---|---|---|
| 1 | SDK 콜백(백그라운드 스레드)에서 구독 해제 목록을 바꿈, stop 뒤 늦은 구독 누수 | 구독을 메인 스레드로 옮기고(`onSeat`), `stopped` 표시 뒤에는 아무것도 하지 않음 |
| 2 | 조수석 착좌만 초기값을 안 읽음 | `register*`가 구독 직후 현재값을 보내 주는 것을 에뮬레이터 로그로 확인하고 `get*` 호출을 모두 뺌 |
| 3 | 안내 직후 정차 중에 써 보면 "써 보셨네요"가 영영 안 나옴 | 비교 기준을 "운전 시작 시 익힘"에서 "지난 운전 끝 시 익힘"으로 바꿈. 회귀 테스트 추가 |
| 4 | AUTO 신호가 없는 차에서 AUTO를 계속 권함(확인 불가) | 사용 신호를 한 번도 못 받은 기능은 안내하지 않음(`DriveStats.heard`). 회귀 테스트 추가 |
| 5 | 차 연결 실패 후 재시도 없음 | 10초 뒤 재시도, 연결되면 상태 줄의 실패 문구 제거 |
| 6 | 일시 실패가 상태 줄에 영구히 남음 | 구독 중 실패는 로그만, 기능 미지원(capability 실패)만 화면에 표시 |
| 7 | 초기값 읽기와 구독 이벤트 순서가 뒤바뀌어 가짜 변화가 생김. 전조등 스위치 비동기 읽기도 순서 문제 | `get*` 제거로 해소. 전조등은 상태 변화를 들어온 순서대로 반영하고, 스위치 값은 다음 변화 판단에 씀. 스위치를 못 읽었으면 세지 않음(테스트 추가) |
| 8 | 이미 익힌 기능을 카드가 계속 권함 | 마지막 안내가 익힘이면 카드를 확인 문장으로 바꿈 |
| 9 | `onCreate` 실패 시 `onDestroy`에서 lateinit 크래시, stop 뒤 대기 중 콜백 실행 | `isInitialized` 확인, `removeCallbacksAndMessages` |
| 10 | 구독 블록 11개 복붙 | 구독 해제·스레드 처리를 `onSeat` 한 곳으로 모음. 리스너 인터페이스가 신호마다 달라 블록 자체는 남음 |
