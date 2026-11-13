package kr.skku.unusedfeature

enum class Side { LEFT, RIGHT, ABOVE }

/** 차 지도의 구역 7개. 좌표는 car_top 그림(300×560, 앞이 위) 기준 (data-model). */
enum class Zone(val title: String, val icon: Int, val x: Float, val y: Float, val side: Side) {
    WINDSHIELD("앞유리·와이퍼", R.drawable.ic_z_windshield, 150f, 160f, Side.RIGHT),
    HEADLAMP("전조등", R.drawable.ic_z_headlamp, 88f, 66f, Side.RIGHT),
    MIRROR("사이드미러", R.drawable.ic_z_mirror, 24f, 206f, Side.ABOVE),
    CLIMATE("공조 패널", R.drawable.ic_z_climate, 150f, 214f, Side.RIGHT),
    WHEEL("운전대", R.drawable.ic_z_wheel, 105f, 246f, Side.LEFT),
    SEAT("시트", R.drawable.ic_z_seat, 105f, 298f, Side.LEFT),
    CONSOLE("콘솔·실내", R.drawable.ic_z_console, 150f, 362f, Side.RIGHT),
}

/** 근거 종류 */
enum class Kind(val icon: Int) {
    FAN(R.drawable.ic_z_climate), TEMP(R.drawable.ic_k_temp), PASSENGER(R.drawable.ic_z_seat), REAR(R.drawable.ic_z_seat),
    AC(R.drawable.ic_k_snow), HEAT(R.drawable.ic_k_temp), SEAT_HEAT(R.drawable.ic_f_seat_heat), VENT(R.drawable.ic_f_seat_vent),
    LIGHT(R.drawable.ic_z_headlamp), HIGH_BEAM(R.drawable.ic_f_high_beam), WIPER(R.drawable.ic_z_windshield),
    RAIN(R.drawable.ic_k_drop), RECIRC(R.drawable.ic_k_recirc), NIGHT(R.drawable.ic_k_moon), HIGHWAY(R.drawable.ic_f_cruise),
    DRIVE(R.drawable.ic_z_wheel),
}

/** 누를 곳: 자리 이름과 버튼 줄. "*"로 시작하는 버튼을 강조한다. */
class Where(val place: String, vararg labels: String) {
    val buttons = labels.map { it.removePrefix("*") }
    val hi = labels.indexOfFirst { it.startsWith("*") }
}

/**
 * 다루는 기능 18개. 열거 순서 = 같은 안내 횟수일 때의 우선순위(data-model). 문장은 contracts/ui.md.
 * 사용 여부는 [DriveStats.used]로 본다(Tracker가 채움).
 */
