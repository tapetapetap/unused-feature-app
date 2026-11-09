# Quickstart: 추천 기록 페이지 검증

## 준비

002 quickstart(`specs/002-redesign-21-features/quickstart.md`)의 "준비"와 같다. 빌드, 설치, `adb forward`를 하고, 브라우저의 Virtual Console 페이지는 닫는다.

- **단위 테스트**: `./gradlew :app:testDebugUnitTest`. `CoachTest`에 History 테스트가 들어 있다.
- **화면 글자 읽기**(Pleos가 앱을 가릴 때): `adb shell uiautomator dump /sdcard/ui.xml && adb shell cat /sdcard/ui.xml | grep -o 'text="[^"]*"'`
- **"추천 기록" 버튼 누르기**: 화면을 보고 직접 누르거나, `uiautomator dump`에서 버튼의 `bounds`를 읽어 `adb shell input tap <x> <y>`로 누른다.

## 시나리오

| # | 조작 | 기대 결과 |
|---|---|---|
| 1 | `scripts/demo.sh reset` → "추천 기록" 누름 | 버튼 글자가 "추천 기록  ›"(숫자 없음)이다. 빈 상태 문구가 보인다. "✕ 닫기"를 누르면 첫 실행 화면으로 돌아간다 |
| 2 | `climate` → `wiper` → "추천 기록" 누름 | 버튼 글자가 "추천 기록 2  ›"다. 오늘 날짜 머리 아래 오토 와이퍼(위)와 공조 AUTO(아래)가 각자 시각·이유 한 줄과 함께 보인다 |
| 3 | 2에서 공조 AUTO 항목 누름 | 작은 제목이 "{오늘} {시각} 운전 · {m}분"이다. 근거 3개(10초·30초·50초)와 AUTO 강조가 보이고, 지도의 공조 패널에 선택 링이 있다. "← 추천 기록"을 누르면 목록으로 돌아간다 |
| 4 | `highbeam` → "추천 기록" | 기록 수가 그대로 2다(권할 것 없던 운전은 남기지 않음) |
| 5 | `learn` → "추천 기록" | 공조 AUTO 항목에 "써 봄 ✓"가 붙는다. 누르면 위쪽 줄에 "써 봄 ✓ {오늘}"이 보인다 |
| 6 | 기록 페이지를 연 채 기어 N(콘솔) → 덤프 → P | 덤프에 "운전 중이에요"가 보이고, "추천 기록" 버튼이 `enabled="false"`다. P로 돌리면 기록 페이지가 아니라 상황 화면이 나온다 |
| 7 | `adb shell am force-stop --user 10 kr.skku.unusedfeature` → `am start` → "추천 기록" | 기록 수와 내용이 그대로다 |
| 8 | 목록과 다시 보기를 다크(`cmd uimode night yes`)와 세로(`am task resize`)로 보기 | 잘림·겹침이 없다. 세로에서 "추천 기록" 버튼 글자가 잘리지 않는다 |
| 9 | 공조 패널 점을 눌러 구역 카드 열기 → "추천 기록" → 목록에서 구역 점 누르기 → 다시 보기에서 구역 점 누르기 | "추천 기록"을 누르면 목록이 뜨고 구역 카드(선택 링)는 닫힌다. 목록이나 다시 보기에서 구역 점을 누르면 구역 카드가 뜨고 기록 페이지는 닫힌다(FR-008) |
| 10 | 기록 54개 넣기(아래) → "추천 기록" | 버튼 글자가 "추천 기록 54  ›"다. 누름부터 목록이 다 그려질 때까지 1초 안이고, 끝까지 스크롤하면 가장 오래된 기록이 보인다(SC-003) |

구역 점은 에뮬레이터 화면에서 직접 누르거나, 캡처에서 좌표를 읽어 `adb shell input tap <x> <y>`로 누른다.

**시나리오 10: 기록 54개 넣기**
1. 기록 54개(18개 기능 × 3번, 기록마다 근거 몇 개)를 data-model "저장" 형식으로 만든다. XML에서는 `\u001E`·`\u001F`·줄바꿈을 `&#30;`·`&#31;`·`&#10;`로 쓴다.
2. 그 값을 `<string name="history">`에 넣고, `drives`(1 이상)를 함께 넣은 `progress.xml`을 만든다.
3. `adb shell am force-stop --user 10 kr.skku.unusedfeature`
4. `adb shell "run-as kr.skku.unusedfeature --user 10 sh -c 'cat > shared_prefs/progress.xml'" < progress.xml`
5. 앱을 시작하고, `adb shell screenrecord`로 버튼 누름부터 목록이 다 그려질 때까지를 찍는다. 녹화의 프레임 시각으로 1초 안인지 본다.
6. 끝나면 `scripts/demo.sh reset`으로 되돌린다.

이 절차를 못 하면 verification.md의 "확인하지 못한 것"에 적는다.

## 통과 기준

- 단위 테스트 실패 0
- 위 10개 시나리오에서 기대 결과와 다른 화면이 없음
- `logcat -b crash`에 이 앱 0건
