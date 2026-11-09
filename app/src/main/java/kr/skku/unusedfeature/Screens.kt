package kr.skku.unusedfeature

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 받침이 있으면 "을", 없으면 "를". 영문으로 끝나면 읽는 소리로(L·M·N이면 받침) */
fun obj(word: String): String {
    val last = word.last()
    val batchim = if (last in '가'..'힣') (last - '가') % 28 != 0 else last.uppercaseChar() in "LMN"
    return word + if (batchim) "을" else "를"
}

fun day(ms: Long): String = SimpleDateFormat("M월 d일", Locale.KOREA).format(Date(ms))
fun moment(ms: Long): String = SimpleDateFormat("M월 d일 a h:mm", Locale.KOREA).format(Date(ms))

/** 아이콘 타일: 바탕 그라데이션과 아이콘 색 */
enum class Tile(val from: Int, val to: Int, val fg: Int) {
    BLUE(R.color.blue, R.color.blue_dark, R.color.on_accent),
    GREEN(R.color.green, R.color.green_dark, R.color.on_accent),
    VIOLET(R.color.violet, R.color.blue, R.color.on_accent),
    SOFT(R.color.blue_soft, R.color.blue_soft, R.color.blue),
    GREY(R.color.chip, R.color.chip, R.color.text3),
}

/** 구역 카드의 기능 한 줄 상태 */
enum class RowState { LEARNED, SHOWN, NOT_YET, MISSING }

class ZoneRow(val feature: Feature, val state: RowState, val label: String)

/** 상황 화면을 코드로 조립하는 작은 도구들. 크기는 Pleos 글자 단계(research R7) */
class Ui(val ctx: Context) {
    private val density = ctx.resources.displayMetrics.density
    fun px(v: Number) = (v.toFloat() * density).toInt()
    fun c(id: Int) = ctx.getColor(id)

    fun text(s: CharSequence, sp: Float, color: Int = R.color.text1, weight: Int = 400): TextView = TextView(ctx).apply {
        text = s
        textSize = sp
        setTextColor(c(color))
        typeface = Typeface.create(Typeface.SANS_SERIF, weight, false)
        setLineSpacing(0f, 1.18f)
    }

    fun small(s: String) = text(s, 22f, R.color.text3, 600)
    fun headline(s: String, sp: Float = 48f) = text(s, sp, R.color.text1, 700)
    fun body(s: String, sp: Float = 26f) = text(s, sp, R.color.text2)

