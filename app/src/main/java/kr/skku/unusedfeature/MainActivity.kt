package kr.skku.unusedfeature

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** 상황 화면(오른쪽, 세로는 위)과 차 지도(왼쪽, 세로는 아래). 상태 우선순위는 data-model "화면 상태" */
class MainActivity : Activity() {

    private enum class State { DRIVING, ZONE, FIRST, COMPLETE, TIP, CONFIRM, NONE }

    private lateinit var progress: Progress
    private lateinit var ui: Ui
    private var openZone: Zone? = null
    private var shown: State? = null
    private var drivesSeen = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        progress = Progress(this)
        ui = Ui(this)
        findViewById<View>(R.id.mapPane).background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(getColor(R.color.map_top), getColor(R.color.map_bottom)),
        )
        findViewById<CarMapView>(R.id.map).onZone = ::toggleZone
        findViewById<LinearLayout>(R.id.legend).apply {
            addView(legend(R.color.blue, "이번 안내"))
            addView(legend(R.color.green, "익힘"))
            addView(legend(R.color.dot_line, "아직"))
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        WatchService.start(this)
    }

    override fun onResume() {
        super.onResume()
        WatchService.onUpdate = ::render
        shown = null
        render()
    }

    override fun onPause() {
        WatchService.onUpdate = null
        super.onPause()
    }

    private fun legend(color: Int, label: String) = ui.row(10, ui.dot(color, 14), ui.text(label, 20f, R.color.text3)).apply {
        setPadding(ui.px(14), 0, ui.px(14), 0)
    }

    /** 정차 중에만 구역을 열고 닫는다 */
    private fun toggleZone(z: Zone) {
        if (WatchService.driving) return
        openZone = if (openZone == z) null else z
        render()
    }

    private fun render() {
        // 운전이 시작됐거나 새 운전 결과가 있으면 열어 둔 구역 카드를 닫는다(안내가 가려지지 않게)
        if (WatchService.driving || progress.drives() != drivesSeen) openZone = null
        drivesSeen = progress.drives()
        val learned = progress.learned()
        val supported = Feature.entries.filterNot { it in WatchService.unsupported }
        val total = supported.size
        val done = supported.count { it in learned }
        val pick = progress.lastPick()
        val confirmed = progress.lastConfirmed() ?: pick?.takeIf { it in learned }
        val state = when {
            WatchService.driving -> State.DRIVING
            openZone != null -> State.ZONE
            progress.drives() == 0 -> State.FIRST
            total > 0 && done == total -> State.COMPLETE
            pick != null && pick !in learned && pick in supported -> State.TIP
            confirmed != null -> State.CONFIRM
            else -> State.NONE
        }
        if (state == State.DRIVING && shown == State.DRIVING) return // 주행 중에는 화면을 바꾸지 않는다(FR-010)
        shown = state

        val screen = findViewById<LinearLayout>(R.id.screen)
        screen.removeAllViews()
        WatchService.problems.forEach { screen.addView(ui.banner(it), lp(bottom = 28)) }
        val driveMs = progress.lastDriveMillis()
        screen.addView(when (state) {
            State.DRIVING -> ui.driving(WatchService.recording)
            State.ZONE -> zoneScreen(openZone!!, learned, supported)
            State.FIRST -> ui.firstRun()
            State.COMPLETE -> ui.complete(supported, progress::learnedAt, progress.firstDrive(), progress.drives())
            State.TIP -> ui.tip(pick!!, progress.lastReason(), progress.lastMarks(), driveMs,
                progress.lastConfirmed()?.let { ui.confirmBanner(it, done, total) })
            State.CONFIRM -> ui.confirm(confirmed!!, driveMs, supported.filter { it in learned }, total,
                progress.learnedAt(confirmed), progress.learnedNote(confirmed))
            State.NONE -> ui.none(driveMs, progress.lastUsed().filter { it in supported }, supported.filterNot { it in learned })
        }, lp())
        findViewById<ScrollView>(R.id.scroll).scrollTo(0, 0)

        val spots = Zone.entries.associateWith { z ->
            val fs = supported.filter { it.zone == z }
            CarMapView.Spot(fs.count { it in learned }, fs.size)
        }
        val pulse = pick?.zone?.takeIf { state == State.TIP }
        findViewById<CarMapView>(R.id.map).set(spots, pulse, confirmed?.zone?.takeIf { state == State.CONFIRM }, openZone, state == State.DRIVING)
        findViewById<RingView>(R.id.ring).set(done, total)
        findViewById<TextView>(R.id.mapSub).text = when {
            state == State.FIRST -> "${total}개가 기다리고 있어요"
            done == total -> "모두 익힘"
            else -> "${done}개 익힘 · ${total - done}개 남음"
        }
        findViewById<LinearLayout?>(R.id.zoneList)?.let { list -> // 세로 화면만
            list.removeAllViews()
            Zone.entries.forEach { z -> list.addView(zoneRow(z, spots.getValue(z), z == pulse, state == State.DRIVING), lp(bottom = 12)) }
        }
    }

    private fun zoneScreen(z: Zone, learned: Set<Feature>, supported: List<Feature>): View {
        val shownCounts = progress.shown()
        val rows = Feature.entries.filter { it.zone == z }.map { f ->
            when {
                f !in supported -> ZoneRow(f, RowState.MISSING, "이 차에 없음")
                f in learned -> ZoneRow(f, RowState.LEARNED, progress.learnedAt(f).takeIf { it > 0 }?.let { "익힘 · ${day(it)}" } ?: "익힘")
                shownCounts.getValue(f) > 0 -> ZoneRow(f, RowState.SHOWN, "안내함 ${shownCounts.getValue(f)}회")
                else -> ZoneRow(f, RowState.NOT_YET, "아직")
            }
        }
        val inZone = supported.filter { it.zone == z }
        return ui.zone(z, rows, inZone.count { it in learned }, inZone.size) { openZone = null; render() }
    }

    /** 세로 화면의 구역 목록 한 줄 */
    private fun zoneRow(z: Zone, s: CarMapView.Spot, hot: Boolean, driving: Boolean): View {
        val done = s.total > 0 && s.learned == s.total
        val tint = getColor(if (hot) R.color.blue else if (done) R.color.green else R.color.text3)
        return ui.row(16, ui.icon(z.icon, tint, 34), ui.text(z.title, 24f, R.color.text1, 600), ui.text("${s.learned}/${s.total}", 22f, R.color.text3), grow = 1).apply {
            background = ui.round(getColor(R.color.card), 18f, getColor(if (hot) R.color.blue else R.color.line))
            setPadding(ui.px(20), ui.px(16), ui.px(22), ui.px(16))
            if (driving) alpha = 0.4f else setOnClickListener { toggleZone(z) }
        }
    }

    private fun lp(bottom: Int = 0) = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        .apply { bottomMargin = ui.px(bottom) }
}
