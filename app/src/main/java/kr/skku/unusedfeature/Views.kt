package kr.skku.unusedfeature

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.SystemClock
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin

private fun View.dp(v: Float) = v * resources.displayMetrics.density
private fun View.sp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)
private fun View.color(id: Int) = context.getColor(id)
/** 1분 미만은 초로: "40초", 그 밖은 "3분" */
fun elapsed(ms: Long) = if (ms < MINUTE) "${ms / 1000}초" else "${ms / MINUTE}분"

private fun textPaint(weight: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(Typeface.SANS_SERIF, weight, false) }

/** 진행 원. 가운데에 "익힌 수/전체" */
class RingView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var done = 0
    private var total = 0
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val label = textPaint(700).apply { textAlign = Paint.Align.CENTER }

    fun set(done: Int, total: Int) { this.done = done; this.total = total; invalidate() }

    override fun onDraw(c: Canvas) {
        val s = minOf(width, height).toFloat()
        line.strokeWidth = s * 0.1f
        val r = RectF(0f, 0f, s, s).apply { inset(line.strokeWidth / 2, line.strokeWidth / 2); offset((width - s) / 2, (height - s) / 2) }
        line.color = color(R.color.line)
        c.drawArc(r, 0f, 360f, false, line)
        if (total > 0 && done > 0) {
            line.color = color(R.color.green)
            c.drawArc(r, -90f, 360f * done / total, false, line)
        }
        label.color = color(R.color.text1)
        label.textSize = s * if (total >= 10) 0.24f else 0.28f
        c.drawText("$done/$total", width / 2f, height / 2f - (label.descent() + label.ascent()) / 2, label)
    }
}

/** 근거 타임라인: 출발~도착 위에 순간(점)과 구간(막대). contracts/ui.md 2절 */
class TimelineView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private var marks: List<Mark> = emptyList()
    private var total = MINUTE
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeCap = Paint.Cap.ROUND }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG)
    private val label = textPaint(400)
    private val strong = textPaint(600)

    fun set(marks: List<Mark>, driveMillis: Long) {
        this.marks = marks
        total = maxOf(driveMillis, 1)
        invalidate()
    }

    override fun onMeasure(w: Int, h: Int) = setMeasuredDimension(MeasureSpec.getSize(w), dp(196f).toInt())

    override fun onDraw(c: Canvas) {
        val left = dp(28f)
        val right = width - dp(28f)
        val y = dp(64f)
        fun x(t: Long) = left + (right - left) * (t.coerceIn(0, total).toFloat() / total)
        label.textSize = sp(22f) // 6차: 그림 안 글자 22sp
        strong.textSize = sp(22f)

        // 바탕 선: 흐린 파랑 → 파랑
        line.shader = LinearGradient(left, 0f, right, 0f, color(R.color.blue_soft), color(R.color.blue), Shader.TileMode.CLAMP)
        line.strokeWidth = dp(6f)
        c.drawLine(left, y, right, y, line)
        line.shader = null

        // 구간: 굵은 막대와 위쪽 글자
        line.color = color(R.color.blue)
        line.strokeWidth = dp(16f)
        strong.color = color(R.color.blue_text)
        strong.textAlign = Paint.Align.CENTER
        for (m in marks.filter { it.end >= 0 }) {
            c.drawLine(x(m.at), y, maxOf(x(m.end), x(m.at) + dp(4f)), y, line)
            val t = "${m.text} ${(m.end - m.at) / MINUTE}분"
            val half = strong.measureText(t) / 2 // 6차: 22sp라 막대가 끝에 붙으면 글자가 잘린다. 뷰 안으로
            c.drawText(t, ((x(m.at) + x(m.end)) / 2).coerceIn(half, maxOf(half, width - half)), y - dp(24f), strong)
        }

        // 순간: 6개까지는 원·아이콘·글자, 넘으면 눈금과 횟수만
        val points = marks.filter { it.end < 0 }
        label.color = color(R.color.text2)
        if (points.size > 6) {
            line.strokeWidth = dp(3f)
            points.forEach { c.drawLine(x(it.at), y - dp(14f), x(it.at), y + dp(14f), line) }
            strong.textAlign = Paint.Align.RIGHT
            c.drawText("${points.size}번", right, y - dp(28f), strong)
        } else {
            val rowEnds = FloatArray(2) { -Float.MAX_VALUE } // 글자 줄 2개
            val r = dp(20f)
            for (m in points) {
                val px = x(m.at)
                dot.style = Paint.Style.FILL; dot.color = color(R.color.card)
                c.drawCircle(px, y, r, dot)
                dot.style = Paint.Style.STROKE; dot.strokeWidth = dp(3f); dot.color = color(R.color.blue)
                c.drawCircle(px, y, r, dot)
                context.getDrawable(m.kind.icon)?.mutate()?.let { d ->
                    d.colorFilter = PorterDuffColorFilter(color(R.color.blue), PorterDuff.Mode.SRC_IN)
                    val s = dp(13f).toInt()
                    d.setBounds((px - s).toInt(), (y - s).toInt(), (px + s).toInt(), (y + s).toInt())
                    d.draw(c)
                }
                val text = "${elapsed(m.at)} · ${m.text}"
                val w = label.measureText(text)
                val start = maxOf(0f, minOf(px - w / 2, width - w)) // 글자가 뷰보다 넓어도 예외 없이 왼쪽에 붙인다
                val row = rowEnds.indexOfFirst { it < start - dp(12f) }
                if (row < 0) continue // 자리가 없으면 글자만 생략(점은 남김)
                rowEnds[row] = start + w
                label.textAlign = Paint.Align.LEFT
                c.drawText(text, start, y + r + dp(34f) + row * dp(32f), label)
            }
        }

        // 출발·도착
        label.color = color(R.color.text3)
        label.textAlign = Paint.Align.LEFT
        c.drawText("출발", left - dp(8f), height - dp(8f), label)
        label.textAlign = Paint.Align.RIGHT
        c.drawText("도착 ${total / MINUTE}분", right + dp(8f), height - dp(8f), label)
    }
}

