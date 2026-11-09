package kr.skku.unusedfeature

import android.content.Context

/** 기능별 안내 횟수·익힘과 마지막 운전 결과. 기기 안에만 저장한다(원칙 V). 키는 data-model. */
class Progress(context: Context) {
    private val p = context.getSharedPreferences("progress", Context.MODE_PRIVATE)

    fun shown(): Map<Feature, Int> = Feature.entries.associateWith { p.getInt("shown_${it.name}", 0) }

    fun learned(): Set<Feature> = Feature.entries.filter { p.getBoolean("learned_${it.name}", false) }.toSet()

    /** 새로 익힌 기능을 시각·상황과 함께 기록. 이미 익힌 것은 건드리지 않는다. */
    fun learn(features: Set<Feature>, note: String) {
        val now = System.currentTimeMillis()
        p.edit().apply {
            features.filterNot { p.getBoolean("learned_${it.name}", false) }.forEach {
                putBoolean("learned_${it.name}", true)
                putLong("learned_at_${it.name}", now)
                putString("learned_note_${it.name}", note)
            }
        }.apply()
    }

    fun record(o: Outcome) {
        p.edit().apply {
            o.pick?.let {
                putInt("shown_${it.name}", p.getInt("shown_${it.name}", 0) + 1)
                val r = Record(System.currentTimeMillis(), it, o.reason, o.driveMillis, o.marks)
                putString("history", History.append(p.getString("history", null), r))
            }
            putString("last_pick", o.pick?.name)
            putString("last_confirmed", o.confirmed?.name)
            putString("last_reason", o.reason)
            putString("last_marks", Marks.encode(o.marks))
            putString("last_used", o.used.joinToString(",") { it.name })
            putLong("last_drive_ms", o.driveMillis)
            putInt("drives", drives() + 1)
            if (!p.contains("first_drive")) putLong("first_drive", System.currentTimeMillis())
        }.apply()
    }

    /** 운전 끝 판단을 한 횟수. 1차의 `drove` 기록이 있으면 최소 1번으로 본다 */
    fun drives(): Int = p.getInt("drives", if (p.getBoolean("drove", false)) 1 else 0)
    fun firstDrive(): Long = p.getLong("first_drive", 0)
    fun learnedAt(f: Feature): Long = p.getLong("learned_at_${f.name}", 0)
    fun learnedNote(f: Feature): String = p.getString("learned_note_${f.name}", null).orEmpty()
    fun lastPick(): Feature? = p.getString("last_pick", null)?.let(::feature)
    fun lastConfirmed(): Feature? = p.getString("last_confirmed", null)?.let(::feature)
    fun lastReason(): String = p.getString("last_reason", null).orEmpty()
    fun lastMarks(): List<Mark> = Marks.decode(p.getString("last_marks", null))
    fun lastUsed(): Set<Feature> = p.getString("last_used", null).orEmpty().split(',').mapNotNull(::feature).toSet()
    fun lastDriveMillis(): Long = p.getLong("last_drive_ms", 0)

    /** 추천 기록, 최신이 앞(003) */
    fun history(): List<Record> = History.decode(p.getString("history", null)).asReversed()

    private fun feature(name: String) = Feature.entries.firstOrNull { it.name == name }
}
