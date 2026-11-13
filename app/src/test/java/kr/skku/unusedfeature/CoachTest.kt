package kr.skku.unusedfeature

import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.constant.area.SeatValue
import kr.skku.unusedfeature.Feature.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachTest {

    private var now = 0L

    /** 6차 근거 카드 오른쪽 숫자(contracts §4): 순간만 n번, 구간만 합친 분, 섞이면 없음 */
    @Test fun evidenceCountRules() {
        val point = { at: Long -> Mark(Kind.FAN, at * MINUTE, -1, "풍량") }
        val span = { from: Long, to: Long -> Mark(Kind.RAIN, from * MINUTE, to * MINUTE, "와이퍼") }
        assertEquals("2번", evidenceCount(listOf(point(1), point(2)), AUTO_CLIMATE.evidence))
        assertEquals("8분", evidenceCount(listOf(span(0, 3), span(5, 10)), REAR_DEFROST.evidence))
        assertEquals("", evidenceCount(listOf(span(3, 15), point(4)), FRONT_DEFROST.evidence)) // 앞유리 김서림: 비 구간 + 내기 순환 켬
        assertEquals("", evidenceCount(listOf(span(3, 15)), FRONT_DEFROST.evidence)) // 내기 순환이 처음부터 켜져 표시가 없어도(코드 리뷰)
    }

    /** 가짜 시계를 [ms]만큼 흘린다 */
    private fun Tracker.wait(ms: Long) { now += ms }

    /** 모든 사용 신호가 "꺼짐"으로 들어온 차. 실제 차처럼 구독 직후 현재값이 먼저 온다. */
    private fun Tracker.allOff() {
        auto(false); dual(false); ac(false); passenger(false); fan(1); temp(24f)
        seatHeat(0); vent(0); wheelHeat(0); lightSwitch(false); light(false); cruise(false)
        wiper(false); wiperSwitch("OFF"); highBeam(false); highBeamSwitch(false)
        recirc(false); autoRecirc(false); maxAc(false); frontDefrost(false); rearDefrost(false); mirrorHeat(0)
        driveMode("NORMAL"); passengerSeat(0); rearSeat(0); rearSeated(false); ambient(false)
    }

    /** 운전 시작 → [block] → 최소 [millis] 길이로 끝낸 Stats */
    private fun drive(millis: Long = 5 * MINUTE, before: Tracker.() -> Unit = { allOff() }, block: Tracker.() -> Unit): DriveStats {
        val t = Tracker { now }
        t.before()
        val start = now
        t.startDrive()
        t.block()
        now = maxOf(now, start + millis)
        return t.endDrive()
    }

    private fun finish(s: DriveStats, shown: Map<Feature, Int> = emptyMap(), before: Set<Feature> = emptySet()) =
        Coach.finish(s, shown, before, before + s.used)

    @Test fun firstValueIsNotAChange() {
        val s = drive(before = {}) { fan(3); temp(22f) }
        assertEquals(0, s.manualClimateChanges)
        assertEquals(0, s.tempChanges)
    }

    @Test fun valuesBeforeDriveAreBaseline() {
        val s = drive(before = { fan(3) }) { fan(3); fan(4) }
        assertEquals(1, s.manualClimateChanges)
    }

    @Test fun fanChangesWhileAutoOnAreNotManual() {
        val s = drive { auto(true); fan(2); fan(3); fan(4) }
        assertEquals(0, s.manualClimateChanges)
        assertTrue(AUTO_CLIMATE in s.used)
    }

    @Test fun autoClimateOpportunityAfterThreeManualChanges() {
        val s = drive { fan(2); fan(3); temp(23f) }
        assertEquals(3, s.manualClimateChanges)
        assertTrue(AUTO_CLIMATE.opportunity(s))
        assertFalse(AUTO_CLIMATE in s.used)
        assertEquals(AUTO_CLIMATE, finish(s)?.pick)
    }

    @Test fun stateAlreadyOnAtDriveStartCountsAsUsed() {
        val s = drive(before = { allOff(); auto(true) }) { }
        assertTrue(AUTO_CLIMATE in s.used)
    }

    @Test fun firstSixOpportunities() {
        assertTrue(DUAL_CLIMATE.opportunity(drive { passenger(true); temp(21f) }))
        assertFalse(DUAL_CLIMATE.opportunity(drive { temp(21f) }))
        assertTrue(WHEEL_HEAT.opportunity(drive { seatHeat(2) }))
        assertFalse(WHEEL_HEAT.opportunity(drive { seatHeat(0) }))
        assertTrue(SEAT_VENT.opportunity(drive { ac(true); temp(20f) }))
        assertFalse(SEAT_VENT.opportunity(drive { ac(true); temp(24f) }))
        assertFalse(SEAT_VENT.opportunity(drive { ac(true); temp(20f); seatHeat(2) }))
        assertTrue(AUTO_LIGHT.opportunity(drive { light(true); light(false) }))
        assertFalse(AUTO_LIGHT.opportunity(drive { lightSwitch(true); light(true); light(false) }))
        // 주행거리 신호가 없는 차: 크루즈는 1차 조건(20분 운전)
        assertTrue(CRUISE.opportunity(drive(millis = 20 * MINUTE) { }))
        assertFalse(CRUISE.opportunity(drive(millis = 19 * MINUTE) { }))
    }

    @Test fun firstSixUsed() {
        assertTrue(DUAL_CLIMATE in drive { dual(true) }.used)
        assertTrue(WHEEL_HEAT in drive { wheelHeat(1) }.used)
        assertTrue(SEAT_VENT in drive { vent(1) }.used)
        assertTrue(SEAT_VENT in drive { seatHeat(-2) }.used) // 시트 온도 음수 = 통풍
        assertFalse(WHEEL_HEAT.opportunity(drive { seatHeat(-2) }))
        assertTrue(AUTO_LIGHT in drive { lightSwitch(true) }.used)
        assertTrue(CRUISE in drive { cruise(true) }.used)
    }

    @Test fun lightTogglesNeedKnownManualSwitch() {
        // 스위치를 한 번도 못 읽었으면 수동 조작인지 모른다 → 세지 않는다
        val s = drive(before = {}) { light(false); light(true); light(false) }
        assertEquals(0, s.manualLightToggles)
    }

    @Test fun featureWithoutUsedSignalIsNeverSuggested() {
        // AUTO 신호를 못 받는 차: 풍량이 바뀌어도 AUTO를 권하지 않는다(써도 확인할 수 없으므로)
        val s = drive(before = { fan(1) }) { fan(2); fan(3); fan(4) }
        assertTrue(AUTO_CLIMATE.opportunity(s))
        assertNull(finish(s)?.pick)
    }

    @Test fun shortDriveIsIgnored() {
        val s = drive(millis = 59_000) { fan(2); fan(3); fan(4) }
        assertNull(finish(s))
    }

    @Test fun noMissedFeatureMeansSilence() {
        assertNull(finish(drive { })?.pick)
    }

    @Test fun picksLeastShownThenTableOrder() {
        val s = drive { fan(2); fan(3); fan(4); seatHeat(1) } // AUTO_CLIMATE, WHEEL_HEAT 둘 다 기회
        assertEquals(AUTO_CLIMATE, finish(s)?.pick)
        assertEquals(WHEEL_HEAT, finish(s, mapOf(AUTO_CLIMATE to 1))?.pick)
    }

    @Test fun learnedAndThriceShownAreExcluded() {
        val s = drive { fan(2); fan(3); fan(4) }
        assertNull(finish(s, before = setOf(AUTO_CLIMATE))?.pick)
        assertNull(finish(s, mapOf(AUTO_CLIMATE to 3))?.pick)
        assertEquals(AUTO_CLIMATE, finish(s, mapOf(AUTO_CLIMATE to 2))?.pick)
    }

    @Test fun confirmsFeatureShownBeforeAndUsedThisDrive() {
        val s = drive { auto(true) }
        assertEquals(AUTO_CLIMATE, finish(s, mapOf(AUTO_CLIMATE to 1))?.confirmed)
        assertNull(finish(s)?.confirmed) // 안내한 적 없으면 확인 문장 없음
        assertNull(finish(s, mapOf(AUTO_CLIMATE to 1), setOf(AUTO_CLIMATE))?.confirmed) // 이미 익힘
    }

    @Test fun confirmsFeatureTriedWhileParkedBeforeThisDrive() {
        // 안내 직후 정차 중에 AUTO를 눌러 보고 끈 뒤 다음 운전 → 그래도 확인 문장이 나와야 한다
        val o = Coach.finish(drive { }, mapOf(AUTO_CLIMATE to 1), learnedBefore = emptySet(), learnedNow = setOf(AUTO_CLIMATE))
        assertEquals(AUTO_CLIMATE, o?.confirmed)
    }

    @Test fun usedSignalsReportedImmediately() {
        val t = Tracker { now }
        assertEquals(emptySet<Feature>(), t.usedNow())
        t.wheelHeat(2)
        assertEquals(setOf(WHEEL_HEAT), t.usedNow())
    }

    @Test fun marksRecordDriveTimeAndText() {
        val s = drive { wait(3 * MINUTE); fan(2); wait(5 * MINUTE); temp(23f) }
        assertEquals(Mark(Kind.FAN, 3 * MINUTE, -1, "풍량 1→2"), s.marks[0])
        assertEquals(Mark(Kind.TEMP, 8 * MINUTE, -1, "온도 24→23"), s.marks[1])
    }

    @Test fun marksBeforeDriveAreNotRecorded() {
        val t = Tracker { now }
        t.allOff(); t.fan(3); t.fan(4)
        t.startDrive()
        assertTrue(t.stats.marks.none { it.kind == Kind.FAN })
    }

    @Test fun outcomeCarriesReasonUsedAndOnlyPickEvidence() {
        val s = drive(millis = 23 * MINUTE) { fan(2); fan(3); temp(23f); seatHeat(2) }
        val o = finish(s, mapOf(WHEEL_HEAT to 3))!!
        assertEquals(AUTO_CLIMATE, o.pick)
        assertEquals("바람과 온도를 손으로 3번 바꾸셨어요.", o.reason)
        assertEquals(3, o.marks.size)
        assertTrue(o.marks.all { it.kind in AUTO_CLIMATE.evidence })
        assertEquals(s.used, o.used)
        assertEquals(23 * MINUTE, o.driveMillis)
    }

    @Test fun wholeDriveIsAMarkWhenDistanceIsUnknown() {
        val s = drive(millis = 25 * MINUTE) { }
        assertEquals(Mark(Kind.DRIVE, 0, 25 * MINUTE, "운전"), s.marks.last())
        assertEquals(CRUISE, finish(s)?.pick)
        assertEquals(listOf(Kind.DRIVE), finish(s)?.marks?.map { it.kind })
    }

    // ---- 2차: 새 기능 12개 ----

    @Test fun highBeamAssistUsedBySwitch() {
        assertTrue(HIGH_BEAM_ASSIST in drive { highBeamSwitch(true) }.used)
        assertFalse(HIGH_BEAM_ASSIST in drive { highBeam(true) }.used)
    }

    @Test fun wiperManualChanges() {
        val s = drive { wiperSwitch("LO"); wiperSwitch("HI"); wiperSwitch("LO") }
        assertEquals(3, s.wiperChanges)
        assertTrue(AUTO_WIPER.opportunity(s))
        assertEquals("와이퍼 LO→HI", s.marks.filter { it.kind == Kind.WIPER }[1].text)
        val auto = drive { wiperSwitch("AUTO"); wiperSwitch("LO"); wiperSwitch("AUTO") }
        assertEquals(0, auto.wiperChanges) // AUTO를 오가는 것은 수동 단계 변경이 아니다
        assertTrue(AUTO_WIPER in auto.used)
    }

    @Test fun rainSpansFeedDefrostAndMirror() {
        val s = drive(millis = 10 * MINUTE) { wiperSwitch("LO"); wait(6 * MINUTE); wiperSwitch("OFF") }
        assertEquals(6 * MINUTE, s.rainMillis)
        assertTrue(REAR_DEFROST.opportunity(s))
        assertTrue(MIRROR_HEAT.opportunity(s))
        assertFalse(FRONT_DEFROST.opportunity(s)) // 내기 순환은 안 켰다
        assertEquals(Mark(Kind.RAIN, 0, 6 * MINUTE, "와이퍼"), s.marks.first { it.kind == Kind.RAIN })
        assertFalse(REAR_DEFROST.opportunity(drive(millis = 4 * MINUTE) { wiperSwitch("LO") }))
        assertTrue(REAR_DEFROST.opportunity(drive { wiper(true); wait(5 * MINUTE) })) // 동작 신호만 있어도 비
    }

    @Test fun rainSpanClosesAtDriveEnd() {
        val s = drive(millis = 7 * MINUTE) { wiperSwitch("INT") }
        assertEquals(7 * MINUTE, s.rainMillis)
        assertEquals(7 * MINUTE, s.marks.first { it.kind == Kind.RAIN }.end)
    }

    @Test fun frontDefrostNeedsRainAndRecirculation() {
        val s = drive { wiperSwitch("LO"); recirc(true); wait(6 * MINUTE) }
        assertEquals(6 * MINUTE, s.rainRecircMillis)
        assertTrue(FRONT_DEFROST.opportunity(s))
        assertTrue(FRONT_DEFROST in drive { frontDefrost(true) }.used)
        assertTrue(REAR_DEFROST in drive { rearDefrost(true) }.used)
        assertTrue(MIRROR_HEAT in drive { mirrorHeat(1) }.used)
    }

    /** 그 차에 없다고 확인된 기능(스위치에 AUTO 없음 등)은 신호가 와도 권하지 않는다 */
    @Test fun unsupportedIsNeverPicked() {
        val s = drive { highBeam(true); highBeam(false); highBeam(true) }
        assertEquals(HIGH_BEAM_ASSIST, finish(s)?.pick)
        assertNull(Coach.finish(s, emptyMap(), emptySet(), s.used, unsupported = setOf(HIGH_BEAM_ASSIST))?.pick)
    }

    @Test fun highBeamToggles() {
        val s = drive { highBeam(true); highBeam(false); highBeam(true) }
        assertEquals(3, s.highBeamToggles)
        assertTrue(HIGH_BEAM_ASSIST.opportunity(s))
        assertEquals(0, drive { highBeamSwitch(true); highBeam(true); highBeam(false); highBeam(true) }.highBeamToggles)
    }

    @Test fun recirculationToggles() {
        val s = drive { recirc(true); recirc(false) }
        assertEquals(2, s.recircToggles)
        assertEquals(listOf("내기 순환 켬", "외기로 바꿈"), s.marks.filter { it.kind == Kind.RECIRC }.map { it.text }) // 6차 표시 글자
        assertTrue(AUTO_RECIRC.opportunity(s))
        assertEquals(0, drive { autoRecirc(true); recirc(true); recirc(false) }.recircToggles)
        assertTrue(AUTO_RECIRC in drive { autoRecirc(true) }.used)
    }

    @Test fun maxAc() {
        assertTrue(MAX_AC.opportunity(drive { ac(true); temp(17f) }))
        assertFalse(MAX_AC.opportunity(drive { ac(true); temp(18f) }))
        assertTrue(MAX_AC in drive { maxAc(true) }.used)
    }

    @Test fun seatHeat() {
        val s = drive { temp(27f) }
        assertTrue(SEAT_HEAT.opportunity(s))
        assertEquals("난방 27°", s.marks.first { it.kind == Kind.HEAT }.text)
        assertFalse(SEAT_HEAT.opportunity(drive { temp(27f); seatHeat(1) }))
        assertTrue(SEAT_HEAT in drive { seatHeat(1) }.used)
    }

    @Test fun passengerAndRearSeats() {
        assertTrue(PASSENGER_SEAT.opportunity(drive { passenger(true); seatHeat(2) }))
        assertTrue(PASSENGER_SEAT.opportunity(drive { passenger(true); vent(1) }))
        assertFalse(PASSENGER_SEAT.opportunity(drive { seatHeat(2) }))
        assertTrue(PASSENGER_SEAT in drive { passengerSeat(-1) }.used) // 조수석 통풍도 사용
        assertTrue(REAR_SEAT_HEAT.opportunity(drive { rearSeated(true); seatHeat(1) }))
        assertFalse(REAR_SEAT_HEAT.opportunity(drive { rearSeated(true) }))
        assertTrue(REAR_SEAT_HEAT in drive { rearSeat(2) }.used)
    }

    @Test fun ambientAfterTenMinutesWithHeadlights() {
        val s = drive(millis = 12 * MINUTE) { lightSwitch(true); light(true); wait(11 * MINUTE); light(false) }
        assertEquals(11 * MINUTE, s.nightMillis)
        assertTrue(AMBIENT.opportunity(s))
        assertFalse(AMBIENT.opportunity(drive { lightSwitch(true); light(true); wait(9 * MINUTE); light(false) }))
        assertTrue(AMBIENT in drive { ambient(true) }.used)
    }

    /** 주행거리 신호: [kmPerMinute]로 [minutes]분 달린다 */
    private fun Tracker.roll(minutes: Int, kmPerMinute: Float, from: Float = 1000f) {
        var km = from
        repeat(minutes) { wait(MINUTE); km += kmPerMinute; distance(km) }
    }

    @Test fun highwayFromDistance() {
        val s = drive(before = { allOff(); distance(1000f) }) { roll(12, 1.5f) } // 시속 90km
        assertEquals(12 * MINUTE, s.highwayMillis)
        assertTrue(CRUISE.opportunity(s))
        assertFalse(DRIVE_MODE.opportunity(s))
        assertEquals(listOf(Mark(Kind.HIGHWAY, 0, 12 * MINUTE, "시속 70km 이상")), s.marks.filter { it.kind == Kind.HIGHWAY })
        assertTrue(DRIVE_MODE.opportunity(drive(before = { allOff(); distance(1000f) }) { roll(16, 1.5f) }))
        assertEquals("시속 70km 넘게 12분 달리셨어요.", CRUISE.reason(s))
    }

    /** 경계: 정확히 시속 70km는 고속, 69km는 아니다. 60분 간격 표본이라 시간·거리 모두 정확한 값이다 */
    @Test fun highwayBoundaryIs70() {
        fun hour(km: Float) = drive(before = { allOff(); distance(1000f) }) { wait(60 * MINUTE); distance(1000f + km) }
        assertEquals(60 * MINUTE, hour(70f).highwayMillis)
        assertEquals(0L, hour(69f).highwayMillis)
    }

    @Test fun slowDistanceIsNotHighway() {
        val s = drive(millis = 25 * MINUTE, before = { allOff(); distance(1000f) }) { roll(25, 0.5f) } // 시속 30km
        assertEquals(0L, s.highwayMillis)
        assertFalse(CRUISE.opportunity(s)) // 주행거리를 아는 차는 운전 시간만으로 크루즈를 권하지 않는다
        assertTrue(s.marks.none { it.kind == Kind.DRIVE })
    }

    @Test fun driveModeUse() {
        assertTrue(DRIVE_MODE in drive { driveMode("ECO") }.used)
        assertFalse(DRIVE_MODE in drive { driveMode("NORMAL") }.used)
    }

    @Test fun newFeaturesNeedTheirSignal() {
        // 하이빔 스위치를 못 읽는 차: 수동 조작인지 모르니 세지 않고, 권하지도 않는다
        val s = drive(before = { highBeam(false) }) { highBeam(true); highBeam(false); highBeam(true) }
        assertEquals(0, s.highBeamToggles)
        assertNull(finish(s)?.pick)
    }

    @Test fun acMarksOnlyWhenCrossingThresholds() {
        // 에어컨 켜고 24 → 21(통풍 기준) → 20 → 17(MAX A/C 기준): 근거는 기준을 넘을 때만
        val s = drive { ac(true); temp(21f); temp(20f); temp(17f) }
        assertEquals(listOf("에어컨 21°", "에어컨 17°"), s.marks.filter { it.kind == Kind.AC }.map { it.text })
    }

    @Test fun priorityFollowsTable() {
        val s = drive(millis = 12 * MINUTE) {
            wiperSwitch("LO"); wiperSwitch("HI"); wiperSwitch("LO")
            highBeam(true); highBeam(false); highBeam(true)
            lightSwitch(true); light(true); wait(10 * MINUTE)
        }
        assertEquals(AUTO_WIPER, finish(s)?.pick)
        assertEquals(HIGH_BEAM_ASSIST, finish(s, mapOf(AUTO_WIPER to 1))?.pick)
        assertEquals(AMBIENT, finish(s, mapOf(AUTO_WIPER to 1, HIGH_BEAM_ASSIST to 1, REAR_DEFROST to 1, MIRROR_HEAT to 1))?.pick)
    }

    @Test fun eighteenFeatures() {
        assertEquals(18, Feature.entries.size)
        assertEquals(7, Feature.entries.map { it.zone }.toSet().size)
        assertTrue(Feature.entries.all { it.where.hi in it.where.buttons.indices })
    }

    @Test fun marksSurviveSaving() {
        val ms = listOf(Mark(Kind.FAN, 180_000, -1, "풍량 2→4"), Mark(Kind.RAIN, 0, 600_000, "와이퍼"), Mark(Kind.NIGHT, 5, 9, "전조등"))
        assertEquals(ms, Marks.decode(Marks.encode(ms)))
        assertEquals(emptyList<Mark>(), Marks.decode(null))
        assertEquals(listOf(ms[0]), Marks.decode("NOPE|1|2|x\n" + Marks.encode(ms.take(1)))) // 모르는 줄은 건너뜀
    }
}

