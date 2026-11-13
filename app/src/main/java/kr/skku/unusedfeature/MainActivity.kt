package kr.skku.unusedfeature

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** 상황 화면(오른쪽, 세로는 위)과 차 지도(왼쪽, 세로는 아래). 상태 우선순위는 data-model "화면 상태" */
class MainActivity : Activity() {

    private enum class State { DRIVING, RECORD, HISTORY, ZONE, FIRST, COMPLETE, TIP, CONFIRM, NONE }

    private lateinit var progress: Progress
    private lateinit var ui: Ui
    private var openZone: Zone? = null
    private var openHistory: Int? = null // 추천 기록(003): null 닫힘, -1 목록, n 다시 보기
    private var listY = 0 // 다시 보기로 가기 전 목록의 스크롤 위치
    private var shown: State? = null
    private var drivingKey: String? = null // 5차: 주행 중 화면은 이 값이 바뀔 때만 다시 그린다(contracts §2)
    private var drivesSeen = -1
    private var restoreY = -1 // 6차: 낮·밤 전환 뒤 첫 그리기에서 돌아갈 스크롤(contracts §5)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        progress = Progress(this)
        ui = Ui(this)
        // 6차: 낮·밤 전환처럼 다시 만들어질 때 열어 둔 칸을 되살린다. 운전이 시작됐으면 render가 닫는다
        savedInstanceState?.let { b ->
            openZone = b.getString("zone")?.let { runCatching { Zone.valueOf(it) }.getOrNull() }
            openHistory = if (b.containsKey("history")) b.getInt("history") else null
            listY = b.getInt("listY"); restoreY = b.getInt("scrollY", -1); drivesSeen = b.getInt("drivesSeen", -1)
        }
        findViewById<View>(R.id.mapPane).background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(getColor(R.color.map_top), getColor(R.color.map_bottom)),
        )
        // 6차 사용자 결정: 세로(1:1) 칸의 차 지도는 그림으로만(이름표 없음, 누르기 없음). 구역은 옆 목록 줄(76dp)로 연다
        val portrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        findViewById<CarMapView>(R.id.map).apply { labels = !portrait; onZone = if (portrait) null else ::toggleZone }
        findViewById<TextView>(R.id.historyButton).apply {
            setTextColor(getColor(R.color.blue_text))
            background = ui.round(getColor(R.color.blue_soft), 100f)
            setPadding(ui.px(24), 0, ui.px(24), 0)
            setOnClickListener {
                if (WatchService.driving) return@setOnClickListener // 구역 점과 같은 규칙(FR-007). 비활성 처리와 함께 둔다
                openHistory = -1; openZone = null; render() // 늘 목록으로, 닫기는 "✕ 닫기"(FR-006)
            }
        }
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
        // 가려지는 동안 운전이 있었을 수 있다(1분 미만이면 운전 횟수도 그대로). 돌아오면 상황 화면부터(US3-3).
        // 6차: 낮·밤 전환처럼 다시 만들어지는 중이면 닫지 않고 onSaveInstanceState로 넘긴다
        if (!isChangingConfigurations) { openZone = null; openHistory = null }
        super.onPause()
    }

    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        openZone?.let { out.putString("zone", it.name) }
        openHistory?.let { out.putInt("history", it) }
        out.putInt("listY", listY); out.putInt("scrollY", findViewById<ScrollView>(R.id.scroll).scrollY); out.putInt("drivesSeen", drivesSeen)
    }

    /** 6차 뒤로 버튼: 다시 보기 → 기록 목록 → 닫기 → 앱 나가기, 구역 카드 → 닫기 */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val h = openHistory
        when {
            h != null && h >= 0 -> { openHistory = -1; render() }
            h != null || openZone != null -> { openHistory = null; openZone = null; render() }
            else -> super.onBackPressed()
        }
    }

    private fun legend(color: Int, label: String) = ui.row(10, ui.dot(color, 14), ui.text(label, 24f, R.color.text3)).apply {
        setPadding(ui.px(14), 0, ui.px(14), 0)
    }

    /** 정차 중에만 구역을 열고 닫는다 */
    private fun toggleZone(z: Zone) {
        if (WatchService.driving) return
        openZone = if (openZone == z) null else z
        openHistory = null // 기록 페이지와 구역 카드는 동시에 열리지 않는다(FR-008)
        render()
    }

    private fun render() {
        // 운전이 시작됐거나 새 운전 결과가 있으면 열어 둔 구역 카드·기록 페이지를 닫는다(안내가 가려지지 않게, FR-007)
        if (WatchService.driving || progress.drives() != drivesSeen) { openZone = null; openHistory = null }
        drivesSeen = progress.drives()
        val records = progress.history()
        val record = openHistory?.let(records::getOrNull) // 범위 밖이면 목록
        val learned = progress.learned()
        val supported = Feature.entries.filterNot { it in WatchService.unsupported }
        val total = supported.size
        val done = supported.count { it in learned }
        val pick = progress.lastPick()
        val spoken = WatchService.lastSpoken?.takeIf { it.feature == pick && it.said } // 이번 안내를 실제로 말했다(자막 막대)
        val confirmed = progress.lastConfirmed() ?: pick?.takeIf { it in learned }
        val state = when {
            WatchService.driving -> State.DRIVING
            record != null -> State.RECORD
            openHistory != null -> State.HISTORY
            openZone != null -> State.ZONE
            progress.drives() == 0 -> State.FIRST
            total > 0 && done == total -> State.COMPLETE
            pick != null && pick !in learned && pick in supported -> State.TIP
            confirmed != null -> State.CONFIRM
            else -> State.NONE
        }
        // 5차: 주행 중에는 권함 상태(기록 여부, 마지막 권함, 써 봤는지)가 바뀔 때만 다시 그린다(4차 FR-010의 뜻을 잇는다)
        val live = WatchService.tips.lastOrNull()?.feature?.let { it to (it in learned) }
        val key = "${WatchService.recording} $live ${WatchService.problems} ${WatchService.unsupported.size}"
        if (state == State.DRIVING && shown == State.DRIVING && key == drivingKey) return
        drivingKey = key
        // 5차: 내비 옆 작은 칸(세로 배치)에서는 주행 중 지도 칸을 숨겨 카드가 창 높이를 다 쓴다(research R8)
        val portrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        findViewById<View>(R.id.mapPane).visibility = if (portrait && state == State.DRIVING) View.GONE else View.VISIBLE
        val prev = shown
        shown = state

        val screen = findViewById<LinearLayout>(R.id.screen)
        screen.removeAllViews()
        WatchService.problems.forEach { screen.addView(ui.banner(it), lp(bottom = 28)) }
        val driveMs = progress.lastDriveMillis()
        screen.addView(when (state) {
            State.DRIVING -> ui.driving(WatchService.recording, live, portrait)
            State.RECORD -> recordScreen(record!!, learned)
            State.HISTORY -> ui.history(records, learned, WatchService.unsupported.toSet(),
                onOpen = { listY = findViewById<ScrollView>(R.id.scroll).scrollY; openHistory = it; render() },
                onClose = { openHistory = null; render() })
            State.ZONE -> zoneScreen(openZone!!, learned, supported)
            State.FIRST -> ui.firstRun()
            State.COMPLETE -> ui.complete(supported, progress::learnedAt, progress.firstDrive(), progress.drives())
            State.TIP -> ui.tip(pick!!, progress.lastReason(), progress.lastMarks(), driveMs,
                progress.lastConfirmed()?.let { ui.confirmBanner(it, done, total) },
                spoken?.let { ui.caption(pick, "방금 들은 음성", it.at + 6_000) } ?: ui.caption(pick, "안내 음성", 0L))
            State.CONFIRM -> ui.confirm(confirmed!!, driveMs, supported.filter { it in learned }, total,
                progress.learnedAt(confirmed), progress.learnedNote(confirmed))
            State.NONE -> ui.none(driveMs, progress.lastUsed().filter { it in supported }, supported.filterNot { it in learned })
        }, lp())
        // 다른 화면으로 바뀔 때만 맨 위로. 다시 보기에서 목록으로 돌아오면 보던 자리로, 같은 화면을 다시 그리면 그대로
        val scroll = findViewById<ScrollView>(R.id.scroll)
        val y = restoreY.also { restoreY = -1 } // 6차: 다시 만들어진 뒤 첫 그리기는 그 스크롤로(주행 중 화면은 맨 위)
        if (y >= 0 && state != State.DRIVING) scroll.post { scroll.scrollTo(0, y) }
        else if (state == State.HISTORY && prev == State.RECORD) scroll.post { scroll.scrollTo(0, listY) }
        else if (state != prev) scroll.scrollTo(0, 0)

        val spots = Zone.entries.associateWith { z ->
            val fs = supported.filter { it.zone == z }
            CarMapView.Spot(fs.count { it in learned }, fs.size)
        }
        val pulse = pick?.zone?.takeIf { state == State.TIP }
        // 4차 새 안내 강조: 가장 최근 추천 기록이 생긴 지 10초 안(앱을 늦게 열거나 seed 뒤에는 없다, research R3)
        // 4차 새 안내 강조: 이 안내를 말한 때(uptime)부터 10초. 시계가 바뀌어도 그대로고, 앱을 새로 띄운 뒤(늦게 열기·seed)에는 없다(코드 리뷰 반영)
        val spotlight = WatchService.lastSpoken?.takeIf { state == State.TIP && it.feature == pick }?.let { it.at + 10_000 } ?: 0L
        findViewById<CarMapView>(R.id.map).set(spots, pulse, confirmed?.zone?.takeIf { state == State.CONFIRM },
            record?.pick?.zone ?: openZone, state == State.DRIVING, spotlight) // 다시 보기는 그 구역에 선택 링
        findViewById<RingView>(R.id.ring).set(done, total)
        findViewById<TextView>(R.id.historyButton).apply {
            text = if (records.isEmpty()) "추천 기록  ›" else "추천 기록 ${records.size}  ›"
            isEnabled = !WatchService.driving // 비활성 뷰는 누르기를 받지 않고 덤프에 enabled="false"(FR-007)
            alpha = if (WatchService.driving) 0.4f else 1f
        }
        findViewById<TextView>(R.id.mapSub).text = when {
            state == State.FIRST -> "${total}개가 기다리고 있어요"
            done == total -> "모두 익힘"
            else -> "${done}개 익힘 · ${total - done}개 남음"
        }
        findViewById<LinearLayout?>(R.id.zoneList)?.let { list -> // 세로 화면만
            list.removeAllViews()
            Zone.entries.forEach { z -> list.addView(zoneRow(z, spots.getValue(z), z == pulse, state == State.DRIVING), lp(bottom = 24)) } // 6차: 누르는 줄 사이 24dp
        }
    }

    /** 추천 기록 다시 보기(003): 그때 안내 화면 그대로, 제목만 그때 시각 */
    private fun recordScreen(r: Record, learned: Set<Feature>): View {
        val back = ui.action("←  추천 기록", R.color.text2, R.color.chip) { openHistory = -1; render() }
        val used = progress.learnedAt(r.pick).takeIf { r.pick in learned }
            ?.let { ui.chip(if (it > 0) "써 봄 ✓ ${day(it)}" else "써 봄 ✓", R.color.green_text, R.color.green_soft) }
        return ui.tip(r.pick, r.reason, r.marks, r.driveMs, ui.row(12, *listOfNotNull(back, used).toTypedArray()),
            ui.caption(r.pick, "그때 들은 음성", 0L), ui.small("${dayLabel(r.at)} ${timeOfDay(r.at)} 운전 · ${r.driveMs / MINUTE}분"))
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
        val count = if (s.total == 0) "기능 없음" else "${s.learned}/${s.total}" // 6차: "0/0"은 고장처럼 보인다
        return ui.row(16, ui.icon(z.icon, tint, 34), ui.text(z.title, 24f, R.color.text1, 600), ui.text(count, 24f, R.color.text3), grow = 1).apply {
            background = ui.round(getColor(R.color.card), 18f, getColor(if (hot) R.color.blue else R.color.line))
            setPadding(ui.px(20), ui.px(16), ui.px(22), ui.px(16))
            minimumHeight = ui.px(76)
            if (driving) alpha = 0.4f else setOnClickListener { toggleZone(z) }
        }
    }

    private fun lp(bottom: Int = 0) = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        .apply { bottomMargin = ui.px(bottom) }
}
