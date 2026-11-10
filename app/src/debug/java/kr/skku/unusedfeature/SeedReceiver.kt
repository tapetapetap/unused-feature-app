package kr.skku.unusedfeature

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.TimeZone

/**
 * 디버그 빌드 전용 시연 샘플 상태(specs/004-visible-tips data-model 키 표). scripts/demo.sh seed가 adb shell로 부른다.
 * progress를 비우고 기록 6개, 익힘 2개, 운전 6번을 앱 저장 형식 그대로 쓴다
 */
class SeedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val rs = Seed.records(System.currentTimeMillis(), TimeZone.getDefault())
        val learned = Seed.learnedAt(rs)
        val last = rs.last()
        context.getSharedPreferences("progress", Context.MODE_PRIVATE).edit().clear().apply {
            putString("history", History.encode(rs))
            rs.forEach { putInt("shown_${it.pick.name}", 1) }
            learned.forEach { (f, v) ->
                putBoolean("learned_${f.name}", true)
                putLong("learned_at_${f.name}", v.first)
                putString("learned_note_${f.name}", v.second)
            }
            putString("last_pick", last.pick.name)
            putString("last_reason", last.reason)
            putString("last_marks", Marks.encode(last.marks))
            putString("last_used", "")
            putLong("last_drive_ms", last.driveMs)
            putInt("drives", rs.size)
            putLong("first_drive", rs.first().at)
        }.commit()
        Log.i("SeedReceiver", "샘플 상태: 기록 ${rs.size}개, 익힘 ${learned.size}개")
    }
}
