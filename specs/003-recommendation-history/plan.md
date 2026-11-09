# Implementation Plan: 추천 기록 페이지

**Branch**: `003-recommendation-history` (스펙 폴더 기준. git 브랜치는 `main`) | **Date**: 2026-11-09 | **Spec**: [spec.md](./spec.md)

**Input**: `specs/003-recommendation-history/spec.md`, 설계 `docs/superpowers/specs/2026-11-09-recommendation-history-design.md`

## Summary

안내가 나간 운전마다 기록 1개를 기기 안에 남긴다. 기록에는 시각, 기능, 이유, 운전 길이, 근거가 들어간다. 지도 머리의 "추천 기록" 버튼을 누르면 날짜별 목록이 나오고, 항목을 누르면 그때의 안내 화면을 다시 볼 수 있다.

구현은 이렇게 한다.
- 구역 카드처럼 같은 액티비티의 오른쪽 상황 칸을 바꾸는 화면 상태 두 개(`HISTORY`, `RECORD`)를 더한다.
- 다시 보기는 기존 안내 화면 함수(`Ui.tip`)를 쓰고, 작은 제목과 위쪽 줄만 바꾼다.
- 저장은 기존 진행 기록(SharedPreferences "progress")에 키 하나를 더해서 한다.

## Technical Context

**Language/Version**: Kotlin 2.0.21, JDK 17 (002와 같음)

**Primary Dependencies**: Android View, Pleos Vehicle·TextToSpeech SDK(바뀌지 않음). 새 의존성 없음

**Storage**: SharedPreferences "progress"의 새 키 `history` 하나(research R1)

**Testing**: JUnit 4(`app/src/test/.../CoachTest.kt`), 에뮬레이터 확인(quickstart)

**Target Platform**: Pleos Connect Emulator v2.1.0(API 34), 실차 Pleos Connect

**Project Type**: mobile-app (Android 단일 모듈 `app`)

**Performance Goals**: 기록 최대 54개 목록이 1초 안에 열린다(SC-003). 뷰가 수십 개라 ScrollView + LinearLayout으로 충분하다(research R3).

**Constraints**:
- 주행 중 화면 안내 금지(원칙 I)
- 기기 밖 전송 금지(원칙 V)
- 글자 18sp 이상

**Scale/Scope**: 기록은 최대 54개. 화면 상태 2개를 더하고, 파일 6개를 고친다(새 파일 없음).

## Constitution Check

| 원칙 | 판단 | 근거 |
|---|---|---|
| I. 정차 중에만 말한다 | 통과 | 기록 페이지는 정차 중에만 열린다. 기어가 P가 아니면 `DRIVING`이 우선해서 닫힌다(FR-007). 음성은 없다 |
| II. 차 신호만 믿는다 | 해당 없음 | 새 판단이 없다. "써 봄"은 기존 익힘 기록을 읽기만 한다 |
| III. 최소 구현 | 통과 | 새 의존성과 새 파일이 없다. 안내 화면 함수와 근거 저장 형식(`Marks`)을 다시 쓴다. RecyclerView, Room, JSON 라이브러리는 쓰지 않는다(research R1·R3) |
| IV. 판단 로직은 테스트로 지킨다 | 통과 | 저장 형식(`History.encode/decode/append`)은 순수 Kotlin이고, 왕복·손상·덧붙이기 테스트를 둔다 |
| V. 기록은 차 안에만 | 통과 | 기기 안 SharedPreferences에만 저장한다 |

설계를 마친 뒤 다시 검토했고 판단은 같다.

## Project Structure

### Documentation (this feature)

```text
specs/003-recommendation-history/
├── spec.md
├── plan.md              # 이 파일
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/ui.md
├── checklists/requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code (바꾸는 파일만)

```text
app/src/main/java/kr/skku/unusedfeature/
├── Coach.kt          # Record, History(encode/decode/append) — Marks 옆
├── Progress.kt       # record()에서 기록 덧붙임, history()
├── Screens.kt        # Ui.history() 목록 페이지, Ui.tip()에 title 매개변수
└── MainActivity.kt   # HISTORY·RECORD 상태, 지도 머리 버튼
app/src/main/res/layout/map_head.xml                    # "추천 기록" 버튼
app/src/test/java/kr/skku/unusedfeature/CoachTest.kt    # History 테스트
```

**Structure Decision**: 002와 같은 단일 모듈이다. 새 파일 없이 기존 파일에 더한다.

## 핵심 설계

1. **기록 남기기**: `Progress.record(o)`에서 `o.pick`이 있으면 `History.append(기존 문자열, Record(...))`로 덧붙인다.
   - `Record`에는 지금 시각, 기능, 이유, 운전 길이, 근거가 들어간다.
   - 기존 문자열을 다시 쓰지 않고 뒤에 이어 붙인다. 그래서 읽을 수 없는 옛 기록도 지워지지 않는다(research R9).
2. **읽기**: `Progress.history()`는 `History.decode(...)`의 순서를 뒤집어 최신을 앞에 둔다. 읽을 수 없는 기록은 `decode`가 건너뛴다(FR-009).
3. **화면 상태**: `openHistory: Int?` 하나로 나타낸다(`null` 닫힘, `-1` 목록, `n` n번째 다시 보기).
   - 우선순위는 DRIVING > RECORD > HISTORY > ZONE > FIRST > COMPLETE > TIP > CONFIRM > NONE이다.
   - `render()` 머리에서 운전 중이거나 운전 횟수가 바뀌면 `openZone`과 함께 닫는다.
   - 버튼과 구역 누르기는 서로를 닫는다(FR-008).
4. **목록**: `Ui.history(records, learned, onOpen, onClose)`로 만든다.
   - 기록을 날짜(`day(at)`)로 묶는다.
   - 항목 카드는 아이콘 타일, 이름과 시각, 이유, "써 봄 ✓" 칩으로 이루어진다. 타일은 익힘이면 초록, 아니면 파랑이다.
   - 기록이 없으면 빈 상태 문구를 보인다.
5. **다시 보기**: `Ui.tip(f, reason, marks, driveMs, banner, title)`로 만든다.
   - `banner` 자리에는 "← 추천 기록" 칩을 두고, 익힘이면 "써 봄 ✓ {날짜}" 칩을 함께 둔다.
   - `title`은 그때 시각과 운전 길이다. 기본값은 지금의 "방금 끝난 운전 · {m}분"이라, 기존 호출은 그대로 둔다.
   - 지도는 `selected = pick.zone`이고 깜빡임은 없다.
6. **지도 머리 버튼**: `map_head.xml`의 글자 묶음 셋째 줄("내 차 기능"·"n개 익힘" 아래)에 칩 모양 TextView `historyButton`을 둔다. 가로·세로가 같다. `render()`에서 글자를 정하고, 주행 중에는 흐리게 하고 비활성(`isEnabled = false`)으로 둔다. 목록이나 다시 보기가 열려 있어도 누르면 늘 목록으로 간다.
7. **테스트**: `CoachTest`에 다음을 넣는다.
   - History 왕복: 근거 여러 개, 근거 없음, `|`·`→`·`°`가 든 이유
   - 손상 건너뛰기: 필드 수가 다름, 모르는 기능 이름, 숫자가 아님
   - 덧붙이기: `null`, 빈 문자열, 기존 기록 뒤

## Complexity Tracking

없음.
