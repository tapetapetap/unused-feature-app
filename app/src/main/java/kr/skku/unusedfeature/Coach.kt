package kr.skku.unusedfeature

const val MINUTE = 60_000L
private const val HIGHWAY_KMH = 70.0

/** 근거 하나. [at]·[end]는 운전 시작부터의 ms, 순간이면 [end] = -1 */
data class Mark(val kind: Kind, val at: Long, val end: Long, val text: String)

/** 한 번의 운전 동안 센 값. 메모리에만 둔다. */
class DriveStats {
    var driveMillis = 0L
    var manualClimateChanges = 0
    var tempChanges = 0
    var minSetTemp = Float.MAX_VALUE
    var maxSetTemp = -Float.MAX_VALUE
    var manualLightToggles = 0
    var highBeamToggles = 0
    var wiperChanges = 0
    var recircToggles = 0
    var rainMillis = 0L
    var rainRecircMillis = 0L
    var nightMillis = 0L
    var highwayMillis = 0L
    var acOn = false
    var seatHeatOn = false
    var passengerSeated = false
    var rearSeated = false
    var distanceHeard = false

    /** 이번 운전에서 쓴 기능(운전 시작 때 이미 켜져 있던 것 포함) */
    val used = mutableSetOf<Feature>()

    /** 사용 신호를 한 번이라도 받은 기능. 못 받은 기능은 안내하지 않는다(원칙 II) */
    val heard = mutableSetOf<Feature>()
    val marks = mutableListOf<Mark>()
}

/** 켜져 있던 시간 누적. [kind]가 있으면 켜짐 구간마다 근거 막대를 남긴다 */
private class Span(private val kind: Kind?, private val text: String) {
    var total = 0L
        private set
    private var since = -1L

    fun reset(on: Boolean) { total = 0; since = if (on) 0 else -1 }

    fun set(on: Boolean, t: Long, marks: MutableList<Mark>) {
        if (on && since < 0) since = t
        if (!on && since >= 0) {
            total += t - since
            kind?.let { marks += Mark(it, since, t, text) }
            since = -1
        }
    }
}

/**
 * 차 신호를 받아 현재 상태와 이번 운전의 DriveStats를 갱신한다.
 * 직전 값(운전 전 값 포함)과 달라야 "변화"로 센다. 구독 직후 첫 값은 변화가 아니다.
 * 시각은 [clock](앱 시계, 디버그 10배)으로 잰다. 횟수·근거·구간은 운전 중에만 센다.
 */
class Tracker(private val clock: () -> Long) {
    var stats = DriveStats()
        private set
    private var start = -1L // -1 = 지켜보는 운전 없음
    val driving get() = start >= 0
    fun minutesIntoDrive(): Long? = if (driving) (clock() - start) / MINUTE else null
    private fun t() = clock() - start

    private var auto: Boolean? = null
    private var dual: Boolean? = null
    private var ac: Boolean? = null
    private var passenger: Boolean? = null
    private var fan: Int? = null
    private var temp: Float? = null
    private var seatHeat: Int? = null
    private var vent: Int? = null
    private var wheelHeat: Int? = null
    private var lightOn: Boolean? = null
    private var lightAuto: Boolean? = null
    private var cruise: Boolean? = null
    private var wiperOn: Boolean? = null
    private var wiperMode: String? = null // OFF, AUTO, 미스트, INT, LO, HI
    private var highBeamOn: Boolean? = null
    private var highBeamAuto: Boolean? = null
    private var recirc: Boolean? = null
    private var autoRecirc: Boolean? = null
    private var maxAc: Boolean? = null
    private var frontDefrost: Boolean? = null
    private var rearDefrost: Boolean? = null
    private var mirrorHeat: Int? = null
    private var driveMode: String? = null
    private var passengerSeat: Int? = null
    private var rearSeat: Int? = null
    private var rearSeatedNow: Boolean? = null
    private var ambient: Boolean? = null
    private var km: Float? = null

    private val rain = Span(Kind.RAIN, "와이퍼")
    private val rainRecirc = Span(null, "")
    private val night = Span(Kind.NIGHT, "전조등")
    private val samples = ArrayDeque<Pair<Long, Float>>() // (운전 시각, km)
    private var highwayFrom = -1L
    private var highwayTo = -1L

