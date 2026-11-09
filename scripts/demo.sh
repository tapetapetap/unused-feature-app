#!/bin/sh
# 안 써본 기능 시연. Virtual Console HTTP(127.0.0.1:8088, adb forward 필요)로 차 신호를 넣는다.
# 디버그 빌드는 앱 시계가 10배 빠르다: 실제 6초 = 앱의 1분 (specs/002-redesign-21-features/research.md R5)
#
#   demo.sh climate   풍량·온도를 손으로 바꾼 운전 → "공조 AUTO" 안내
#   demo.sh learn     AUTO를 켠 운전 → "공조 AUTO, 써 보셨네요"
#   demo.sh wiper     와이퍼 단계를 손으로 바꾼 운전 → "오토 와이퍼" 안내
#   demo.sh rain      비(와이퍼) + 내기 순환 5분 → "앞유리 김서림 제거" (다시 하면 뒷유리 열선, 사이드미러 열선)
#   demo.sh highbeam  하이빔을 손으로 켜고 끈 운전 → "하이빔 보조" 안내 (이 에뮬레이터엔 하이빔 보조가 없어 "권할 것 없음")
#   demo.sh night     전조등 켜고 10분 → "앰비언트 라이트" 안내
#   demo.sh highway   시속 120km로 10분(주행거리 증가) → "크루즈 컨트롤" 안내
#   demo.sh heat      난방 27° → "시트 열선" 안내
#   demo.sh multi     조수석·온도·시트 열선·전조등 → 여러 번 하면 하나씩 다른 안내
#   demo.sh reset     앱 기록 지우기(처음 설치 상태)
VC=http://127.0.0.1:8088
ADB=${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb

# VC는 브라우저 세션 신호가 3초 넘게 끊기면 쓰기를 거부한다(emulator_inactive)
( while true; do curl -s -m 2 -X POST "$VC/browser/session?client=demo" >/dev/null; sleep 1; done ) &
HB=$!
trap 'kill $HB' EXIT
say() { echo "$(date +%T)  $*"; }
put() { curl -s -m 5 -X PUT "$VC/vehicle/$1?$2" >/dev/null; say "  $1 $2"; sleep "${3:-1}"; }
quiet() { curl -s -m 5 -X PUT "$VC/vehicle/$1?$2" >/dev/null; }
drive() { put gear gear=Drive; say "▶ 주행 중 (화면: 운전 중이에요, 지도 흐려짐)"; }
park() { put gear gear=Park; say "■ P (화면: 안내 · 음성)"; }
wait_min() { say "  … 앱 시간 $1분 ($(($1 * 6))초)"; sleep $(($1 * 6 + 1)); }

sleep 2
put gear gear=Park
if [ "$1" = reset ]; then
  # 차를 "아무 기능도 안 쓰는" 상태로 되돌린 뒤 앱 기록을 지운다. 켜진 기능이 남아 있으면 앱이 바로 "익힘"으로 기록한다
  for q in "hvac power=true" "hvac auto=false" "hvac dual=false" "hvac ac=false" "hvac recirc=false" "hvac front_defroster=false" \
           "hvac rear_defroster=false" "hvac driver_seat=0" "hvac passenger_seat=0" "hvac driver_temp=24" "hvac fan_speed=1" \
           "steering-wheel-heat level=0" "driving-mode ad_mode_state=ready" "body front_wiper=Off" "body ambient_mode=Off" \
           "lights feature=headlamp_mode&value=Off" "lights feature=high_beam&value=false" \
           "seat kind=occupied&area=passenger&value=false" "seat kind=occupied&area=rear_left&value=false" \
           "seat kind=occupied&area=rear_right&value=false"; do quiet ${q% *} "${q#* }"; done
  sleep 1
  $ADB shell am force-stop --user 10 kr.skku.unusedfeature
  $ADB shell run-as kr.skku.unusedfeature --user 10 rm -f shared_prefs/progress.xml
  $ADB shell sync
  $ADB shell am start --user 10 -n kr.skku.unusedfeature/.MainActivity >/dev/null
  say "차 상태와 앱 기록을 처음으로 되돌렸어요. 화면: 첫 실행"; exit 0
fi
case "$1" in
  learn)
    put hvac power=true; put hvac auto=false; drive
    put hvac auto=true; say "  AUTO 켬 (화면 아래 공조 막대에 AUTO)"; wait_min 1 ;;
  wiper) # 와이퍼 스위치는 3초마다 읽으므로 4초 간격
    put body front_wiper=Off; drive
    put body front_wiper=Low 4; put body front_wiper=High 4; put body front_wiper=Low 4; put body front_wiper=Off 4
    wait_min 1 ;;
  rain)
    put hvac power=true; put hvac recirc=false; put body front_wiper=Off; drive
    put body front_wiper=Low; put hvac recirc=true; say "  비 + 내기 순환"; wait_min 5
    put body front_wiper=Off ;;
  highbeam)
    put lights "feature=headlamp_mode&value=LowBeam"; put lights "feature=high_beam&value=false"; drive
    for v in true false true false; do put lights "feature=high_beam&value=$v" 2; done
    wait_min 1 ;;
  night)
    put lights "feature=high_beam&value=false"; put lights "feature=headlamp_mode&value=LowBeam"; put body ambient_mode=Off
    drive; say "  전조등 켠 채 달리는 중"; wait_min 10 ;;
  highway)
    # 주행거리는 줄어드는 쓰기를 무시한다. 지금 값에서부터 늘린다
    km=$(curl -s -m 5 "$VC/vehicle/state" | python3 -I -c 'import json,sys; print(int(json.load(sys.stdin)["odometer"]["traveled_distance_km"]) + 1)')
    put odometer traveled_distance_km=$km; drive
    say "  주행거리 3초마다 +1km (앱 시간으로 시속 120km)"
    # 첫 두 표본은 1분 창이 안 차서 세지 않는다. 여유 있게 26번(앱 시간 10분 넘김)
    i=0; while [ $i -lt 26 ]; do i=$((i + 1)); curl -s -m 5 -X PUT "$VC/vehicle/odometer?traveled_distance_km=$((km + i))" >/dev/null; sleep 3; done ;;
  heat)
    put hvac power=true; put hvac driver_seat=0; put hvac driver_temp=24; drive
    put hvac driver_temp=27; say "  난방 27°"; wait_min 1 ;;
  multi)
    # 여러 번 돌려도 매번 같은 변화가 생기도록 시작 상태를 맞춘다
    put lights "feature=headlamp_mode&value=Off"; put hvac driver_temp=24; put hvac driver_seat=0
    put seat "kind=occupied&area=passenger&value=false"; drive
    put seat "kind=occupied&area=passenger&value=true"; put hvac driver_temp=21; put hvac driver_seat=2
    put lights "feature=headlamp_mode&value=LowBeam"; put lights "feature=headlamp_mode&value=Off"; wait_min 1 ;;
  *)
    put hvac power=true; put hvac auto=false; put hvac fan_speed=2; put hvac driver_temp=24; drive
    put hvac fan_speed=4 2; put hvac fan_speed=5 2; put hvac driver_temp=23 2; wait_min 1 ;;
esac
park
