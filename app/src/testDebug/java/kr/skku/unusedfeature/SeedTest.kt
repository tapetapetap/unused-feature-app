package kr.skku.unusedfeature

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/** 4차 시연 샘플 상태(specs/004-visible-tips data-model Seed 불변식) */
class SeedTest {
    private val now = 1_762_831_800_000L // 2025-11-11 12:30 KST, 03:30 UTC
    private val day = 24 * 60 * MINUTE

    private fun startOfDay(t: Long, zone: TimeZone) = Calendar.getInstance(zone).apply {
        timeInMillis = t
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test fun sixRecordsOverThreeDaysInEveryZone() {
        for (zone in listOf(TimeZone.getTimeZone("Asia/Seoul"), TimeZone.getTimeZone("UTC"))) {
            val rs = Seed.records(now, zone)
            assertEquals(rs, History.decode(History.encode(rs))) // 6개, 저장 형식 왕복
            assertEquals(6, rs.size)
            assertEquals(6, rs.map { it.pick }.toSet().size)
            val d0 = startOfDay(now, zone)
            assertTrue(rs.all { it.at >= d0 - 3 * day && it.at < d0 })
            assertEquals(3, rs.map { startOfDay(it.at, zone) }.toSet().size)
            assertEquals(rs.sortedBy { it.at }, rs) // 오래된 것이 앞(History 저장 순서)
        }
    }

    @Test fun reasonsAreTheAppsOwnSentences() {
        Seed.records(now, TimeZone.getTimeZone("Asia/Seoul")).forEach { r ->
            val s = Seed.stats(r.pick)
            assertEquals(r.pick.reason(s), r.reason)
            assertTrue(r.pick.name, r.pick.opportunity(s))
            assertTrue(r.pick.name, r.marks.isNotEmpty() && r.marks.all { it.kind in r.pick.evidence })
        }
    }

    @Test fun learnedFeaturesAreInTheRecords() {
        val rs = Seed.records(now, TimeZone.getTimeZone("Asia/Seoul"))
        assertEquals(setOf(Feature.AUTO_CLIMATE, Feature.SEAT_HEAT), Seed.learned)
        assertTrue(Seed.learned.all { f -> rs.any { it.pick == f } })
        assertEquals(Seed.learned, Seed.learnedAt(rs).keys)
        assertEquals(Feature.AMBIENT, rs.last().pick) // seed 뒤 첫 화면 = 앰비언트 안내
    }
}