    /** 새 운전. 이미 켜져 있는 상태는 이번 운전에도 켜진 것으로 보고 근거도 0분에 남긴다. */
    fun startDrive() {
        stats = DriveStats()
        start = clock()
        rain.reset(raining()); rainRecirc.reset(raining() && recirc == true); night.reset(lightOn == true)
        samples.clear(); km?.let { samples.addLast(0L to it) }
        highwayFrom = -1; highwayTo = -1
        if ((seatHeat ?: 0) > 0) mark(Kind.SEAT_HEAT, "시트 열선 ${seatHeat}단")
        if (passenger == true) mark(Kind.PASSENGER, "조수석 탑승")
        if (rearSeatedNow == true) mark(Kind.REAR, "뒷좌석 탑승")
        temp?.let {
            if (ac == true && it <= 21f) mark(Kind.AC, "에어컨 ${deg(it)}°")
            if (it >= 26f) mark(Kind.HEAT, "난방 ${deg(it)}°")
        }
        refresh()
    }

    fun endDrive(): DriveStats = stats.also {
        val end = t()
        it.driveMillis = end
        rain.set(false, end, it.marks); rainRecirc.set(false, end, it.marks); night.set(false, end, it.marks)
        it.rainMillis = rain.total; it.rainRecircMillis = rainRecirc.total; it.nightMillis = night.total
        closeHighway()
        if (!it.distanceHeard) it.marks += Mark(Kind.DRIVE, 0, end, "운전")
        start = -1
    }

    private fun mark(kind: Kind, text: String) {
        if (driving) stats.marks += Mark(kind, t(), -1, text)
    }

    private fun raining() = wiperOn == true || wiperMode in setOf("미스트", "INT", "LO", "HI")

    private fun spans() {
        if (!driving) return
        val now = t()
        rain.set(raining(), now, stats.marks)
        rainRecirc.set(raining() && recirc == true, now, stats.marks)
        night.set(lightOn == true, now, stats.marks)
    }

    fun auto(on: Boolean) { auto = on; refresh() }
    fun dual(on: Boolean) { dual = on; refresh() }
    fun cruise(on: Boolean) { cruise = on; refresh() }
    fun wheelHeat(level: Int) { wheelHeat = level; refresh() }
    fun autoRecirc(on: Boolean) { autoRecirc = on; refresh() }
    fun maxAc(on: Boolean) { maxAc = on; refresh() }
    fun frontDefrost(on: Boolean) { frontDefrost = on; refresh() }
    fun rearDefrost(on: Boolean) { rearDefrost = on; refresh() }
    fun mirrorHeat(level: Int) { mirrorHeat = level; refresh() }
    fun driveMode(mode: String) { driveMode = mode; refresh() }
    fun passengerSeat(level: Int) { passengerSeat = level; refresh() } // 음수 = 통풍
    fun rearSeat(level: Int) { rearSeat = level; refresh() }
    fun ambient(on: Boolean) { ambient = on; refresh() }
    fun highBeamSwitch(auto: Boolean) { highBeamAuto = auto; refresh() }
    fun lightSwitch(auto: Boolean) { lightAuto = auto; refresh() }

    fun ac(on: Boolean) {
        if (on && ac != true) temp?.let { if (it <= 21f) mark(Kind.AC, "에어컨 ${deg(it)}°") }
        ac = on; refresh()
    }

    fun passenger(seated: Boolean) {
        if (seated && passenger != true) mark(Kind.PASSENGER, "조수석 탑승")
        passenger = seated; refresh()
    }

    fun rearSeated(seated: Boolean) {
        if (seated && rearSeatedNow != true) mark(Kind.REAR, "뒷좌석 탑승")
        rearSeatedNow = seated; refresh()
    }

    fun seatHeat(level: Int) {
        val was = seatHeat ?: 0
        if (level > 0 && was <= 0) mark(Kind.SEAT_HEAT, "시트 열선 ${level}단")
        if (level < 0 && was >= 0) mark(Kind.VENT, "통풍 ${-level}단") // 시트 온도 음수 = 통풍
        seatHeat = level; refresh()
    }

    fun vent(level: Int) {
        if (level > 0 && (vent ?: 0) <= 0) mark(Kind.VENT, "통풍 ${level}단")
        vent = level; refresh()
    }

    fun fan(speed: Int) {
        if (driving && fan != null && fan != speed && auto != true) {
            stats.manualClimateChanges++
            mark(Kind.FAN, "풍량 $fan→$speed")
        }
        fan = speed
    }

    fun temp(celsius: Float) {
        val was = temp
        if (driving && was != null && was != celsius) {
            stats.tempChanges++
            stats.manualClimateChanges++
            mark(Kind.TEMP, "온도 ${deg(was)}→${deg(celsius)}")
            // 통풍 기준(21°)·MAX A/C 기준(17°)을 넘을 때만 근거로 남긴다
            if (ac == true && ((celsius <= 21f && was > 21f) || (celsius <= 17f && was > 17f))) mark(Kind.AC, "에어컨 ${deg(celsius)}°")
            if (celsius >= 26f && was < 26f) mark(Kind.HEAT, "난방 ${deg(celsius)}°")
        }
        temp = celsius; refresh()
    }