    fun round(fill: Int, radius: Float, line: Int = 0) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = radius * density
        if (line != 0) setStroke(px(1.5f), line)
    }

    fun dot(color: Int, size: Int = 12) = View(ctx).apply {
        background = round(c(color), 100f)
        layoutParams = LinearLayout.LayoutParams(px(size), px(size))
    }

    fun column(gap: Int, vararg views: View) = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        views.forEachIndexed { i, v -> addView(v, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { if (i > 0) topMargin = px(gap) }) }
    }

    /** 가로 줄. [grow] 번째 칸이 남은 폭을 차지한다(글자가 줄바꿈되게) */
    fun row(gap: Int, vararg views: View, grow: Int = -1) = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        views.forEachIndexed { i, v ->
            val lp = if (i == grow) LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
            else (v.layoutParams as? LinearLayout.LayoutParams) ?: LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            addView(v, lp.apply { if (i > 0) marginStart = px(gap) })
        }
    }

    /** 칩·칸 묶음: [columns]칸 격자. [even]이면 칸 폭을 똑같이 */
    fun grid(columns: Int, views: List<View>, even: Boolean = false) = GridLayout(ctx).apply {
        columnCount = columns
        views.forEach {
            addView(it, GridLayout.LayoutParams().apply {
                setMargins(0, 0, px(12), px(12))
                if (even) { width = 0; columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f) }
            })
        }
    }

    fun card(vararg views: View, gap: Int = 16) = column(gap, *views).apply {
        background = round(c(R.color.card), 24f, c(R.color.line))
        setPadding(px(32), px(28), px(32), px(28))
    }

    fun icon(res: Int, color: Int, size: Int) = ImageView(ctx).apply {
        setImageResource(res)
        setColorFilter(color)
        layoutParams = LinearLayout.LayoutParams(px(size), px(size))
    }

    fun tile(res: Int, tile: Tile, size: Int = 176, badge: Boolean = false): View = FrameLayout(ctx).apply {
        background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(c(tile.from), c(tile.to))).apply {
            cornerRadius = size * 0.24f * density
        }
        if (tile.fg == R.color.on_accent) elevation = px(size / 16).toFloat()
        addView(ImageView(ctx).apply { setImageResource(res); setColorFilter(c(tile.fg)) }, FrameLayout.LayoutParams(px(size * 0.56f), px(size * 0.56f), Gravity.CENTER))
        if (badge) addView(ImageView(ctx).apply {
            setImageResource(R.drawable.ic_check)
            setColorFilter(c(R.color.green))
            background = round(Color.WHITE, 100f)
            setPadding(px(8), px(8), px(8), px(8))
            elevation = px(4).toFloat()
        }, FrameLayout.LayoutParams(px(size * 0.3f), px(size * 0.3f), Gravity.TOP or Gravity.END).apply { setMargins(0, px(8), px(8), 0) })
        layoutParams = LinearLayout.LayoutParams(px(size), px(size))
    }

    fun chip(label: String, fg: Int, bg: Int, iconRes: Int = 0) = text(label, 20f, fg, 600).apply {
        background = round(c(bg), 100f)
        setPadding(px(18), px(9), px(20), px(9))
        if (iconRes != 0) {
            val d = ctx.getDrawable(iconRes)!!.mutate()
            d.setTint(c(fg))
            d.setBounds(0, 0, px(22), px(22))
            setCompoundDrawablesRelative(d, null, null, null)
            compoundDrawablePadding = px(8)
        }
    }

    /** 천천히 깜빡이기. 화면에서 빠지면 멈춘다 */
    fun pulse(v: View) {
        val a = ObjectAnimator.ofFloat(v, "alpha", 1f, 0.5f).apply {
            duration = 900
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
        }
        v.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) = a.start()
            override fun onViewDetachedFromWindow(view: View) = a.cancel()
        })
    }

    /** 누를 곳: 어두운 버튼 줄에서 해당 버튼만 파랗게 깜빡인다 */
    fun where(w: Where): View {
        val buttons = w.buttons.mapIndexed { i, b ->
            val hot = i == w.hi
            text(b, if (hot) 22f else 19f, if (hot) R.color.on_accent else R.color.panel_text, if (hot) 700 else 400).apply {
                if (hot) pulse(this)
                background = if (hot) round(c(R.color.blue), 12f) else round(c(R.color.panel), 12f, c(R.color.panel_line))
                setPadding(px(18), px(12), px(18), px(12))
            }
        }
        return row(10, *buttons.toTypedArray()).apply {
            background = round(c(R.color.panel), 18f)
            setPadding(px(16), px(14), px(16), px(14))
        }
    }

    /** 아이콘 타일 + 큰 제목 + 아래 글자 */
    fun hero(tile: View, title: String, sub: String?) =
        row(36, tile, column(14, headline(title), *listOfNotNull(sub?.let { body(it) }).toTypedArray()), grow = 1)

    fun banner(text: String) = row(14, dot(R.color.red), text(text, 22f, R.color.red_text, 600), grow = 1).apply {
        background = round(c(R.color.red_soft), 18f, c(R.color.red))
        setPadding(px(24), px(18), px(24), px(18))
    }

    fun cardTitle(title: String, right: String = "") = row(12, text(title, 26f, R.color.text1, 600), text(right, 22f, R.color.text3), grow = 0)

    fun recent(driveMs: Long) = small("방금 끝난 운전 · ${driveMs / MINUTE}분")
}

