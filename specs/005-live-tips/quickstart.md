# Quickstart: 안 써본 기능 5차 확인

작업 위치는 `/Users/jiwon/orca/workspaces/unused-feature-app/live-tips`(브랜치 `live-tips`)다. 에뮬레이터 준비와 콘솔 연결은 README "시연"과 같다.

## 1. 단위 테스트와 빌드

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
./gradlew -q :app:testDebugUnitTest :app:assembleDebug
```

- 기대: 1~4차 48개와 새 `LiveCoachTest`가 모두 통과한다(SC-005).

## 2. 주행 최적화 시험(research R1·R2, SC-007)

1. 디버그 APK를 설치하고 앱을 연다. 그다음 내비를 열어 앱을 작은 칸에 둔다.
2. 콘솔로 기어를 D로 바꾼다.
3. 확인
   - `adb logcat -d | grep HOME_SCREEN | grep -E "unusedfeature.*distractionOptimized|hideTask.*unusedfeature"`
   - 화면 캡처에 앱 카드가 보이는가
4. 결과를 research R1·R2와 verification에 적는다. 받아들이지 않으면 FR-014(음성만)로 간다.

## 3. 시연 장면 `live`(SC-001~SC-004, SC-006)

```sh
scripts/demo.sh reset
scripts/demo.sh live
```

| 때 | 기대 |
|---|---|
| 와이퍼를 세 번째로 바꾼 뒤 10초 안 | 카드 "오토 와이퍼"와 문장. 로그 `doSpeak(kr.skku.unusedfeature) text=와이퍼를 자주 바꾸고 계세요…` 한 번 |
| 레버를 Auto로 바꾼 뒤 5초 안 | 카드에 "써 봤어요"와 "오토 와이퍼" 두 줄. 그 사이 말한 로그 없음 |
| 방향지시등 Left, 난방 26°, 앱 시계 5분 | 권함 없음(말한 로그 없음) |
| 방향지시등 Off 뒤 5초 안 | 카드 "시트 열선"과 문장 한 번. 권함은 이 운전에서 2번째이고 더 없다 |
| 주행 중 1초 간격 캡처 두 장 | 앱 창 영역(uiautomator 앱 루트 bounds)이 같다 |
| P | 시트 열선 안내 화면(근거, 누를 곳), P에서 말한 로그 없음, 위에 "오토 와이퍼, 써 보셨네요" 초록 띠 |

## 4. 1~4차 장면

- `scripts/demo.sh`의 기존 장면(`climate`, `rain` 등)을 돌린다. P 안내로 뽑히는 기능은 1~4차와 대체로 같다(FR-017).
- 기대 결과는 spec.md "1~4차와의 관계" 표다. 주행 중 권함이 생기고, 그 기능이 P에서 뽑히면 말하지 않는다(FR-012).
