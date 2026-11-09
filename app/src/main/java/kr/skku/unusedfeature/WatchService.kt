package kr.skku.unusedfeature

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.OnInitializeListener
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import java.util.Locale

/** 운전을 지켜보다 P가 되면 놓친 기능 하나를 음성·알림으로 알린다. */
class WatchService : Service() {

    companion object {
        private const val TAG = "WatchService"
        private const val CH_WATCH = "watch"
        private const val CH_TIP = "tip"
        private const val ID_WATCH = 1
        private const val ID_TIP = 2

        // 아래 값은 같은 프로세스의 MainActivity가 읽는다(메인 스레드)

        /** 연결 문제 문구 */
        val problems = sortedSetOf<String>()

        /** 이 차가 사용 신호를 주지 않는 기능. 진행 원의 분모에서 뺀다 */
        val unsupported = mutableSetOf<Feature>()

        /** 기어가 P가 아닌지 (주행 중 화면). 운전 도중 시작돼 기록하지 않는 운전도 포함 */
        var driving = false
            private set

        /** 지금 운전을 기록하는지. 운전 도중에 서비스가 시작됐으면 false */
        var recording = false
            private set

        /** 화면이 열려 있을 때만 설정된다 */
        var onUpdate: (() -> Unit)? = null

        fun start(context: Context) =
            ContextCompat.startForegroundService(context, Intent(context, WatchService::class.java))
    }

    private lateinit var progress: Progress
    private lateinit var watcher: CarWatcher
    private var learnedAtLastEnd = emptySet<Feature>() // 확인 문장 판단용: 지난 운전 끝 이후 새로 익힌 것
    private var tts: TextToSpeech? = null
    @Volatile private var ttsReady = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        progress = Progress(this)
        learnedAtLastEnd = progress.learned()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_WATCH, "운전 지켜보기", NotificationManager.IMPORTANCE_MIN))
        nm.createNotificationChannel(NotificationChannel(CH_TIP, "안 써본 기능 안내", NotificationManager.IMPORTANCE_HIGH))
        ServiceCompat.startForeground(
            this, ID_WATCH,
            notification(CH_WATCH, "운전을 지켜보는 중", "운전이 끝나면 놓친 기능 하나를 알려 드려요.", ongoing = true),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        startTts()
        watcher = CarWatcher(this, ::onChange, ::onDriveEnd, ::onProblem, ::onUnsupported)
        watcher.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onDestroy() {
        if (::watcher.isInitialized) watcher.stop()
        runCatching { tts?.release() }
        driving = false; recording = false
        onUpdate?.invoke()
        super.onDestroy()
    }

    /** 신호가 바뀔 때마다. 사용 신호가 오면(정차 중 포함) 바로 익힘으로 저장한다 */
    private fun onChange() {
        var changed = problems.remove(NO_SIGNAL) // 신호가 들어온다 = 재시도로 연결됨
        val tracker = watcher.tracker
        if (driving != watcher.notParked || recording != tracker.driving) {
            driving = watcher.notParked; recording = tracker.driving; changed = true
        }
        val newly = tracker.usedNow() - progress.learned()
        if (newly.isNotEmpty()) {
            val note = tracker.minutesIntoDrive()?.let { if (it == 0L) "운전을 시작하자마자" else "운전을 시작하고 ${it}분 뒤" }
                ?: if (watcher.notParked) "운전 중" else "정차 중" // 기록하지 않는 운전(운전 도중 시작)은 몇 분 뒤인지 모른다
            progress.learn(newly, note)
            changed = true
        }
        if (changed) onUpdate?.invoke()
    }

    /** 저장·발화만 한다. driving/recording 정리와 화면 갱신은 바로 뒤 onChange(CarWatcher.onGear)가 한다 */
    private fun onDriveEnd(stats: DriveStats) {
        val learnedNow = progress.learned()
        val outcome = Coach.finish(stats, progress.shown(), learnedAtLastEnd, learnedNow, unsupported)
        if (outcome != null) {
            progress.record(outcome)
            learnedAtLastEnd = learnedNow
            outcome.pick?.let {
                speak(it.speech)
                getSystemService(NotificationManager::class.java)
                    .notify(ID_TIP, notification(CH_TIP, "안 써본 기능: ${it.title}", it.speech, ongoing = false))
            }
        }
    }

    private fun onProblem(message: String) {
        if (problems.add(message)) onUpdate?.invoke() // 10초마다 재시도해도 같은 띠면 화면을 다시 그리지 않는다
    }

    private fun onUnsupported(f: Feature) {
        if (unsupported.add(f)) onUpdate?.invoke()
    }

    private fun startTts() {
        val t = TextToSpeech(applicationContext, Mode.ON_DEVICE)
        tts = t
        try {
            t.initialize(object : OnInitializeListener {
                override fun onSuccess() {
                    runCatching { t.setLocale(Locale.KOREA) }
                    ttsReady = true
                }
                override fun onFailure(error: ErrorType) {
                    Log.w(TAG, "TTS 초기화 실패: $error")
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "TTS 초기화 실패", e) // 알림과 화면은 그대로 남는다
        }
    }

    private fun speak(text: String) {
        if (!ttsReady) {
            Log.w(TAG, "TTS 준비 안 됨, 음성 생략")
            return
        }
        runCatching { tts?.speak(text, true) }.onFailure { Log.w(TAG, "TTS 발화 실패", it) }
    }

    private fun notification(channel: String, title: String, text: String, ongoing: Boolean): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, channel)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .build()
    }
}