class PickAreaTest {
    @Test fun picksNarrowestAreaContainingTarget() {
        val left = SeatArea(SeatValue.ROW_1_LEFT)
        val both = SeatArea(SeatValue.ROW_1_LEFT, SeatValue.ROW_1_RIGHT)
        assertEquals(left, pickArea(listOf(both, left), SeatValue.ROW_1_LEFT) { it.values })
        assertEquals(both, pickArea(listOf(both), SeatValue.ROW_1_RIGHT) { it.values })
        assertNull(pickArea(listOf(left), SeatValue.ROW_1_RIGHT) { it.values })
    }
}

/** 003 추천 기록 저장 형식(data-model "저장") */
class HistoryTest {
    private fun rec(at: Long, f: Feature, reason: String, marks: List<Mark> = emptyList()) = Record(at, f, reason, 2 * MINUTE, marks)

    @Test fun roundTrip() {
        val records = listOf(
            rec(1_000L, AUTO_CLIMATE, "바람과 온도를 손으로 3번 바꾸셨어요.",
                listOf(Mark(Kind.FAN, 10_200, -1, "풍량 2→4"), Mark(Kind.RAIN, 0, 30_000, "와이퍼"))),
            rec(2_000L, SEAT_VENT, "에어컨을 20°로 | 낮게 트셨어요."), // 근거 없음, 이유에 구분 기호
        )
        assertEquals(records, History.decode(History.encode(records)))
    }