/** 1. 첫 실행 */
fun Ui.firstRun(): View {
    fun step(n: Int, color: Int, title: String, sub: String) = row(24,
        text("$n", 24f, R.color.on_accent, 700).apply {
            gravity = Gravity.CENTER
            background = round(c(color), 100f)
            layoutParams = LinearLayout.LayoutParams(px(52), px(52))
        },
        column(4, text(title, 28f, R.color.text1, 600), text(sub, 22f, R.color.text3)), grow = 1,
    ).apply {
        background = round(c(R.color.card), 22f, c(R.color.line))
        setPadding(px(28), px(24), px(28), px(24))
    }
    return column(28,
        small("안 써본 기능"),
        hero(tile(R.drawable.ic_spark, Tile.VIOLET), "운전을 마치면\n놓친 기능 하나를\n알려 드려요", null),
        step(1, R.color.blue, "평소처럼 운전하세요", "주행 중에는 말하지 않고 지켜보기만 해요"),
        step(2, R.color.blue, "P에 놓으면 딱 한 가지", "이번 운전에서 쓸 만했던 기능을 음성과 화면으로"),
        step(3, R.color.green, "써 보면 지도에 초록불", "익힌 기능은 다시 알려 드리지 않아요"),
    )
}

/** 2. 주행 중: 고정 문구만 (원칙 I) */
fun Ui.driving(recording: Boolean): View = column(32,
    row(0, tile(R.drawable.ic_z_wheel, Tile.SOFT, 150)),
    headline("운전 중이에요", 60f),
    body(if (recording) "차를 세우고 P에 놓으면 알려 드릴게요." else "이번 운전은 기록하지 않아요. 다음 운전부터 지켜볼게요.", 30f),
    row(14, dot(R.color.green, 14), text("주행 중에는 말하지도, 화면을 바꾸지도 않아요", 22f, R.color.text3)).apply {
        background = round(c(R.color.card), 18f, c(R.color.line))
        setPadding(px(24), px(18), px(28), px(18))
    }.let { row(0, it) },
).apply { setPadding(0, px(120), 0, 0) }

/** 3. 운전 직후 안내: 왜(근거 타임라인)와 어디(누를 곳) */
fun Ui.tip(f: Feature, reason: String, marks: List<Mark>, driveMs: Long, banner: View?): View {
    val verb = if (f == Feature.DRIVE_MODE) "바꿔" else "켜"
    val parts = mutableListOf<View>()
    banner?.let(parts::add)
    parts += recent(driveMs)
    parts += hero(tile(f.icon, Tile.BLUE), "다음엔 ${obj(f.title)}\n$verb 보세요", reason)
    if (marks.isNotEmpty()) {
        val points = marks.count { it.end < 0 }
        val minutes = marks.filter { it.end >= 0 }.sumOf { it.end - it.at } / MINUTE
        parts += card(
            cardTitle("이번 운전에서 · ${f.evidenceTitle}", if (points > 0) "${points}번" else "${minutes}분"),
            TimelineView(ctx).apply { set(marks, driveMs) },
        )
    }
    parts += whereCard(f)
    return column(28, *parts.toTypedArray())
}

/** 누를 곳 카드: 버튼 줄 + 자리·언제 + 각주 */
fun Ui.whereCard(f: Feature): View = card(
    cardTitle("여기를 누르세요"),
    row(28, where(f.where), column(6,
        text("${f.where.place} · ${f.where.buttons[f.where.hi]}", 24f, R.color.text1, 600),
        text(f.whenText, 22f, R.color.text3),
    ), grow = 1),
    text("버튼 위치는 차종마다 조금 다를 수 있어요.", 20f, R.color.text4),
)

/** 새 안내 위에 붙는 초록 띠 */
fun Ui.confirmBanner(f: Feature, done: Int, total: Int) = row(14,
    icon(R.drawable.ic_check, c(R.color.green), 28),
    text("${f.title}, 써 보셨네요 · 도감 $done/$total", 24f, R.color.green_text, 600), grow = 1,
).apply {
    background = round(c(R.color.green_soft), 18f)
    setPadding(px(24), px(18), px(24), px(18))
}