/**
 * 위에서 본 차와 구역 점 7개. 구역 점: 이번 안내 = 파랑 깜빡임, 모두 익힘 = 초록, 그 밖 = 흰 점 + 진행 호.
 * 좌표는 [Zone]의 car_top 좌표(300×560).
 */
class CarMapView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    class Spot(val learned: Int, val total: Int)

    private var spots: Map<Zone, Spot> = emptyMap()
    private var pulse: Zone? = null
    private var sparkle: Zone? = null
    private var sparkleUntil = 0L
    private var selected: Zone? = null
    private var dim = false
    private var spotlightUntil = 0L // 4차: 새 안내 강조가 끝나는 uptime(research R3)
    var onZone: ((Zone) -> Unit)? = null
    /** 6차: 세로(1:1) 칸에서는 false. 지도가 작아 이름표를 그리지 않는다(구역 목록이 같은 정보를 보인다) */
    var labels = true

    private val car = context.getDrawable(R.drawable.car_top)!!
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val label = textPaint(600)
    private val strong = textPaint(800) // 강조 중 추천 구역 이름표
    private val anim = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1800
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener { invalidate() }
    }
    private var scale = 1f
    private var ox = 0f
    private var oy = 0f

    fun set(spots: Map<Zone, Spot>, pulse: Zone?, sparkle: Zone?, selected: Zone?, dim: Boolean, spotlightUntil: Long = 0L) {
        this.spots = spots
        this.spotlightUntil = spotlightUntil
        val left = spotlightUntil - SystemClock.uptimeMillis()
        // 애니메이터가 꺼져 있으면 다시 그리는 것이 없으니 10초에 한 번 더 그려 강조를 지운다(켜져 있으면 맥박이 매 프레임 그린다)
        if (pulse != null && !dim && left > 0 && !ValueAnimator.areAnimatorsEnabled()) postInvalidateDelayed(left + 50)
        this.pulse = pulse
        if (sparkle != this.sparkle) sparkleUntil = SystemClock.uptimeMillis() + 3 * 1800 // 반짝임은 3번
        this.sparkle = sparkle
        this.selected = selected
        this.dim = dim
        if ((pulse != null || sparkle != null) && !dim) { if (!anim.isStarted) anim.start() } else anim.cancel()
        invalidate()
    }

    override fun onDetachedFromWindow() {
        anim.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(c: Canvas) {
        // 차 그림 300×560에 좌우 이름표 자리를 더해 맞춘다
        scale = minOf(width / 400f, height / 580f)
        ox = (width - 300 * scale) / 2
        oy = (height - 560 * scale) / 2
        car.setBounds(ox.toInt(), oy.toInt(), (ox + 300 * scale).toInt(), (oy + 560 * scale).toInt())
        car.alpha = if (dim) 80 else 255
        car.draw(c)

        val t = anim.animatedValue as Float
        val r = 21 * scale
        label.textSize = sp(22f) // 6차: 그림 안 글자 22sp
        strong.textSize = sp(22f)
        val sparkling = sparkle != null && SystemClock.uptimeMillis() < sparkleUntil
        val spot = pulse != null && !dim && SystemClock.uptimeMillis() < spotlightUntil
        val moving = ValueAnimator.areAnimatorsEnabled()
        if (pulse == null && !sparkling) anim.cancel() // 반짝임 3번이 끝나면 다시 그리지 않는다
        for (z in Zone.entries) {
            val s = spots[z] ?: Spot(0, 0)
            val px = ox + z.x * scale
            val py = oy + z.y * scale
            val done = s.total > 0 && s.learned == s.total
            val hot = z == pulse && !dim
            val base = when {
                hot -> color(R.color.blue)
                done || (z == sparkle && sparkling) -> color(R.color.green)
                else -> color(R.color.dot_idle)
            }
            val alpha = if (dim) 90 else 255

            // 깜빡임(파랑)·반짝임(초록): 퍼져 나가는 원
            if (hot || (z == sparkle && sparkling && !dim)) {
                fill.color = base
                fill.alpha = ((1 - t) * 140).toInt()
                c.drawCircle(px, py, r * (1 + 1.2f * t), fill)
            }
            // 4차 새 안내 강조: r에서 3.4r까지 1.2초마다 퍼지는 테두리 링, 움직임 줄이기면 고정 테두리
            if (hot && spot) {
                stroke.color = color(R.color.blue)
                if (moving) {
                    val k = (SystemClock.uptimeMillis() % 1200) / 1200f
                    stroke.alpha = ((1 - k) * 200).toInt(); stroke.strokeWidth = dp(3f)
                    c.drawCircle(px, py, r * (1 + 2.4f * k), stroke)
                } else {
                    stroke.alpha = 255; stroke.strokeWidth = dp(4f)
                    c.drawCircle(px, py, r + dp(6f), stroke)
                }
            }
            fill.color = base
            fill.alpha = alpha
            c.drawCircle(px, py, r, fill)
            if (!hot && !done) {
                stroke.color = color(R.color.dot_line); stroke.alpha = alpha; stroke.strokeWidth = dp(1.5f)
                c.drawCircle(px, py, r, stroke)
            }
            // 진행 호: 일부만 익힌 구역
            if (!done && s.learned > 0) {
                stroke.color = color(R.color.green); stroke.alpha = alpha; stroke.strokeWidth = dp(4f)
                val g = r + dp(5f)
                c.drawArc(px - g, py - g, px + g, py + g, -90f, 360f * s.learned / s.total, false, stroke)
            }
            context.getDrawable(z.icon)?.mutate()?.let { d ->
                val tint = if (hot || done || (z == sparkle && sparkling)) color(R.color.card) else color(R.color.text3)
                d.colorFilter = PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_IN)
                d.alpha = alpha
                val half = r * 0.62f
                d.setBounds((px - half).toInt(), (py - half).toInt(), (px + half).toInt(), (py + half).toInt())
                d.draw(c)
            }
            if (z == selected) {
                stroke.color = color(R.color.text1); stroke.alpha = 255; stroke.strokeWidth = dp(3f)
                c.drawCircle(px, py, r + dp(10f), stroke)
            }
            if (labels) drawLabel(c, z, s, px, py, r, alpha, if (hot && spot) strong else label)
        }
    }

    /** 구역 이름표: "공조 패널 1/5" */
    private fun drawLabel(c: Canvas, z: Zone, s: Spot, px: Float, py: Float, r: Float, alpha: Int, label: Paint) {
        val text = if (s.total == 0) "${z.title} · 기능 없음" else "${z.title} ${s.learned}/${s.total}" // 6차: "0/0" 대신
        val w = label.measureText(text) + dp(20f)
        val h = dp(40f)
        val gap = dp(10f)
        val box = when (z.side) {
            Side.RIGHT -> RectF(px + r + gap, py - h / 2, px + r + gap + w, py + h / 2)
            Side.LEFT -> RectF(px - r - gap - w, py - h / 2, px - r - gap, py + h / 2)
            Side.ABOVE -> RectF(px - w / 2, py - r - gap - h, px + w / 2, py - r - gap)
        }
        box.offset(minOf(0f, width - box.right), 0f) // 화면 밖으로 나가지 않게(6차: 오른쪽도). 오른쪽 먼저, 그다음 왼쪽(글자 시작이 보이게)
        box.offset(maxOf(0f, -box.left), 0f)
        fill.color = color(R.color.card); fill.alpha = alpha
        c.drawRoundRect(box, h / 2, h / 2, fill)
        stroke.color = color(if (z == pulse && !dim) R.color.blue else R.color.line); stroke.alpha = alpha; stroke.strokeWidth = dp(1.5f)
        c.drawRoundRect(box, h / 2, h / 2, stroke)
        label.color = color(if (z == pulse && !dim) R.color.blue_text else R.color.text2); label.alpha = alpha
        label.textAlign = Paint.Align.CENTER
        c.drawText(text, box.centerX(), box.centerY() - (label.descent() + label.ascent()) / 2, label)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (dim || onZone == null) return false
        if (e.action == MotionEvent.ACTION_UP) {
            val hit = Zone.entries.minByOrNull { hypot(ox + it.x * scale - e.x, oy + it.y * scale - e.y) }
            if (hit != null && hypot(ox + hit.x * scale - e.x, oy + hit.y * scale - e.y) < 21 * scale * 2.2f) {
                performClick()
                onZone?.invoke(hit)
            }
        }
        return true
    }

}

