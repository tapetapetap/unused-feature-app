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

    /** 7차: 근거 그림에서 센 값 = 이유 문장의 통계. 어긋난 기록이 실패 메시지에 모두 보인다(specs/007-demo-pictures research R3) */
    @Test fun picturesMatchTheSentences() {
        // 앞유리 김서림: "내기 순환 켬" 점부터 다음 "외기로 바꿈" 점(없으면 운전 끝)까지가 비 막대와 겹친 길이
        fun rainWhileRecirc(r: Record) = r.marks.filter { it.text == "내기 순환 켬" }.sumOf { on ->
            val off = r.marks.filter { it.text == "외기로 바꿈" && it.at > on.at }.minOfOrNull { it.at } ?: r.driveMs
            r.marks.filter { it.kind == Kind.RAIN }.sumOf { maxOf(0L, minOf(off, it.end) - maxOf(on.at, it.at)) }
        }
        val both = Seed.records(now, TimeZone.getTimeZone("Asia/Seoul")).associate { r ->
            val s = Seed.stats(r.pick)
            val card = evidenceCount(r.marks, r.pick.evidence) // 카드 오른쪽 숫자: 순간만이면 점 수, 구간만이면 막대 길이 합
            r.pick to when (r.pick) {
                Feature.AUTO_CLIMATE -> "${s.manualClimateChanges}번" to card
                Feature.AUTO_WIPER -> "${s.wiperChanges}번" to card
                Feature.CRUISE -> "${s.highwayMillis / MINUTE}분" to card
                Feature.AMBIENT -> "${s.nightMillis / MINUTE}분" to card
                Feature.SEAT_HEAT -> "난방 ${deg(s.maxSetTemp)}°" to r.marks.first { it.kind == Kind.HEAT }.text
                Feature.FRONT_DEFROST -> "${s.rainRecircMillis / MINUTE}분" to "${rainWhileRecirc(r) / MINUTE}분"
                else -> error("${r.pick}: 그림에서 세는 법을 정한다")
            }
        }
        assertEquals(both.mapValues { it.value.first }, both.mapValues { it.value.second }) // 문장 통계, 그림에서 센 값
    }
}
