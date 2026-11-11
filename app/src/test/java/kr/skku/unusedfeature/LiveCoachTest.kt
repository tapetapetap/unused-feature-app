package kr.skku.unusedfeature

import kr.skku.unusedfeature.Feature.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 5차 주행 중 권함(specs/005-live-tips data-model §1·§3·§4) */
class LiveCoachTest {

    private var now = 0L

    /** 모든 사용 신호가 꺼진 채 운전을 시작한 Tracker(CoachTest.allOff와 같은 값) */
    private fun driving() = Tracker { now }.apply {
        auto(false); dual(false); ac(false); passenger(false); fan(1); temp(24f)
        seatHeat(0); vent(0); wheelHeat(0); lightSwitch(false); light(false); cruise(false)
        wiper(false); wiperSwitch("OFF"); highBeam(false); highBeamSwitch(false)
        recirc(false); autoRecirc(false); maxAc(false); frontDefrost(false); rearDefrost(false); mirrorHeat(0)
        driveMode("NORMAL"); passengerSeat(0); rearSeat(0); rearSeated(false); ambient(false)
        startDrive()
    }

    private fun Tracker.wipersByHand() { wiperSwitch("LO"); wiperSwitch("HI"); wiperSwitch("LO") }

    /** 와이퍼를 손으로 세 번 바꾼 값만 든 Stats(조건 하나씩 빼 보는 데 쓴다) */
    private fun wiperStats() = DriveStats().apply { wiperChanges = 3; heard += AUTO_WIPER }

    private fun next(s: DriveStats, at: Long = 10 * MINUTE, tips: List<LiveTip> = emptyList(), shown: Map<Feature, Int> = emptyMap(),
                     learned: Set<Feature> = emptySet(), unsupported: Set<Feature> = emptySet(), signal: Boolean = false) =
        LiveCoach.next(s, at, tips, shown, learned, unsupported, signal)

    @Test fun tipsTheMomentTheOpportunityAppears() {
        val t = driving()
        now += MINUTE // 출발 1분 뒤부터 권한다
        t.wiperSwitch("LO"); t.wiperSwitch("HI")
        assertNull(next(t.stats, t.elapsed()))
        t.wiperSwitch("LO")
        assertEquals(AUTO_WIPER, next(t.stats, t.elapsed()))
    }

    @Test fun eachConditionBlocksTheTip() {
        assertEquals(AUTO_WIPER, next(wiperStats()))
        assertNull(next(DriveStats().apply { wiperChanges = 3 })) // 신호를 받지 못한 기능
        assertNull(next(wiperStats(), unsupported = setOf(AUTO_WIPER)))
        assertNull(next(wiperStats().apply { wiperChanges = 2 })) // 기회 없음
        assertNull(next(wiperStats().apply { used += AUTO_WIPER }))
        assertNull(next(wiperStats(), learned = setOf(AUTO_WIPER)))
        assertNull(next(wiperStats(), shown = mapOf(AUTO_WIPER to 3)))
        assertNull(next(wiperStats(), tips = listOf(LiveTip(AUTO_WIPER, 0)))) // 이번 운전에 이미 권함
        assertNull(next(wiperStats(), signal = true)) // 방향지시등이나 후진
        assertNull(next(wiperStats(), at = 30_000)) // 출발 1분 안
    }

    @Test fun ambientAndRearSeatHeatAreNeverTippedWhileDriving() {
        // 밤 시간은 운전 중 tick이 채우지 않으므로 Stats를 직접 채운다(그래야 live 필터만 시험한다)
        val night = DriveStats().apply { nightMillis = 11 * MINUTE; heard += AMBIENT }
        assertTrue(AMBIENT.opportunity(night))
        assertNull(next(night))
        val rear = DriveStats().apply { rearSeated = true; seatHeatOn = true; used += SEAT_HEAT; heard += REAR_SEAT_HEAT }
        assertTrue(REAR_SEAT_HEAT.opportunity(rear))
        assertNull(next(rear))
    }

    @Test fun atMostTwoTipsFiveMinutesApart() {
        val s = wiperStats().apply { maxSetTemp = 26f; heard += SEAT_HEAT }
        val first = listOf(LiveTip(AUTO_WIPER, 2 * MINUTE))
        assertNull(next(s, at = 6 * MINUTE, tips = first)) // 4분 뒤
        assertEquals(SEAT_HEAT, next(s, at = 7 * MINUTE, tips = first)) // 5분 뒤
        assertNull(next(s, at = 20 * MINUTE, tips = first + LiveTip(SEAT_HEAT, 7 * MINUTE)))
    }

    @Test fun picksLeastShownThenTableOrder() {
        val s = wiperStats().apply { manualClimateChanges = 3; heard += AUTO_CLIMATE }
        assertEquals(AUTO_CLIMATE, next(s)) // 같으면 표 순서
        assertEquals(AUTO_WIPER, next(s, shown = mapOf(AUTO_CLIMATE to 1)))
    }

    @Test fun rainAndRainRecircAccumulateWhileDriving() {
        val t = driving()
        t.wiperSwitch("LO"); t.recirc(true)
        now += 6 * MINUTE
        t.tick()
        assertEquals(6 * MINUTE, t.stats.rainMillis)
        assertEquals(6 * MINUTE, t.stats.rainRecircMillis)
        assertTrue(REAR_DEFROST.opportunity(t.stats))
        assertTrue(FRONT_DEFROST.opportunity(t.stats))
        assertEquals(6 * MINUTE, t.endDrive().rainMillis) // 운전 끝 값은 그대로
    }

    @Test fun driveTimeAccumulatesForCruiseWithoutDistance() {
        val t = driving()
        now += 21 * MINUTE
        t.tick()
        assertEquals(21 * MINUTE, t.stats.driveMillis)
        assertTrue(CRUISE.opportunity(t.stats))
    }

}