/** 4차 소리 막대 세 개(contracts/ui.md 표). until(uptimeMillis) 전에만 움직이고, 그 뒤나 움직임 줄이기에서는 멈춘 모습이다 */
class SoundBars(context: Context, private val until: Long) : View(context) {
    private val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = context.getColor(R.color.blue) }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) = setMeasuredDimension(dp(3 * 5f + 2 * 4f).toInt(), dp(24f).toInt())

    override fun onDraw(c: Canvas) {
        val now = SystemClock.uptimeMillis()
        val moving = now < until && ValueAnimator.areAnimatorsEnabled()
        val w = dp(5f)
        for (i in 0..2) {
            val f = if (moving) 0.3f + 0.7f * (0.5f + 0.5f * sin(2 * PI * (now / 900.0 + i / 3.0)).toFloat()) else STILL[i]
            val h = height * f
            val x = i * (w + dp(4f))
            c.drawRoundRect(x, (height - h) / 2, x + w, (height + h) / 2, w / 2, w / 2, bar)
        }
        if (moving) postInvalidateOnAnimation() // 마지막 그림은 until 뒤라 멈춘 모습이다
    }

    private companion object { val STILL = floatArrayOf(0.4f, 0.7f, 0.5f) }
}

/** 4차 추천 기록 카드의 작은 차 그림(research R2): 그 기능 구역에 점과 링. 이름표·효과는 없다 */
class ZoneThumb(context: Context, private val zone: Zone, private val state: RowState) : View(context) {
    private val car = context.getDrawable(R.drawable.car_top)!!.mutate() // 큰 지도의 흐림(알파)과 나누지 않는다
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }

    init { contentDescription = "${zone.title} 위치" }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) = setMeasuredDimension(dp(72f).toInt(), dp(128f).toInt())

    override fun onDraw(c: Canvas) {
        val scale = minOf(width / 300f, height / 560f) // CarMapView와 같은 300×560 좌표
        val ox = (width - 300 * scale) / 2
        val oy = (height - 560 * scale) / 2
        car.setBounds(ox.toInt(), oy.toInt(), (ox + 300 * scale).toInt(), (oy + 560 * scale).toInt())
        car.draw(c)
        val col = color(when (state) {
            RowState.LEARNED -> R.color.green
            RowState.MISSING -> R.color.dot_line // dot_idle은 밝은 테마에서 흰색이다
            else -> R.color.blue
        })
        val px = ox + zone.x * scale
        val py = oy + zone.y * scale
        fill.color = col
        c.drawCircle(px, py, dp(7f), fill)
        ring.color = col; ring.strokeWidth = dp(2f)
        c.drawCircle(px, py, dp(13f), ring)
    }
}