/** 4. 써 보셨네요 */
fun Ui.confirm(f: Feature, driveMs: Long, learned: List<Feature>, total: Int, at: Long, note: String): View {
    val bar = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        repeat(total) { i ->
            addView(View(ctx).apply {
                background = round(c(if (i < learned.size) R.color.green else R.color.line), 8f)
            }, LinearLayout.LayoutParams(0, px(14), 1f).apply { if (i > 0) marginStart = px(6) })
        }
    }
    val chips = learned.map {
        if (it == f) chip("${it.title} · 새로", R.color.on_accent, R.color.green, it.icon)
        else chip(it.title, R.color.green_text, R.color.green_soft, it.icon)
    }
    return column(28,
        recent(driveMs),
        hero(tile(f.icon, Tile.GREEN, badge = true), "${f.title},\n써 보셨네요!", "이제 다시 알려 드리지 않아요."),
        card(cardTitle("도감", "${learned.size} / $total 익힘"), bar, grid(3, chips)),
        card(row(20, tile(R.drawable.ic_clock, Tile.SOFT, 64), column(4,
            text("처음 쓴 순간", 26f, R.color.text1, 600),
            text(if (at > 0) "${moment(at)} · $note" else note, 22f, R.color.text3),
        ), grow = 1)),
    )
}

/** 5. 권할 기능이 없을 때 */
fun Ui.none(driveMs: Long, used: List<Feature>, remaining: List<Feature>): View {
    val left = remaining.take(8).map { chip(it.title, R.color.text3, R.color.chip, it.icon) } +
        listOfNotNull(if (remaining.size > 8) chip("외 ${remaining.size - 8}개", R.color.text3, R.color.chip) else null)
    return column(28,
        recent(driveMs),
        hero(tile(R.drawable.ic_ok, Tile.GREY), "이번 운전에선\n권할 기능이 없었어요", "다음 운전도 지켜볼게요."),
        card(cardTitle("이번 운전에서 쓴 기능"),
            if (used.isEmpty()) body("없어요", 24f) else grid(3, used.map { chip(it.title, R.color.green_text, R.color.green_soft, it.icon) })),
        card(cardTitle("아직 남은 기능 ${remaining.size}개"), grid(3, left),
            text("쓸 만한 순간이 오면 그 운전이 끝날 때 알려 드릴게요.", 22f, R.color.text3)),
    )
}

/** 6. 도감 완성 */
fun Ui.complete(features: List<Feature>, learnedAt: (Feature) -> Long, firstDrive: Long, drives: Int): View {
    val ring = RingView(ctx).apply { set(features.size, features.size); layoutParams = LinearLayout.LayoutParams(px(176), px(176)) }
    val cells = features.map { f ->
        row(14, tile(f.icon, Tile.GREEN, 56), column(2,
            text(f.title, 22f, R.color.text1, 600),
            text(learnedAt(f).takeIf { it > 0 }?.let(::day) ?: "익힘", 20f, R.color.text3),
        )).apply {
            background = round(c(R.color.card), 18f, c(R.color.line))
            setPadding(px(16), px(14), px(22), px(14))
        }
    }
    return column(28,
        small("도감 완성"),
        hero(ring, "내 차 기능 ${features.size}개를\n모두 익혔어요", "${day(firstDrive)}부터 운전 ${drives}번 만이에요."),
        grid(3, cells, even = true),
    )
}

/** 7. 구역 카드: 그 구역 기능들의 상태·언제·누를 곳 */
fun Ui.zone(z: Zone, rows: List<ZoneRow>, done: Int, total: Int, onClose: () -> Unit): View {
    val close = chip("✕  닫기", R.color.text2, R.color.chip).apply { setOnClickListener { onClose() } }
    val cards = rows.map { r ->
        val (style, fg, bg) = when (r.state) {
            RowState.LEARNED -> Triple(Tile.GREEN, R.color.green_text, R.color.green_soft)
            RowState.SHOWN -> Triple(Tile.BLUE, R.color.blue, R.color.blue_soft)
            else -> Triple(Tile.GREY, R.color.text3, R.color.chip)
        }
        card(
            row(24, tile(r.feature.icon, style, 96), column(10,
                text(r.feature.title, 30f, R.color.text1, 700),
                row(0, chip(r.label, fg, bg)),
            ), grow = 1),
            text("언제 쓰면 좋아요", 22f, R.color.text3, 600),
            body(r.feature.whenText, 24f),
            row(0, where(r.feature.where)),
        ).apply { if (r.state == RowState.MISSING) alpha = 0.5f }
    }
    return column(24,
        row(20, column(6, small("내 차 지도"), headline(z.title, 44f), body("$done / $total 익힘", 24f)), close, grow = 0),
        *cards.toTypedArray(),
        text("버튼 위치는 차종마다 조금 다를 수 있어요.", 20f, R.color.text4),
    )
}