enum class Feature(
    val title: String,
    val zone: Zone,
    val icon: Int,
    val speech: String,
    val evidenceTitle: String,
    val whenText: String,
    val where: Where,
    val evidence: Set<Kind>,
    val reason: (DriveStats) -> String,
    val opportunity: (DriveStats) -> Boolean,
    /** 5차: 주행 중 권함 문구(화면·음성 같음, 40자 이하). null이면 주행 중에 권하지 않는다(specs/005-live-tips contracts §3) */
    val live: String? = null,
) {
    AUTO_CLIMATE(
        "공조 AUTO", Zone.CLIMATE, R.drawable.ic_f_auto_climate,
        "이번 운전에서 바람과 온도를 여러 번 손으로 바꾸셨어요. 다음엔 공조 AUTO를 켜 보세요. 알아서 맞춰 줘요.",
        "손으로 바꾼 순간", "온도만 정해 두고 싶을 때. 풍량과 바람 방향은 차가 맞춰요.",
        Where("공조 패널", "A/C", "*AUTO", "DUAL"), setOf(Kind.FAN, Kind.TEMP),
        { "바람과 온도를 손으로 ${it.manualClimateChanges}번 바꾸셨어요." }, { it.manualClimateChanges >= 3 },
        live = "바람과 온도를 자주 바꾸셨네요. 공조 AUTO를 켜 보세요.",
    ),
    AUTO_WIPER(
        "오토 와이퍼", Zone.WINDSHIELD, R.drawable.ic_f_auto_wiper,
        "와이퍼 단계를 여러 번 직접 바꾸셨어요. 레버를 AUTO에 두면 빗물 양에 맞춰 차가 조절해요.",
        "와이퍼를 손으로 바꾼 순간", "비가 오다 말다 할 때. 빗물 양에 맞춰 속도를 바꿔요.",
        Where("오른쪽 레버", "OFF", "*AUTO", "LO", "HI"), setOf(Kind.WIPER),
        { "와이퍼 단계를 손으로 ${it.wiperChanges}번 바꾸셨어요." }, { it.wiperChanges >= 3 },
        live = "와이퍼를 자주 바꾸셨네요. 레버를 AUTO에 두면 차가 맞춰요.",
    ),
    AUTO_LIGHT(
        "오토 라이트", Zone.HEADLAMP, R.drawable.ic_f_auto_light,
        "전조등을 여러 번 직접 켜고 끄셨어요. 라이트 스위치를 AUTO에 두면 차가 알아서 켜요.",
        "전조등을 켜고 끈 순간", "터널이나 해 질 녘. 어두워지면 차가 알아서 켜요.",
        Where("왼쪽 레버", "OFF", "*AUTO", "미등", "전조등"), setOf(Kind.LIGHT),
        { "전조등을 손으로 ${it.manualLightToggles}번 켜고 끄셨어요." }, { it.manualLightToggles >= 2 },
        live = "전조등을 자주 켜고 끄셨네요. 스위치를 AUTO에 두면 알아서 켜요.",
    ),
    HIGH_BEAM_ASSIST(
        "하이빔 보조", Zone.HEADLAMP, R.drawable.ic_f_high_beam,
        "하이빔을 여러 번 직접 켜고 끄셨어요. 하이빔 보조를 켜면 앞차가 보일 때 차가 알아서 바꿔요.",
        "하이빔을 켜고 끈 순간", "가로등 없는 밤길. 앞차가 보이면 하향등으로 바꿔요.",
        Where("왼쪽 레버", "AUTO 위치", "*앞으로 밀기", "표시등 확인"), setOf(Kind.HIGH_BEAM),
        { "하이빔을 손으로 ${it.highBeamToggles}번 켜고 끄셨어요." }, { it.highBeamToggles >= 3 },
        live = "하이빔을 자주 바꾸셨네요. 하이빔 보조를 켜면 차가 바꿔요.",
    ),
    CRUISE(
        "크루즈 컨트롤", Zone.WHEEL, R.drawable.ic_f_cruise,
        "빠른 속도로 오래 달리셨어요. 다음엔 크루즈 컨트롤로 발을 쉬게 해 보세요.",
        "빠르게 달린 구간", "고속도로나 막히지 않는 국도. 정한 속도를 차가 지켜요.",
        Where("운전대 오른쪽", "*CRUISE", "SET−", "RES+"), setOf(Kind.HIGHWAY, Kind.DRIVE),
        { if (it.distanceHeard) "시속 70km 넘게 ${it.highwayMillis / MINUTE}분 달리셨어요." else "${it.driveMillis / MINUTE}분 동안 운전하셨어요." },
        // 주행거리 신호가 없는 차는 1차 조건(연속 20분)
        { if (it.distanceHeard) it.highwayMillis >= 10 * MINUTE else it.driveMillis >= 20 * MINUTE },
        live = "빠르게 오래 달리셨네요. 크루즈 컨트롤로 발을 쉬게 해 보세요.",
    ),
    SEAT_HEAT(
        "시트 열선", Zone.SEAT, R.drawable.ic_f_seat_heat,
        "난방을 높게 트셨어요. 시트 열선을 켜면 몸이 더 빨리 따뜻해져요.",
        "난방을 높인 순간", "추운 날 출발할 때. 공조보다 몸이 먼저 따뜻해져요.",
        Where("공조 패널", "*시트 열선", "통풍", "운전대 열선"), setOf(Kind.HEAT),
        { "난방을 ${deg(it.maxSetTemp)}°로 트셨어요." }, { it.maxSetTemp >= 26f && !it.seatHeatOn },
        live = "난방을 높이셨네요. 시트 열선을 켜면 더 빨리 따뜻해져요.",
    ),
    WHEEL_HEAT(
        "운전대 열선", Zone.WHEEL, R.drawable.ic_f_wheel_heat,
        "시트 열선을 켜셨네요. 이 차에는 운전대 열선도 있어요.",
        "시트 열선을 켠 순간", "손이 시린 겨울 아침. 몇 분 안에 따뜻해져요.",
        Where("공조 패널", "시트 열선", "*운전대 열선", "통풍"), setOf(Kind.SEAT_HEAT),
        { "시트 열선을 켜셨어요. 손도 따뜻하게 할 수 있어요." }, { it.seatHeatOn },
        live = "시트 열선을 켜셨네요. 운전대 열선도 있어요.",
    ),
    SEAT_VENT(
        "통풍 시트", Zone.SEAT, R.drawable.ic_f_seat_vent,
        "에어컨을 낮게 트셨어요. 통풍 시트를 켜면 등이 덜 덥습니다.",
        "에어컨을 낮춘 순간", "더운 날. 시트에서 바람이 나와 등과 허벅지가 덜 더워요.",
        Where("공조 패널", "시트 열선", "*통풍", "A/C"), setOf(Kind.AC),
        { "에어컨을 ${deg(it.minSetTemp)}°로 낮게 트셨어요." },
        { it.acOn && it.minSetTemp <= 21f && !it.seatHeatOn }, // 열선을 켠 사람에게 통풍을 권하지 않는다
        live = "에어컨을 낮추셨네요. 통풍 시트를 켜면 등이 덜 더워요.",
    ),
    DUAL_CLIMATE(
        "동승석 개별 온도", Zone.CLIMATE, R.drawable.ic_f_dual,
        "옆자리에 동승자가 있었어요. DUAL을 켜면 조수석 온도를 따로 맞출 수 있어요.",
        "동승자와 온도를 바꾼 순간", "옆 사람과 덥고 추운 느낌이 다를 때.",
        Where("공조 패널", "AUTO", "*DUAL", "조수석 온도"), setOf(Kind.PASSENGER, Kind.TEMP),
        { "옆자리에 동승자가 있었고 온도를 바꾸셨어요." }, { it.passengerSeated && it.tempChanges >= 1 },
        live = "옆자리에 동승자가 있어요. DUAL로 조수석 온도를 따로 맞춰요.",
    ),
    PASSENGER_SEAT(
        "동승석 시트 열선·통풍", Zone.SEAT, R.drawable.ic_f_passenger_seat,
        "옆자리에 동승자가 있었어요. 조수석 시트 열선과 통풍도 따로 켤 수 있어요.",
        "동승자가 탄 순간", "동승자가 춥거나 더워할 때.",
        Where("공조 패널 조수석 쪽", "*조수석 열선", "조수석 통풍"), setOf(Kind.PASSENGER, Kind.SEAT_HEAT, Kind.VENT),
        { "운전석 시트만 켜고 조수석은 꺼져 있었어요." }, { it.passengerSeated && (it.seatHeatOn || SEAT_VENT in it.used) },
        live = "옆자리에 동승자가 있어요. 조수석 열선·통풍도 따로 켤 수 있어요.",
    ),
    FRONT_DEFROST(
        "앞유리 김서림 제거", Zone.WINDSHIELD, R.drawable.ic_f_front_defrost,
        "비 오는 날 내기 순환을 오래 켜 두셨어요. 앞유리에 김이 서리면 앞유리 김서림 제거 버튼을 눌러 보세요.",
        "비 온 구간과 내기 순환", "비 오는 날이나 추운 아침에 앞유리가 뿌옇게 될 때.",
        Where("공조 패널", "*앞유리 김서림", "뒷유리 열선", "A/C"), setOf(Kind.RAIN, Kind.RECIRC),
        { "비가 오는 동안 내기 순환을 ${it.rainRecircMillis / MINUTE}분 켜 두셨어요." }, { it.rainRecircMillis >= 5 * MINUTE },
        live = "비 오는 동안 내기 순환이 길었어요. 김이 서리면 김서림을 누르세요.",
    ),
    REAR_DEFROST(
        "뒷유리 열선", Zone.CLIMATE, R.drawable.ic_f_rear_defrost,
        "비 오는 길을 오래 달리셨어요. 뒷유리 열선을 켜면 뒤가 더 잘 보여요.",
        "비가 온 구간", "비 오는 날이나 서리 낀 아침. 뒷유리 물기와 김을 말려요.",
        Where("공조 패널", "앞유리 김서림", "*뒷유리 열선", "A/C"), setOf(Kind.RAIN),
        { "와이퍼가 ${it.rainMillis / MINUTE}분 동안 움직였어요." }, { it.rainMillis >= 5 * MINUTE },
        live = "비 오는 길을 오래 달리셨네요. 뒷유리 열선을 켜면 뒤가 잘 보여요.",
    ),
    MIRROR_HEAT(
        "사이드미러 열선", Zone.MIRROR, R.drawable.ic_f_mirror_heat,
        "비 오는 길을 오래 달리셨어요. 사이드미러 열선을 켜면 거울의 물방울이 빨리 말라요.",
        "비가 온 구간", "사이드미러에 물방울이 맺히거나 성에가 꼈을 때.",
        Where("공조 패널", "뒷유리 열선", "*미러 열선"), setOf(Kind.RAIN),
        { "와이퍼가 ${it.rainMillis / MINUTE}분 동안 움직였어요." }, { it.rainMillis >= 5 * MINUTE },
        live = "비 오는 길을 오래 달리셨네요. 미러 열선을 켜면 물방울이 빨리 말라요.",
    ),
    AUTO_RECIRC(
        "오토 내/외기", Zone.CLIMATE, R.drawable.ic_f_auto_recirc,
        "내기 순환을 여러 번 직접 바꾸셨어요. 오토 내/외기를 켜면 바깥 공기에 맞춰 차가 바꿔요.",
        "내기·외기를 바꾼 순간", "터널이나 매연 많은 길. 차가 알아서 내기로 바꿔요.",
        Where("공조 패널", "내기", "*오토 내/외기", "외기"), setOf(Kind.RECIRC),
        { "내기와 외기를 손으로 ${it.recircToggles}번 바꾸셨어요." }, { it.recircToggles >= 2 },
        live = "내기와 외기를 자주 바꾸셨네요. 오토 내/외기를 켜면 차가 바꿔요.",
    ),
    MAX_AC(
        "MAX A/C", Zone.CLIMATE, R.drawable.ic_f_max_ac,
        "에어컨을 가장 낮게 트셨어요. MAX A/C를 누르면 가장 빨리 시원해져요.",
        "에어컨을 가장 낮춘 순간", "햇볕에 달궈진 차에 막 탔을 때.",
        Where("공조 패널", "A/C", "*MAX A/C", "풍량"), setOf(Kind.AC),
        { "에어컨을 ${deg(it.minSetTemp)}°로 가장 낮게 트셨어요." }, { it.acOn && it.minSetTemp <= 17f },
        live = "에어컨을 가장 낮추셨네요. MAX A/C가 가장 빨리 시원해요.",
    ),
    DRIVE_MODE(
        "주행 모드", Zone.CONSOLE, R.drawable.ic_f_drive_mode,
        "고속 구간을 오래 달리셨어요. 주행 모드를 ECO로 바꾸면 전비가 좋아져요.",
        "빠르게 달린 구간", "고속도로는 ECO, 경쾌하게 달리고 싶으면 SPORT.",
        Where("기어 옆 콘솔", "NORMAL", "*ECO", "SPORT"), setOf(Kind.HIGHWAY),
        { "시속 70km 넘게 ${it.highwayMillis / MINUTE}분, NORMAL 모드로 달리셨어요." },
        { it.distanceHeard && it.highwayMillis >= 15 * MINUTE },
        live = "고속으로 오래 달리셨네요. ECO 모드로 바꾸면 전비가 좋아져요.",
    ),
    REAR_SEAT_HEAT(
        "뒷좌석 시트 열선", Zone.SEAT, R.drawable.ic_f_rear_seat,
        "뒷좌석에 탄 사람이 있었어요. 뒷좌석 시트 열선도 켤 수 있어요.",
        "뒷좌석에 사람이 탄 순간", "추운 날 뒷좌석에 가족이 탔을 때.",
        Where("뒷좌석 공조", "*뒷좌석 왼쪽", "뒷좌석 오른쪽"), setOf(Kind.REAR, Kind.SEAT_HEAT),
        { "운전석 시트 열선을 켰고 뒷좌석에 사람이 있었어요." }, { it.rearSeated && it.seatHeatOn },
    ),
    AMBIENT(
        "앰비언트 라이트", Zone.CONSOLE, R.drawable.ic_f_ambient,
        "밤길을 오래 달리셨어요. 앰비언트 라이트를 켜면 실내가 은은하게 밝아져요.",
        "전조등을 켠 구간", "밤 운전. 실내 분위기가 바뀌고 손 닿는 곳이 잘 보여요.",
        Where("화면 설정", "설정", "조명", "*앰비언트"), setOf(Kind.NIGHT),
        { "전조등을 켜고 ${it.nightMillis / MINUTE}분 달리셨어요." }, { it.nightMillis >= 10 * MINUTE },
    ),
}

/** 24.0 → "24", 23.5 → "23.5" */
fun deg(t: Float): String = if (t % 1f == 0f) t.toInt().toString() else t.toString()
