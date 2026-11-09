# Research: 안 써본 기능 2차

모든 사실은 2026-11-08에 직접 확인했다. SDK는 Gradle 캐시의 `Vehicle-2.3.2.aar`를 `javap`로 열어 봤고, 에뮬레이터는 Pleos Connect v2.1.0과 그 Virtual Console(`GET /vehicle/catalog`)을 썼다.

## R1. 새 기능의 사용 신호와 권한

| 신호 | SDK 함수 | 권한 | 에뮬레이터 콘솔 |
|---|---|---|---|
| 브레이크 페달 | `Brake.registerBrakePedalStatus(listener)` → `onBrakePedalStatusUpdated(Boolean?)` | `CAR_DRIVE` | `brake-pedal-status` (`PUT /vehicle/controls?brake=0..100`). 구현 중 오토 홀드·i-PEDAL을 빼며 쓰지 않음 |
| 와이퍼 동작 | `Wiper.registerWiperState(WindowArea, listener)` → `onWiperStateUpdated(WiperState?)` | `CAR_WINDOWS` | 없음(스위치만) |
| 와이퍼 스위치 | `Wiper.getCurrentWiperSwitch(WindowArea, cb, err)` (구독 함수 없음) | `CAR_WINDOWS` | `windshield-wipers-switch` (`PUT /vehicle/body?front_wiper=Off|Auto|Low|High`) |
| 하이빔 | `Light.registerHighBeamLightState(listener)`, `getHighBeamLightSwitch(cb, err)` | `CAR_LIGHTS` | `high-beam-lights` (`PUT /vehicle/lights?feature=high_beam&value=true`) |
| 앰비언트 | `Light.registerAmbientLightStateListener(SeatArea, listener)` | `CAR_LIGHTS` | `ambient-light-switch` (`PUT /vehicle/body?ambient_mode=On`) |
| 제상(앞/뒤) | `HVAC.registerHvacDefrosterState(WindowArea, listener)` | `CAR_CLIMATE` | `hvac-defroster` (`front_defroster`, `rear_defroster`) |
| 내기 순환 | `HVAC.registerHvacRecirculationState(SeatArea, listener)` | `CAR_CLIMATE` | `hvac-recirc` (`recirc=true`) |
| 오토 내/외기 | `HVAC.registerHvacAutoRecirculationState(SeatArea, listener)` | `CAR_CLIMATE` | 없음 |
| MAX A/C | `HVAC.registerHvacMaxAcState(SeatArea, listener)` | `CAR_CLIMATE` | 없음 |
| 미러 열선 | `SideMirror.registerHvacSideMirrorHeatState(MirrorArea, listener)` | `CAR_CLIMATE`(구독) | 없음 |
| 주행 모드 | `DrivingMode.registerDrivingModeState(listener)` → `DrivingModeType` NORMAL/ECO/SPORT | `CAR_DRIVE` | 없음 |
| 주행거리 | `Odometer.registerDistanceDriven(listener)` → `onDistanceDrivenUpdated(Float?)` km | `CAR_DRIVE` | `perf-odometer` (`PUT /vehicle/odometer?traveled_distance_km=`) |
| 조수석·뒷좌석 시트 열선 | `Seat.registerHvacSeatTemperature(SeatArea, …)` (음수 = 통풍) | `CAR_SEATS` | 조수석만 (`passenger_seat`) |
| 뒷좌석 착좌 | `Seat.registerSeatOccupancy(SeatArea, …)` | `CAR_SEATS` | `rear_left`, `rear_right` |
| 계기판 표시등 | `Safety.getTelltalesInfo(cb, err)` → `Telltales.telltales: List<TelltaleData(type, state)>` | 인터페이스 표기 `CAR_SAFETY`, **구현(`SafetyImpl`)은 `PRIVATE_CAR_SAFETY`** — 구현 중 확인, 쓰지 않음 | `telltales` (`PUT /vehicle/telltales/simulator?type=AutoHold&state=On&color=Red`). AutoHold·AutoHighBeam 있음, LFA·I_PEDAL 없음 |

- `HvacRecirculationStateListener` 하나가 내기 순환과 오토 내/외기 둘 다에 쓰이고 콜백 이름은 `onHvacAutoRecirculationStateUpdated`다.
- 이미 쓰던 권한: `CAR_INFO`, `CAR_DRIVE`, `CAR_CLIMATE`, `CAR_SEATS`, `CAR_LIGHTS`. 새로 선언: `CAR_WINDOWS`, `CAR_MIRRORS`(`CAR_SAFETY`는 구현 중 뺌). 모두 조회 권한(헌법 Constraints).

## R2. 표시등은 쓰지 않는다 (구현 중 바뀜)

- 구현 중 확인: `SafetyImpl`의 `checkTelltalesCapabilities`, `getTelltalesInfo`, `registerTelltales` 모두 `PRIVATE_CAR_SAFETY`를 요구한다(`javap -v`). 이 권한의 보호 수준은 internal|role이라 외부 앱은 받을 수 없다(`pm list permissions -f`). 에뮬레이터에서 `SecurityException: getTelltalesInfo need Permission`. 그래서 표시등으로만 사용을 확인할 수 있는 오토 홀드·LFA·i-PEDAL을 뺐다.
- 처음 결정(기록): `getTelltalesInfo`(`CAR_SAFETY`)를 주행 중 3초마다 부른다.
- Rationale: 구독 `registerTelltales`는 `PRIVATE_CAR_SAFETY`를 요구한다(SDK 어노테이션 확인). 이름대로 내부 전용으로 보고 쓰지 않는다. Pleos 정책은 "승인되지 않은 퍼미션을 선언하면 심사 불가"다.
- Alternatives: 구독(권한 위험), 운전 끝에 한 번만 읽기(운전 중 잠깐 켠 오토 홀드를 놓침).

