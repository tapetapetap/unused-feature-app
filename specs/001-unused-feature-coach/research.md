# Research: 안 써본 기능

## R1. 차 신호를 어디서 받나

- **Decision**: Pleos Vehicle SDK 2.3.2(`ai.pleos.playground:Vehicle`, Maven `https://nexus-playground.pleos.ai/repository/maven-releases/`). `Vehicle(context).initialize()` 후 `getGear()/getHVAC()/getSeat()/getSteeringWheel()/getLight()/getDrivingMode()`의 `register*` 리스너를 씁니다.
- **Rationale**: SDK AAR을 내려받아 `javap`로 공개 인터페이스를 직접 확인했습니다. 필요한 신호가 모두 있습니다.
  - 기어 `registerGearSelection` → `VehicleGear`(GEAR_PARK 등)
  - 공조 `registerHvacAutoState`, `registerHvacDualState`, `registerHvacAcState`, `registerHvacFanSpeed`, `registerHvacTemperature`(SeatArea별)
  - 시트 `registerHvacSeatTemperature`, `registerHvacSeatVentilation`, `registerSeatOccupancy`(SeatArea별)
  - 운전대 `registerHvacSteeringWheelHeatState`
  - 전조등 `registerLowBeamLightState`(LightState) + `getLowBeamLightSwitch`(LightSwitch.AUTOMATIC)
  - 크루즈 `registerCruiseControlState`(CruiseControl.ACTIVATED만. ENABLED는 대기 상태이고 에뮬레이터 기본값이라 제외)
- **Alternatives considered**: AAOS `CarPropertyManager` 직접 사용 — Pleos 문서가 차량 데이터는 Vehicle SDK를 쓰라고 하고, 심사 "기술 활용성"에도 SDK 사용이 유리해 기각.

## R2. 권한

- **Decision**: `pleos.car.permission.CAR_INFO`, `CAR_DRIVE`, `CAR_CLIMATE`, `CAR_SEATS`, `CAR_LIGHTS`, `TTS_SERVICE`. 제어(CONTROL_*) 권한은 선언하지 않습니다.
- **Rationale**: 권한 문자열은 SDK의 `PleosPermission` 상수에서 확인했습니다. 앱은 읽기만 합니다. 어떤 API가 어떤 권한을 요구하는지는 서비스 쪽에서 판정해 AAR에서는 알 수 없어, 쓰는 영역(정보·주행·공조·시트·조명)을 모두 선언했습니다. `TTS_SERVICE`는 공개 저장소의 실측 메모(2026-10-22, 없으면 SecurityException)를 따랐습니다.

## R3. 음성

- **Decision**: Pleos TextToSpeech 3.3.1, `TextToSpeech(context, Mode.ON_DEVICE)` → `initialize(OnInitializeListener)` → `speak(text, true)`.
- **Rationale**: 네트워크 없이 동작하고(원칙 V), Pleos SDK 활용. 실패 시 알림·카드가 남으므로 다른 대체 엔진을 두지 않습니다.
- **Alternatives considered**: `android.speech.tts.TextToSpeech` — Pleos 이미지에 기본 엔진이 있는지 모르고, 둘 다 두면 코드가 늘어 기각.

## R4. 백그라운드에서 운전 지켜보기

- **Decision**: 포그라운드 서비스, `foregroundServiceType="specialUse"` + `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` 설명. `BOOT_COMPLETED`에서 시작, 앱을 열 때도 시작.
- **Rationale**: targetSdk 34는 FGS 타입이 필수입니다. 차량 신호 감시에 맞는 표준 타입이 없어 specialUse를 씁니다. specialUse는 부팅 시 시작 제한 대상이 아닙니다.
- **Risk**: Pleos 심사에서 상시 포그라운드 서비스를 허용하는지는 문서로 확인하지 못했습니다. 기획서에 적고 에뮬레이터에서 확인합니다.

## R5. 운전 시간

- **Decision**: 기어가 P가 아닌 값으로 바뀐 시각부터 P가 된 시각까지 `SystemClock.elapsedRealtime()` 차이로 잽니다.
- **Rationale**: SDK `Odometer.registerTripCurrentDrivingTime`도 있지만 에뮬레이터에서 값이 움직이는지 모르고, 기어만으로 데모가 가능합니다.

## R6. 빌드 도구

- **Decision**: AGP 8.8.2, Gradle 8.11.1(wrapper), Kotlin 2.0.21, compileSdk 35, targetSdk 34, minSdk 28, `useLibrary("android.car")`는 쓰지 않음(Car API 직접 호출 없음).
- **Rationale**: Pleos 공식 샘플 빌드 환경(JDK 17, AGP 8.8.2, Kotlin 2.0)과 맞춥니다. 에뮬레이터는 API 34, Pleos 요구 minSdk 28.

## R7. 화면

- **Decision**: 일반 Activity + XML View. Automotive 템플릿처럼 `android.hardware.type.automotive` required. DO(주의분산 최적화) 선언 안 함 → 주행 중에는 시스템이 화면을 막음.
- **Rationale**: 정차 중에만 보는 앱이고 화면이 단순해 Compose 의존이 필요 없습니다(원칙 III).
