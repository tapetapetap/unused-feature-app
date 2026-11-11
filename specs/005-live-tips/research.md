# Research: 안 써본 기능 5차

## R1. Pleos 에뮬레이터가 adb로 설치한 앱의 주행 최적화 선언을 받아들이는가

- **확인 방법**: 매니페스트 선언만 넣은 디버그 빌드를 설치한다. 앱을 띄운 채 기어를 D로 바꾼 뒤 아래를 본다.
  - 홈 화면 로그 `TaskViewManager::onTaskCreated … distractionOptimized:` 값
  - `TaskSlotLayout::hideTask` 대상에 이 앱이 있는가
  - 화면 캡처
- **이미 아는 것**(같은편 4차 녹화 로그, 2026-11-12)
  - 홈 화면은 앱 창을 띄울 때마다 앱의 `distractionOptimized` 값을 로그에 쓴다(같은편: false).
  - D가 되면 주행 제한이 켜지고(`isRequiresDistractionOptimization=true`, 영상 금지·설정 금지), 같은편 창을 `hideTask`한다.
  - 홈 화면에는 따로 가리지 않는 꾸러미 목록(`preventHidePackage`: Playground 앱 등)이 있다.
  - Pleos 문서(주행 규제 개발 가이드, 주행 안전 설계)에는 선언 방법·심사 조건·에뮬레이터 동작이 없다.
- **결정 규칙**: 받아들이면 FR-001~FR-003·FR-011 화면을 만든다. 아니면 FR-014(음성만)다.
- 위 관찰은 모두 같은편 앱(`kr.skku.sameside`, 같은 팀의 픽업 앱)의 로그다.
- **판정 기준**: 기어 D에서 이 앱이 홈 화면의 `hideTask` 대상이 아니고, 캡처에 앱 창이 보이면 받아들임이다.
- **결과(2026-11-12 2시 27분, T001)**: 받아들임.
  - 매니페스트 선언만 넣은 빌드를 설치했다. 기어 D에서 앱을 열자 홈 화면 로그에 `showTaskView … kr.skku.unusedfeature` 줄이 있었고, `hideTask`는 내비 쪽에만 나왔다.
  - 캡처에 앱의 지금 주행 화면("운전 중이에요")이 보였다.
  - 주행 제한은 그대로 켜져 있었다(`isRequiresDistractionOptimization=true`, NO_VIDEO·NO_SETUP).
  - 캡처: scratchpad `do-test/3-drive-app.png`
  - 그래서 FR-014(음성만)는 쓰지 않는다.

## R2. 앱을 내비 옆 칸에 두는 방법

- Pleos 창 정책(Window Policy)
  - 앱 두 개를 2:1 또는 1:2로 띄운다(기본 2:1).
  - 새 앱은 큰 칸(Primary)에 뜨고, 원래 큰 칸 앱은 작은 칸(Secondary)으로 간다.
- 그래서 시연은 앱을 띄운 뒤 내비를 띄운다. 그러면 내비가 큰 칸, 앱이 작은 칸이다.
- 작은 칸으로 가지 않으면 내비를 띄우지 않고 전체 앱 칸에서 시연한다. FR-001은 두 자리를 다 허용한다.
- **결과(T001)**
  - 앱을 띄운 뒤 내비를 띄우면 나뉘지 않고, 내비가 앱 칸 전체를 차지했다.
  - 화면 오른쪽 끝의 창 손잡이(⋮, 약 x 2547, y 720)를 가운데(x 1500)로 끌면 1:1 분할이 됐다. 홈 화면 로그 `updateSlotWeight driver: 1, passenger: 1`이다. 왼쪽이 앱(폭 약 697px, 세로 배치), 오른쪽이 내비다.
  - 손잡이를 더 왼쪽으로 끌자 분할이 풀렸다. 이 에뮬레이터에서 두 앱은 1:1만 됐고, 2:1은 보지 못했다.
  - 그래서 시연(T018)은 `adb shell input swipe 2547 720 1500 720 800`으로 1:1 분할을 만든다. "내비 옆 칸"은 이 1:1의 앱 칸이다.

## R3. 방향지시등 신호

- **결정**: DRIVE DNA `CarWatcher.watchTurnSignal`(같은 팀 저장소 `drive-dna/app/src/main/java/kr/skku/drivedna/CarWatcher.kt`)과 같다. Vehicle-2.3.2.aar를 javap로 연 결과는 아래와 같다.
  - 받기: `vehicle.getTurnSignal().registerTurnSignalState(l)`, 끊기: `unregisterTurnSignalState(l)`
  - 리스너: `TurnSignalStateListener.onTurnSignalStateUpdated(v: TurnSignalValue?)`, 실패는 `onFailed(e: Exception)`(다른 리스너와 같은 `VehicleListener`)
  - 값: `OFF`, `LEFT`, `RIGHT` 셋이다. LEFT·RIGHT면 켜짐, OFF면 꺼짐이다. null이 오면 무시한다(마지막 값 유지). 운전 끝에 꺼짐으로 돌린다.
  - 콘솔 값은 `turn-signal?mode=BlinkLeft|BlinkRight|Off`다(`Left`는 400 `invalid_turn_signal_mode`, T019에서 확인).
  - 구독이 예외를 던지면 다른 구독처럼 로그만 남기고 방향지시등 조건 없이 판단한다.
