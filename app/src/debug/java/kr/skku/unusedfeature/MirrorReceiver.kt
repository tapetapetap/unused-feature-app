package kr.skku.unusedfeature

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.MirrorArea
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 디버그 빌드 전용 시연 준비. 콜드 부팅 직후 에뮬레이터는 사이드미러 열선 값을 한 번도 보내지 않고, 콘솔에도 이 항목이 없다.
 * 그러면 앱이 사이드미러 열선을 신호 없는 기능으로 빼서 비 오는 운전 세 번째 안내가 달라진다.
 * scripts/demo.sh reset이 adb shell로 이 수신기를 불러 SDK로 한 번 켰다 끈다(차에 값이 생긴다)
 */
class MirrorReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val done = AtomicBoolean(false)
        fun end(note: String) {
            if (done.compareAndSet(false, true)) { Log.i(TAG, note); pending.finish() }
        }
        val fail = { e: Exception -> Log.w(TAG, "미러 열선 쓰기 실패", e); end("실패") }
        Handler(Looper.getMainLooper()).postDelayed({ end("응답 없음") }, 8_000)
        try {
            val mirror = Vehicle(context.applicationContext).apply { initialize() }.getSideMirror()
            mirror.checkHvacSideMirrorHeatCapabilities({ areas: Map<MirrorArea, *> ->
                val a = areas.keys.firstOrNull() // 앱(CarWatcher.watchMirror)이 구독하는 것과 같은 첫 영역
                if (a == null) end("미러 영역 없음")
                else mirror.setHvacSideMirrorHeat(a, 1, { _, _ ->
                    mirror.setHvacSideMirrorHeat(a, 0, { _, v -> end("미러 열선을 켰다 껐어요(지금 $v)") }, fail)
                }, fail)
            }, fail)
        } catch (e: Exception) {
            fail(e)
        }
    }

    companion object {
        private const val TAG = "MirrorReceiver"
    }
}
