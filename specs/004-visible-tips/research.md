# Research: 안 써본 기능 4차

## R1. 자막과 소리 막대(위임 결정 V3)

- 결정
  - `WatchService.speak(text)`가 `Boolean`을 돌려준다. 음성 엔진이 준비됐고 발화 호출에 예외가 없으면 true다.
  - `onDriveEnd`는 `lastSpoken = Spoken(pick, SystemClock.uptimeMillis(), said = speak(pick.speech))`를 companion에 둔다. `at`은 말하기 시작한 시각이다.
  - 안내 화면은 막대가 움직일 끝 시각을 정한다: `until = if (s != null && s.feature == pick && s.said) s.at + 6_000 else 0`.
  - 문장은 늘 `pick.speech`를 보인다. 말하지 못했어도 무엇을 말하려 했는지 보인다.
  - 다시 보기는 `record.pick.speech`를 "그때 들은 음성"으로 보이고 `until = 0`(정지)이다.
  - `SoundBars.set(until)`. `onDraw`는 `SystemClock.uptimeMillis() < until && ValueAnimator.areAnimatorsEnabled()`일 때만 사인 높이로 그리고 `postInvalidateOnAnimation()`을 부른다. 아니면 정지 높이로 한 번 그린다.
- 근거
  - 앱이 말하는 문장은 `Feature.speech` 하나뿐이다(`WatchService.onDriveEnd`). Pleos TTS 로그 줄 `doSpeak(kr.skku.unusedfeature) text=…`의 글자가 `speech`와 같다.
  - 화면(`render`)은 신호가 바뀔 때만 다시 그려진다(`WatchService`의 `if (changed) onUpdate?.invoke()`). 그래서 6초 뒤 멈춤은 View가 스스로 정한다.
  - 시계는 둘 다 `uptimeMillis`다(막대 위상과 같은 시계).
- 그리기 값은 contracts/ui.md 표다.

## R2. 기록 카드 미니 지도(위임 결정 V4)

- 결정: `ZoneThumb(feature, state: RowState)`. 상태는 구역 카드의 `RowState`를 다시 쓴다(LEARNED, NOT_YET, MISSING).
  - 우선순위: 익힘 > 지원 안 함 > 아직. 카드의 타일·"써 봄 ✓"이 익힘만 보는 것과 같게 한다.
  - `car_top` 그림을 카드 안 72×128dp에 맞춰 그린다(`CarMapView`와 같은 300×560 비율).
  - `Zone.x·y`에 점(지름 14dp, 채움)과 링(지름 26dp, 선 2dp)을 그린다.
  - 색은 익힘 `green`, 아직 `blue`, 지원 안 함 `dot_line`이다. `dot_idle`은 밝은 테마에서 흰색이라 쓰지 않는다.
  - 이름표, 진행 호, 효과는 없다.
- `Ui.history(records, learned, unsupported: Set<Feature>, onOpen, onClose)`. `MainActivity`가 `WatchService.unsupported`를 넘긴다.
- 근거: `CarMapView`의 좌표계와 그림을 그대로 쓴다. 작은 크기에서는 이름표가 겹쳐 쓸 수 없다.

## R3. 추천 구역 10초 강조(위임 결정 V1·V5)