- **근거**: DRIVE DNA 2·3차에서 에뮬레이터로 확인한 API다. 콘솔 값은 깜빡임 없이 LEFT/RIGHT로 온다.
- **권한**: 새 권한은 없다. 이 앱이 이미 선언한 조회 권한 안에서 DRIVE DNA와 같은 호출이다. 구현 때 첫 값이 오는지 로그로 확인한다(tasks T008).
- **콘솔 명령**: `PUT /vehicle/turn-signal?mode=BlinkLeft|BlinkRight|Off`(DRIVE DNA `scripts/demo.py` `SIGNAL`과 같다).
- **버린 안**: 조향각으로 "바쁜 순간"을 본다. 기준값이 더 필요하고, 방향지시등 하나로 차로 변경·회전 대부분을 덮는다.

## R4. 운전 중 누적값

- **결정**: `Span`에 `totalAt(t) = total + (켜져 있으면 t − since)`를 두고, `Tracker.tick()`이 `stats.rainMillis`, `rainRecircMillis`, `driveMillis`를 지금까지 값으로 쓴다.
- **근거**
  - 운전 끝(`endDrive`)은 같은 칸을 최종값으로 다시 쓰므로 P 계산은 그대로다.
  - 고속 시간(`highwayMillis`)은 이미 거리 신호마다 더해진다.
  - 운전 시간은 주행거리 신호가 없는 차의 크루즈 기회(운전 20분)에 필요하다.
  - 밤 시간(`nightMillis`)은 주행 중 후보가 아닌 앰비언트 라이트만 쓰므로 채우지 않는다(data-model §1 표).
- **버린 안**: stats 복사본을 만든다. `DriveStats`는 칸이 많고, 복사 함수를 따로 지켜야 한다.

## R5. 판단 주기

- **결정**: `CarWatcher.poll`(운전 중 3초마다 스위치를 읽음)이 `tracker.tick()` 뒤 `onChange()`를 부른다. 신호가 바뀔 때도 `onChange()`가 불린다.
- **근거**: 새 타이머를 두지 않는다(원칙 III). 3초는 SC-001(10초 안 권함)을 넉넉히 지킨다.
- **영향**: 와이퍼·전조등·하이빔 스위치는 이 3초 읽기로만 들어온다. 그래서 스위치 조작에서 "써 봤어요"까지는 최대 3초 + 화면 갱신이다. SC-002와 시연은 조작 뒤 5초를 기준으로 한다.

## R6. 주행 중 화면과 제한 플래그

- **결정**: 제한 플래그를 구독하지 않는다. 기어가 P가 아니면(`WatchService.driving`) 카드를 보인다.
- **근거**
  - 카드는 움직임, 영상, 입력, 설정이 없다. 그래서 에뮬레이터에서 본 두 제한과 문서의 제한 예(키보드 금지, 긴 글 제한)를 모두 지킨다.
  - 1~4차도 기어로 주행 중을 판단했다.
- **버린 안**: `CarUxRestrictionsManager` 구독. `android.car` 라이브러리 연결이 새로 필요하고, 화면이 달라지지 않는다. App Market 심사 전에는 다시 본다.

## R7. 주행 중 문장

- **결정**: 기능마다 40자 안쪽 현재형 한 문장이다. 무엇을 봤는지와 무엇을 누르면 되는지를 담는다. 문구는 contracts/ui.md §3이다.
- **근거**: Pleos 주행 안전 설계는 운전자가 1~2초 안에 파악할 핵심만 보이라고 한다. 음성은 한 번이라 다시 들을 수 없으므로 카드에도 같은 문장을 둔다.

## R8. 내비 옆 작은 칸의 배치

- **사실**: 작은 칸(폭 약 560dp)은 세로로 길어 `layout-port` 배치가 쓰인다. 위쪽 상황 화면(비중 1.2)과 아래쪽 지도 칸(비중 1)으로 나뉘어, 카드가 쓸 수 있는 높이는 창의 절반 남짓이다(`app/src/main/res/layout-port/activity_main.xml`).
- **결정**(goldfish 리뷰 뒤, 사용자 위임 추천안): 주행 중 세로 배치에서는 지도 칸을 숨긴다(GONE). 위 여백 40dp, 타일 120으로 줄인다. 그러면 카드 전체(약 600dp)가 스크롤 없이 들어간다.
- **버린 안**
  - 스크롤 허용: 주행 중 손 조작이 된다.
  - 지도를 남기고 카드를 더 줄임: 기능 이름과 문장 글자가 24sp 아래로 내려간다.