## R3. 와이퍼 스위치도 주기적으로 읽는다

- Decision: 같은 3초 주기에서 `getCurrentWiperSwitch(앞유리)`를 읽는다. 값이 바뀌면 수동 변경 또는 AUTO 사용으로 센다. 비 구간은 "와이퍼 동작 신호 ON 또는 스위치가 AUTO·OFF가 아닌 단계"로 본다.
- Rationale: 스위치 구독 함수가 없다. 콘솔은 스위치만 바꿀 수 있어 동작 신호만으로는 시연이 안 된다.

## R4. 차속 대신 주행거리로 고속 구간을 잡는다

- Decision: 주행거리 표본(시각, km)을 모은다. 새 표본마다, 1분 이상 앞선 가장 가까운 표본과의 평균 시속이 70km 이상이면 직전 표본부터 지금까지를 고속 구간으로 더한다.
- Rationale: SDK에 차속이 없다. 주행거리는 일반 권한이고 콘솔로 시연할 수 있다. 1분 창은 1km 단위로 갱신하는 차에서도 계산이 되게 한다(시속 70km면 1km에 51초).
- Alternatives: 연속 주행 시간만 보기(1차 방식, 시내 정체와 고속도로를 구분 못 함).

## R5. 시연용 시간 압축

- Decision: 앱의 시계 = `SystemClock.elapsedRealtime() × k`. k는 앱이 디버그 빌드(`ApplicationInfo.FLAG_DEBUGGABLE`)면 10, 아니면 1.
- Rationale: 판단 로직은 시계만 받으므로 한 곳에서 바꾸면 1분·5분·10분 조건이 모두 같이 줄어든다. BuildConfig 생성을 켜지 않아도 된다.
- 영향: 주행거리 기반 시속도 압축된 시간으로 계산하므로, 시연 스크립트는 실제 3초마다 1km(압축 시간 30초 = 시속 120km)를 올린다.

## R6. 글꼴

- Decision: 앱에 글꼴을 넣지 않고 시스템 `sans-serif`를 쓴다. 굵기는 `textFontWeight`(API 28+)로 400/600/700을 고른다.
- Rationale: 에뮬레이터 `/product/etc/fonts_customization.xml`이 sans-serif를 AstaSansOTF(Light~ExtraBold)로 지정한다. 600 → Bold, 700 → ExtraBold 파일.

## R7. 크기 단위

- Decision: dp = px. Pleos 글자 단계를 그대로 sp로 쓴다(제목 48, 소제목 32, 본문 26, 보조 22, 최소 18).
- Rationale: 에뮬레이터 밀도 160dpi. 앱 창은 1682×1210dp(가로 분할 창).

## R8. 색

- Decision: `values/colors.xml`과 `values-night/colors.xml`에 Pleos 토큰 값을 둔다. 라이트: 배경 `#F7F8FA`, 카드 `#FFFFFF`, 선 `#E3E5EA`, 글자 `#131417`/`#6B6E76`/`#8B8F98`. 다크: 배경 `#131417`, 카드 `#1D1E21`, 선 `#313236`, 글자 `#F2F3F5`/`#9A9DA5`/`#6B6E76`. 파랑 `#0064FF`/`#0082FF`, 초록 `#02C265`/`#00E074`, 빨강 `#FE3D16`/`#FF4C28`.
- Rationale: `informative_*` 값은 가이드 표에 hex로 있고, 배경·표면 값은 가이드 색 표 이미지를 측정했다.

## R9. 화면 기술

- Decision: 기존 Activity + View 유지. 상황 화면은 코드로 조립하고, Canvas로 그리는 View 3개(`CarMapView`, `TimelineView`, `RingView`)를 둔다. 차 그림은 벡터 드로어블 하나(색은 color 리소스 참조라 다크에서 자동으로 바뀜).
- Alternatives: Jetpack Compose(코드는 짧아지지만 Compose BOM·컴파일러 플러그인이라는 새 의존성, 헌법 III 위반 소지). WebView에 시안 HTML을 그대로 띄우기(차 신호 연결과 심사에서 불리).

## R10. 앱 아이콘

- Decision: 적응형 아이콘(배경 = 보라→파랑 그라데이션, 전경 = 흰 반짝임 별). 알림 아이콘도 같은 별로 바꾼다.
- Rationale: Pleos 가이드는 적응형 아이콘에 브랜드별 마스크(현대 둥근 사각, 기아 모서리 자름)를 씌운다. 전경을 안전 영역(가운데 원) 안에 둔다.

## R11. 세로 화면 확인 방법

- Decision: `adb shell wm size 1440x2560`으로 화면을 세로로 바꿔 확인하고 `wm size reset`으로 되돌린다.
- Rationale: 에뮬레이터에 회전 버튼이 없다. 앱 창 크기만 바뀌면 `layout-port`가 선택된다.