    @Test fun skipsBrokenRecords() {
        val good = rec(3_000L, AUTO_WIPER, "와이퍼 단계를 손으로 4번 바꾸셨어요.")
        val s = listOf(
            "x\u001FAUTO_CLIMATE\u001F이유\u001F1\u001F", // 숫자가 아닌 시각
            "1\u001FAUTO_HOLD\u001F이유\u001F1\u001F", // 지금 앱에 없는 기능
            "1\u001F2", // 필드 수
            History.encode(listOf(good)),
        ).joinToString("\u001E")
        assertEquals(listOf(good), History.decode(s))
        assertEquals(emptyList<Record>(), History.decode(null))
        assertEquals(emptyList<Record>(), History.decode(""))
    }

    @Test fun appendKeepsOldText() {
        val a = rec(1L, AUTO_CLIMATE, "a")
        val b = rec(2L, CRUISE, "b")
        assertEquals(listOf(a), History.decode(History.append(null, a)))
        assertEquals(listOf(a), History.decode(History.append("", a)))
        val s = History.append(History.append("읽을 수 없는 옛 기록", a), b)
        assertTrue(s.startsWith("읽을 수 없는 옛 기록")) // 다시 쓰지 않고 이어 붙인다(research R9)
        assertEquals(listOf(a, b), History.decode(s))
    }
}