- 결정
  - 새 안내: 화면 상태가 TIP이고 `WatchService.lastSpoken`이 그 기능이다. `spotlightUntil = lastSpoken.at + 10_000`(uptime)을 render마다 계산해 `CarMapView.set(..., spotlightUntil)`로 넘긴다. Activity에 따로 들고 있지 않는다(구현 중 바뀜: 처음에는 기록 시각(벽시계)이었다).
    - 자막 막대와 같은 사건·같은 시계다. 헤드유닛 시계가 부팅 뒤 바로잡혀도 10초다.
    - 앱이 새로 떠 `lastSpoken`이 없으면(`seed` 직후, 프로세스가 다시 뜬 뒤) 강조가 없다. 같은 실행 안에서 10초 안에 다시 열면 남은 시간만 강조한다.
    - `drivesSeen`(첫 render에 -1)과 묶지 않는다.
  - 강조 조건: `pulse != null && !dim && uptime < spotlightUntil`.
  - 움직임이 켜져 있으면 추천 구역에 지금 원 위로 아래를 더 그린다.
    - 테두리 링: 반지름 `r × (1 + 2.4t)`(r에서 3.4r), 두께 3dp, 색 `blue`, 알파 `(1 − t) × 200`. `t`는 `uptimeMillis % 1200 / 1200f`다.
    - 지금 1.8초 애니메이터가 계속 다시 그리므로 링은 10초가 지나면 저절로 사라진다.
  - 움직임 줄이기(`ValueAnimator.areAnimatorsEnabled() == false`)면 링 대신 반지름 `r + 6dp`, 두께 4dp, `blue` 원을 고정해서 그린다.
  - 이름표: 강조 동안 굵기 800(지금 600)이다. 움직임 줄이기에서도 같다.
  - 끝내기: 애니메이터가 꺼져 있고 강조 중이면 `set()`에서 `postInvalidateDelayed(spotlightUntil − now)`를 건다. 켜져 있으면 맥박 애니메이터가 매 프레임 그리므로 걸지 않는다.
- 근거: 기록 시각은 이미 저장돼 있다(`Record.at`). 새 상태를 두지 않고 "언제 생긴 안내인가"로 정한다.

## R4. 샘플 상태(위임 결정 V2·V6)

- 결정
  - 순수 `Seed.records(now, zone)`가 기록 6개를 돌려준다(오래된 것이 앞, `History` 저장 순서). 값은 data-model 표다. `Seed.kt`는 `app/src/debug`, `SeedTest`는 `app/src/testDebug`에 두어 출시 빌드에 들어가지 않는다(구현 중 바뀜).
    - 시각: `zone` 기준 오늘 0시에서 사흘 전·그저께·어제의 09:00과 18:00. 날짜 묶음이 늘 셋이다.
    - 이유 문장: 기능마다 `DriveStats().apply { … }`를 만들어 `f.reason(stats)`를 부른다. 앱이 실제로 쓰는 문장 함수 그대로다. `SeedTest`가 `f.opportunity(stats)`도 참인지 본다.
    - 근거 표시(`Mark`)는 기능마다 하나 이상이고, 종류는 그 기능의 근거 종류(`Feature.evidence`) 안에 있다. 글은 앱 기록 형식("풍량 2→4", "와이퍼 OFF→LO", "시속 70km 이상")을 따른다.
  - 익힘은 `Seed.learned`(공조 AUTO, 시트 열선. 둘 다 기록에 있음)다. 익힌 시각·메모도 data-model 표다.
  - 디버그 `SeedReceiver`는 `progress` 저장소를 비우고 data-model 키 표대로 쓴다(`commit()`).
  - 보호(위임 결정 V6): `android:exported="true"`, `android:permission="android.permission.DUMP"`. adb shell은 DUMP를 가지고 일반 앱은 받을 수 없어, shell만 부를 수 있다.
- 근거
  - 기록 형식에 제어 문자(`\u001E`, `\u001F`)가 있어 XML 파일을 손으로 만들면 틀리기 쉽다. 앱의 `History.encode`를 쓰면 형식이 늘 맞다.
  - 같은편 앱도 같은 디버그 수신기 방식(`DemoReceiver`)이다.
- 시연: `demo.sh seed` = 차 상태 되돌리기(`reset`과 같은 신호) → `am force-stop` → `am broadcast --user 10 -n kr.skku.unusedfeature/.SeedReceiver` → `am start`
  - 차 상태를 먼저 되돌리는 까닭: 켜진 기능이 남아 있으면 앱이 열리자마자 "익힘"으로 기록해 샘플 상태가 바뀐다.
- 출시 빌드 확인: `./gradlew :app:processReleaseMainManifest` 뒤 병합 매니페스트에 `SeedReceiver`가 없고, 디버그 병합 매니페스트에는 있다.