    /** 전조등 켜짐·꺼짐 변화. 스위치가 AUTO가 아니라고 확인된 때만 손으로 한 조작으로 센다 */
    fun light(on: Boolean) {
        if (driving && lightOn != null && lightOn != on && lightAuto == false) {
            stats.manualLightToggles++
            mark(Kind.LIGHT, if (on) "전조등 켬" else "전조등 끔")
        }
        lightOn = on; spans()
    }

    /** 하이빔 켜짐·꺼짐. 스위치가 AUTO(하이빔 보조)가 아니라고 확인된 때만 손으로 한 조작으로 센다 */
    fun highBeam(on: Boolean) {
        if (driving && highBeamOn != null && highBeamOn != on && highBeamAuto == false) {
            stats.highBeamToggles++
            mark(Kind.HIGH_BEAM, if (on) "하이빔 켬" else "하이빔 끔")
        }
        highBeamOn = on
    }

    /** 와이퍼 스위치. AUTO가 아닌 단계끼리 바뀐 것만 손으로 바꾼 것으로 센다 */
    fun wiperSwitch(mode: String) {
        val was = wiperMode
        if (driving && was != null && was != mode && was != "AUTO" && mode != "AUTO") {
            stats.wiperChanges++
            mark(Kind.WIPER, "와이퍼 $was→$mode")
        }
        wiperMode = mode; spans(); refresh()
    }

    fun wiper(on: Boolean) { wiperOn = on; spans() }

    fun recirc(on: Boolean) {
        if (driving && recirc != null && recirc != on && autoRecirc != true) {
            stats.recircToggles++
            mark(Kind.RECIRC, if (on) "내기 순환" else "외기")
        }
        recirc = on; spans()
    }

    /**
     * 누적 주행거리(km). 1분 이상 앞선 표본과의 평균 시속이 70km 이상이면 직전 표본부터 지금까지를 고속 구간으로 본다.
     * ponytail: 차속 신호가 SDK에 없어 주행거리로 추정. 차속을 받게 되면 그것으로 바꾼다
     */
    fun distance(value: Float) {
        km = value; refresh()
        if (!driving) return
        val now = t()
        val prev = samples.lastOrNull()
        if (prev != null) {
            val base = samples.lastOrNull { it.first <= now - MINUTE }
            val fast = base != null && (value - base.second) / ((now - base.first) / 3_600_000.0) >= HIGHWAY_KMH
            if (fast) {
                stats.highwayMillis += now - prev.first
                if (highwayTo != prev.first) { closeHighway(); highwayFrom = prev.first }
                highwayTo = now
            } else closeHighway()
        }
        samples.addLast(now to value)
        while (samples.size > 2 && samples[1].first <= now - MINUTE) samples.removeFirst()
    }

    private fun closeHighway() {
        if (highwayFrom >= 0) stats.marks += Mark(Kind.HIGHWAY, highwayFrom, highwayTo, "시속 70km 이상")
        highwayFrom = -1; highwayTo = -1
    }

    /** 지금까지의 신호로 "썼다"가 성립한 기능 (정차 중 조작 포함) */
    fun usedNow(): Set<Feature> = stats.used.toSet()

    private fun refresh() = with(stats) {
        fun use(f: Feature, on: Boolean, known: Boolean) {
            if (known) heard += f
            if (on) used += f
        }
        use(Feature.AUTO_CLIMATE, auto == true, auto != null)
        use(Feature.AUTO_WIPER, wiperMode == "AUTO", wiperMode != null)
        use(Feature.AUTO_LIGHT, lightAuto == true, lightAuto != null)
        use(Feature.HIGH_BEAM_ASSIST, highBeamAuto == true, highBeamAuto != null)
        use(Feature.CRUISE, cruise == true, cruise != null)
        use(Feature.SEAT_HEAT, (seatHeat ?: 0) > 0, seatHeat != null)
        use(Feature.WHEEL_HEAT, (wheelHeat ?: 0) > 0, wheelHeat != null)
        // 시트 온도 음수 = 통풍(Virtual Console 값 정의: -3~-1 Ventilation). 통풍 전용 신호가 없는 차도 있다
        use(Feature.SEAT_VENT, (vent ?: 0) > 0 || (seatHeat ?: 0) < 0, vent != null || seatHeat != null)
        use(Feature.DUAL_CLIMATE, dual == true, dual != null)
        use(Feature.PASSENGER_SEAT, (passengerSeat ?: 0) != 0, passengerSeat != null)
        use(Feature.FRONT_DEFROST, frontDefrost == true, frontDefrost != null)
        use(Feature.REAR_DEFROST, rearDefrost == true, rearDefrost != null)
        use(Feature.MIRROR_HEAT, (mirrorHeat ?: 0) > 0, mirrorHeat != null)
        use(Feature.AUTO_RECIRC, autoRecirc == true, autoRecirc != null)
        use(Feature.MAX_AC, maxAc == true, maxAc != null)
        use(Feature.DRIVE_MODE, driveMode != null && driveMode != "NORMAL", driveMode != null)
        use(Feature.REAR_SEAT_HEAT, (rearSeat ?: 0) > 0, rearSeat != null)
        use(Feature.AMBIENT, ambient == true, ambient != null)
        acOn = acOn || ac == true
        passengerSeated = passengerSeated || passenger == true
        rearSeated = rearSeated || rearSeatedNow == true
        seatHeatOn = seatHeatOn || (seatHeat ?: 0) > 0
        distanceHeard = distanceHeard || km != null
        temp?.let { minSetTemp = minOf(minSetTemp, it); maxSetTemp = maxOf(maxSetTemp, it) }
    }
}

