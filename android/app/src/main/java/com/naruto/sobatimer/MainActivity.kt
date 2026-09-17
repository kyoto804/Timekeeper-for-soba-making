// -----------------------------------------------------------
// MainActivity.kt
// 作成日: 2026-09-07
// 変更日: 2026-09-11
// Ver: 1.1（時刻差分方式・レスポンシブ対応・FGS・3600秒停止・短いコメント統一）
// Ver: 1.1（警告ゼロ・WindowInsetsController対応・3600秒停止・短いコメント統一）
// -----------------------------------------------------------

package com.naruto.sobatimer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.TypedValue
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    // 変数
    private lateinit var tts: TextToSpeech
    private lateinit var timerText: TextView
    private lateinit var countdownText: TextView
    private lateinit var startBtn: Button
    private lateinit var stopBtn: Button
    private lateinit var settingsBtn: Button
    private lateinit var specBtn: Button

    private val handler = Handler(Looper.getMainLooper())
    private var seconds = 0
    private var isRunning = false
    private var startTime: Long = 0L

    private var minA = 600
    private var minB = 1200
    private var minC = 1800
    private var msgA1 = "10分経過"
    private var msgA2 = "10分経過です"
    private var msgB1 = "20分経過"
    private var msgB2 = "20分経過です"
    private var msgC1 = "30分経過"
    private var msgC2 = "30分経過です"

    // ★ タイマーRunnable（3600秒停止のため変数化）
    private val timerRunnable = object : Runnable {
        override fun run() {

            if (!isRunning) return

            val now = System.currentTimeMillis()
            seconds = ((now - startTime) / 1000).toInt()

            val min = seconds / 60
            val sec = seconds % 60

            if (seconds == 2400) timerText.setTextColor(Color.RED)
            timerText.text = String.format("%02d:%02d", min, sec)

            messages[seconds]?.let { (first, second) -> speakTwice(first, second) }

            // ★ 3600秒で確実停止
            if (seconds >= 3600) {
                stopTimerAndShowElapsed()
                return
            }

            handler.postDelayed(this, 1000)
        }
    }

    // メッセージ一覧
    private val messages: Map<Int, Pair<String, String>>
        get() = mapOf(
            minA to Pair(msgA1, msgA2),
            minB to Pair(msgB1, msgB2),
            minC to Pair(msgC1, msgC2),
            2100 to Pair("35分経過", "残り5分です"),
            2160 to Pair("残り4分", "残り4分です"),
            2220 to Pair("残り3分", "残り3分です"),
            2280 to Pair("残り2分", "残り2分です"),
            2340 to Pair("残り1分", "残り1分です"),
            2370 to Pair("残り30秒", "残り30秒です"),
            2380 to Pair("残り20秒", "残り20秒です"),
            2390 to Pair("残り10秒", "残り10秒です"),
            2400 to Pair("終了", "終了です"),
            3600 to Pair("60分経過しました。終了します。", "お疲れさまでした")
        )

    // onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ★ バナー削除（Android14対応）
        hideSystemBars()

        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        timerText = findViewById(R.id.timerText)
        countdownText = findViewById(R.id.countdownText)
        loadSettings()

        // ★ Android 13 通知権限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }

        tts = TextToSpeech(this) { tts.language = Locale.JAPANESE }

        startBtn = findViewById(R.id.startBtn)
        stopBtn = findViewById(R.id.stopBtn)
        settingsBtn = findViewById(R.id.settingsBtn)
        specBtn = findViewById(R.id.specBtn)

        // ★ レスポンシブ配置
        startBtn.post {
            val root = startBtn.rootView
            val w = root.width.toFloat()
            val h = root.height.toFloat()

            startBtn.x = w * 0.05f
            startBtn.y = h * 0.05f
            settingsBtn.x = w * 0.80f
            settingsBtn.y = h * 0.05f
            stopBtn.x = w * 0.05f
            stopBtn.y = h * 0.80f
            specBtn.x = w * 0.80f
            specBtn.y = h * 0.80f

            val px = h * 0.25f
            val sp = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                px,
                resources.displayMetrics
            )
            timerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp)
        }

        // ★ 開始
        startBtn.setOnClickListener {
            seconds = 0
            timerText.text = "00:00"
            timerText.setTextColor(Color.WHITE)
            handler.removeCallbacks(timerRunnable)
            isRunning = false
            startTime = 0L
            startBtn.isEnabled = false
            speakTwice("準備が整ったようですので開始します", "")
            startCountdown()
        }

        // ★ 設定
        settingsBtn.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // ★ 仕様
        specBtn.setOnClickListener {
            startActivity(Intent(this, SpecActivity::class.java))
        }

        // ★ 終了
        stopBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("確認")
                .setMessage("タイマーを終了しますか？")
                .setPositiveButton("終了") { _, _ -> stopTimerAndShowElapsed() }
                .setNegativeButton("キャンセル", null)
                .show()
        }
    }

    // ★ バナー削除（新API）
    private fun hideSystemBars() {
        val controller = window.insetsController ?: return
        controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        controller.systemBarsBehavior =
            WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    // 設定ロード
    override fun onResume() {
        super.onResume()
        loadSettings()
        hideSystemBars()
    }

    private fun loadSettings() {
        val pref = getSharedPreferences("settings", MODE_PRIVATE)
        minA = pref.getInt("minA", 600)
        msgA1 = pref.getString("msgA1", "10分経過")!!
        msgA2 = pref.getString("msgA2", "10分経過です")!!
        minB = pref.getInt("minB", 1200)
        msgB1 = pref.getString("msgB1", "20分経過")!!
        msgB2 = pref.getString("msgB2", "20分経過です")!!
        minC = pref.getInt("minC", 1800)
        msgC1 = pref.getString("msgC1", "30分経過")!!
        msgC2 = pref.getString("msgC2", "30分経過です")!!
    }

    // 音声（2回）
    private fun speakTwice(first: String, second: String) {
        tts.speak(first, TextToSpeech.QUEUE_FLUSH, null, null)
        val delay = when {
            first.length <= 6 -> 350L
            first.length <= 12 -> 500L
            else -> 700L
        }
        handler.postDelayed({
            tts.speak(second, TextToSpeech.QUEUE_ADD, null, null)
        }, delay)
    }

    // カウントダウン
    private fun startCountdown() {
        var count = 5
        countdownText.text = "開始まで: $count"
        handler.post(object : Runnable {
            override fun run() {
                when (count) {
                    in 2..5 -> countdownText.text = "開始まで: $count"
                    1 -> {
                        countdownText.text = "開始まで: 1"
                        speakTwice("よーーい", "はじめ")
                    }
                    0 -> {
                        countdownText.text = ""
                        startTimer()
                        return
                    }
                }
                count--
                handler.postDelayed(this, 1000)
            }
        })
    }

    // タイマー開始
    private fun startTimer() {
        isRunning = true
        startTime = System.currentTimeMillis()

        // ★ Foreground Service開始
        val serviceIntent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        handler.post(timerRunnable)
    }

    // 終了処理
    private fun stopTimerAndShowElapsed() {
        isRunning = false
        handler.removeCallbacks(timerRunnable)

        // ★ Foreground Service停止
        val serviceIntent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        startService(serviceIntent)

        val min = seconds / 60
        val sec = seconds % 60
        timerText.setTextColor(Color.GREEN)
        timerText.text = String.format("%02d:%02d", min, sec)

        startBtn.isEnabled = true
    }

    // 終了時
    override fun onDestroy() {
        tts.shutdown()
        super.onDestroy()
    }
}
