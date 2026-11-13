# Research: 안 써본 기능 6차 — 차 화면 개선

## R1. 차 기준 값

- **Decision**: 누르는 칸 76×76dp 이상, 이웃한 누르는 칸 사이 24dp 이상, 화면 글자 24sp 이상, 카드 제목 28sp 이상, 그림 안 글자 22sp(이 앱), 글자 대비 4.5:1 이상.
- **Rationale**: Android for Cars 디자인 가이드가 누르는 칸 76×76dp·간격 23dp, 주 글자 32dp·보조 24dp, 대비 4.5:1을 권한다(https://developers.google.com/cars/design/design-foundations/visual-principles, https://developers.google.com/cars/design/automotive-os/design-system/sizing). 이 앱의 본문 26sp와 카드 제목 28sp는 주 글자 32dp보다 작지만, 정차 중 읽는 긴 글이라 줄 수를 늘리지 않는 쪽을 골랐다. 주행 중 카드는 이미 60/30sp다. 간격은 4dp 격자에 맞춰 24dp로 올렸다.
- **Alternatives considered**: 본문까지 32sp로(정차 화면 길이가 1.5배가 되어 스크롤이 늘어남), Pleos 기존 규칙 유지(보조 22·최소 18, 점검에서 P1).

## R2. 설정 변화 때 열린 칸 보존

- **Decision**: `onSaveInstanceState`에 열린 구역 이름, 기록 번호(목록 −1), 기록 목록 스크롤(`listY`), 지금 스크롤, 본 운전 수(`drivesSeen`)를 넣고 `onCreate`에서 되살린다. `onPause`의 "칸 닫기"는 `isChangingConfigurations`가 거짓일 때만 한다.
- **Rationale**: 낮·밤 전환은 매니페스트에 `configChanges`가 없어 액티비티를 새로 만든다. 지금은 `onPause`가 칸을 닫고, 새 인스턴스의 `drivesSeen = -1`이 `render`의 "새 운전 결과면 닫기"에 걸려 이중으로 닫힌다. 설정 변화일 때 `onPause` → `onStop` → `onSaveInstanceState` 차례로 불리고 `isChangingConfigurations`가 참이므로, 닫기를 건너뛰고 저장하면 된다. 다른 앱으로 가는 `onPause`는 거짓이라 지금처럼 닫힌다(spec US3-5).
- **Alternatives considered**: `android:configChanges="uiMode"`로 다시 만들기를 막기(레이아웃 XML 뷰와 지도 색을 손으로 다시 칠해야 해 코드가 더 늚), 상태를 SharedPreferences에 저장(설정 변화 밖까지 남아 US3-5와 어긋남).

## R3. 근거 카드 숫자 규칙

- **Decision**: 새 순수 함수 `evidenceCount(marks: List<Mark>): String`. 구간(`end >= 0`)과 순간(`end < 0`)이 함께 있으면 빈 문자열, 순간만 있으면 "n번", 구간만 있으면 합친 분 "n분". 지금 `tip()`의 식을 옮기고 섞인 경우만 더한다.
- **Rationale**: 18개 기능 중 근거가 구간(비·밤·고속·운전)과 순간(조작)으로 섞이는 기능은 앞유리 김서림 제거({RAIN, RECIRC})뿐이다(Feature.kt 근거 집합). 이 경우 지금 식은 순간 수("1번")를 보여 이유("8분")와 어긋난다. 겹친 분은 기록에 없어(구간 `rainRecirc`는 표시를 남기지 않음) 숫자를 바로잡을 수 없으므로 뺀다.
- **Alternatives considered**: 겹친 구간을 새 표시 종류로 기록해 막대로 그리기(저장 형식과 판단 코드가 바뀜, FR-012 위반), 기능마다 머리 숫자를 따로 정하기(18개 모두 손댐).

## R4. 세로 1:1 칸 폭

- **Decision**: 세로 칸(약 850dp, 화면 여백 44dp씩)에 커진 누를 곳 버튼 그림이 한 줄로 들어간다. 줄바꿈 처리를 더하지 않는다. 칩 격자(권할 것 없음, 써 보셨네요, 도감 완성)는 세로 화면에서 2열로 한다. 차 지도는 세로 화면에서 그림으로만 둔다(이름표 없음, 점 누르기 없음, 사용자 결정 2026-11-13). 가로 화면의 이름표는 양쪽으로 뷰 안에 민다.
- **Rationale**: 버튼 글자가 가장 긴 두 기능을 22/24sp로 어림하면 하이빔 보조("AUTO 위치", "앞으로 밀기", "표시등 확인") 약 510dp, 앞유리 김서림("앞유리 김서림", "뒷유리 열선", "A/C") 약 470dp로, 남는 폭 약 760dp보다 작다. 구현 뒤 에뮬레이터 캡처로 확인한다(quickstart §3).
  칩 격자는 3열이면 세 칩("동승석 개별 온도", "동승석 시트 열선·통풍", "크루즈 컨트롤")의 폭 합이 약 830dp로, 세로 카드 안쪽 폭(약 695dp)을 넘는다. 차 지도는 세로 화면에서 폭이 약 385dp라, 가운데 구역(앞유리·와이퍼, 오른쪽 이름표)의 22sp 이름표 상자(약 197dp)가 오른쪽으로 약 25dp 넘친다. 왼쪽으로 밀면 구역 점을 덮고, 위로 옮기면 다른 구역 점·이름표와 겹친다(goldfish 재리뷰). 세로 지도는 축척이 약 0.5라 구역 점 누름 지름도 약 46dp다. 바로 옆 구역 목록이 같은 정보(이름·익힌 수)와 같은 동작(구역 카드 열기)을 76dp 줄로 주므로 세로 지도는 그림으로만 둔다.
- **Alternatives considered**: 버튼 그림을 여러 줄로 감싸기(실제 버튼 줄 모양과 달라짐), 세로 화면에서만 이름표 글자를 줄이기(차 기준에서 더 멀어짐).

## R5. 색 값

- **Decision**: 낮 `text3` #62656D, `green_text` 낮 #017A40·밤 #00E074(밤은 그대로), 새 `blue_text` 낮 #0050D8·밤 #6CB4FF.
- **Rationale**: WCAG 상대 휘도로 계산했다.

| 글자 / 바탕 | 낮 | 밤 |
|---|---|---|
| text3 / 카드 · 바탕 · 회색 칩 · 지도 아래 | 5.83 · 5.49 · 5.03 · 4.92 | 6.14 · 6.79 · 5.15 · 6 이상 |
| green_text / green_soft · 카드 | 4.97 · 5.44 | 7.84 · 9.46 |
| blue_text / blue_soft · 카드 | 5.90 · 6.69 | 6.72 · 7.63 |
| on_accent / blue(누를 버튼·1·2단계 번호, 24sp 굵게 = spec 용어의 큰 글자 예외) | 4.92 | 3.73(3:1 통과) |
| panel_text / panel(누를 곳 그림) | 5.47 | 6.04 |

  지금 값: 낮 text3 / 회색 칩 4.40, green_text / green_soft 3.34, blue / blue_soft 4.34, 밤 blue / 카드 4.47, text4 각주 3.2(밤 3.3).
- **Alternatives considered**: `text4`를 진하게(그러면 text3과 같아져 토큰이 의미를 잃음 → 글자에서는 text4를 안 쓰기로).