/** 운전 끝 판단 결과. [pick] 안내할 기능, [confirmed] 안내했던 기능 중 지난 운전 끝 이후 익힌 것 */
data class Outcome(
    val pick: Feature?,
    val confirmed: Feature?,
    val reason: String,
    val marks: List<Mark>,
    val used: Set<Feature>,
    val driveMillis: Long,
)

object Coach {
    const val MAX_SHOWN = 3
    private const val MAX_MARKS = 60

    /**
     * [learnedBefore]: 지난 운전 끝 시점의 익힘 목록. [learnedNow]: 지금 익힘 목록(정차 중에 써 본 것 포함).
     * [unsupported]: 그 차에 없다고 확인된 기능(data-model "지원 기능"). 신호가 와도 권하지 않는다.
     * 1분 미만 운전이면 null.
     */
    fun finish(
        s: DriveStats, shown: Map<Feature, Int>, learnedBefore: Set<Feature>, learnedNow: Set<Feature>,
        unsupported: Set<Feature> = emptySet(),
    ): Outcome? {
        if (s.driveMillis < MINUTE) return null
        val count = { f: Feature -> shown[f] ?: 0 }
        val pick = Feature.entries
            .filter { it in s.heard && it !in unsupported && it.opportunity(s) && it !in s.used && it !in learnedNow && count(it) < MAX_SHOWN }
            .minByOrNull(count) // 같으면 먼저 나온 것 = 표 순서
        val confirmed = Feature.entries.firstOrNull { it in learnedNow && it !in learnedBefore && count(it) > 0 }
        return Outcome(
            pick, confirmed,
            pick?.reason?.invoke(s).orEmpty(),
            pick?.let { p -> s.marks.filter { it.kind in p.evidence }.take(MAX_MARKS) }.orEmpty(),
            s.used.toSet(), s.driveMillis,
        )
    }
}

/** 근거 저장 형식: 한 줄에 `kind|at|end|text` */
object Marks {
    fun encode(marks: List<Mark>) = marks.joinToString("\n") { "${it.kind}|${it.at}|${it.end}|${it.text}" }

    fun decode(s: String?): List<Mark> = s.orEmpty().lines().mapNotNull { line ->
        val p = line.split('|', limit = 4)
        if (p.size < 4) return@mapNotNull null
        runCatching { Mark(Kind.valueOf(p[0]), p[1].toLong(), p[2].toLong(), p[3]) }.getOrNull()
    }
}

/** 추천 기록 하나 = 안내가 나간 운전 하나(003 data-model "Record") */
data class Record(val at: Long, val pick: Feature, val reason: String, val driveMs: Long, val marks: List<Mark>)

/** 추천 기록 저장 형식: 기록끼리 \u001E, 필드끼리 \u001F, 근거는 [Marks] 형식. 화면 글자에는 이 제어 문자가 없다 */
object History {
    private const val RS = '\u001E'
    private const val US = '\u001F'

    fun encode(records: List<Record>) = records.joinToString("$RS") { r ->
        listOf(r.at, r.pick.name, r.reason, r.driveMs, Marks.encode(r.marks)).joinToString("$US")
    }

    /** 필드 수·숫자·기능 이름을 읽을 수 없는 기록은 건너뛴다(FR-009) */
    fun decode(s: String?): List<Record> = s.orEmpty().split(RS).mapNotNull { rec ->
        val p = rec.split(US)
        if (p.size != 5) return@mapNotNull null
        runCatching { Record(p[0].toLong(), Feature.valueOf(p[1]), p[2], p[3].toLong(), Marks.decode(p[4])) }.getOrNull()
    }

    /** 기존 문자열을 다시 쓰지 않고 뒤에 이어 붙인다. 지금 읽을 수 없는 기록도 남는다(research R9) */
    fun append(old: String?, r: Record) = if (old.isNullOrEmpty()) encode(listOf(r)) else "$old$RS${encode(listOf(r))}"
}
