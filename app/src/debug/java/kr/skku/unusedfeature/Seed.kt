package kr.skku.unusedfeature

import java.util.Calendar
import java.util.TimeZone

/**
 * 4차 시연 샘플 상태(specs/004-visible-tips data-model Seed). 순수 코드이고, 디버그 SeedReceiver만 쓴다(디버그 소스셋이라 출시 빌드에 없다).
 * 이유 문장은 앱이 쓰는 Feature.reason을 그 기록의 운전 통계로 부른 값이다(시연 준비값)
 */
object Seed {
    val learned = setOf(Feature.AUTO_CLIMATE, Feature.SEAT_HEAT)

    /** daysBack: zone 기준 오늘 0시에서 며칠 전, hour: 그날 몇 시, minutes: 운전 길이 */
    private class Item(val f: Feature, val daysBack: Int, val hour: Int, val minutes: Int, val stats: DriveStats.() -> Unit, val marks: List<Mark>)

    private fun min(n: Int) = n * MINUTE
    private fun point(kind: Kind, at: Int, text: String) = Mark(kind, min(at), -1, text)

    private val items = listOf(
        Item(Feature.AUTO_CLIMATE, 3, 9, 14, { manualClimateChanges = 3 },
            listOf(point(Kind.FAN, 2, "풍량 2→4"), point(Kind.FAN, 3, "풍량 4→5"), point(Kind.TEMP, 5, "온도 24→23"))),
        Item(Feature.AUTO_WIPER, 3, 18, 22, { wiperChanges = 4 },
            listOf(point(Kind.WIPER, 4, "와이퍼 OFF→LO"), point(Kind.WIPER, 7, "와이퍼 LO→HI"),
                point(Kind.WIPER, 12, "와이퍼 HI→LO"), point(Kind.WIPER, 18, "와이퍼 LO→OFF"))),
        Item(Feature.CRUISE, 2, 9, 35, { distanceHeard = true; highwayMillis = min(11) },
            listOf(Mark(Kind.HIGHWAY, min(12), min(23), "시속 70km 이상"))),
        Item(Feature.SEAT_HEAT, 2, 18, 9, { maxSetTemp = 27f }, listOf(point(Kind.HEAT, 1, "난방 27°"))),
        Item(Feature.FRONT_DEFROST, 1, 9, 18, { rainRecircMillis = min(8) },
            listOf(Mark(Kind.RAIN, min(3), min(15), "와이퍼"), point(Kind.RECIRC, 4, "내기 순환 켬"))),
        Item(Feature.AMBIENT, 1, 18, 16, { nightMillis = min(12) }, listOf(Mark(Kind.NIGHT, min(2), min(14), "전조등"))),
    )

    /** 그 기능 기록의 운전 통계 */
    fun stats(f: Feature) = DriveStats().apply(items.first { it.f == f }.stats)

    /** 기록 6개, 오래된 것이 앞(History 저장 순서). 날짜 묶음이 늘 셋이다 */
    fun records(now: Long, zone: TimeZone): List<Record> = items.map { i ->
        val at = Calendar.getInstance(zone).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_MONTH, -i.daysBack)
            set(Calendar.HOUR_OF_DAY, i.hour)
        }.timeInMillis
        Record(at, i.f, i.f.reason(stats(i.f)), min(i.minutes), i.marks)
    }

    /**
     * 익힌 시각과 메모: 공조 AUTO는 와이퍼 기록 운전을 시작하고 3분 뒤, 시트 열선은 김서림 기록 운전을 시작하자마자.
     * 기록 시각(at)은 P에 놓은 때(운전 끝)라 시작은 at − 운전 길이다(코드 리뷰 반영)
     */
    fun learnedAt(records: List<Record>): Map<Feature, Pair<Long, String>> {
        fun start(f: Feature) = records.first { it.pick == f }.let { it.at - it.driveMs }
        return mapOf(
            Feature.AUTO_CLIMATE to (start(Feature.AUTO_WIPER) + min(3) to "운전을 시작하고 3분 뒤"),
            Feature.SEAT_HEAT to (start(Feature.FRONT_DEFROST) to "운전을 시작하자마자"),
        )
    }
}
