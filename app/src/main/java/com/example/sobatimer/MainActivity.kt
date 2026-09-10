// ------------------------------------------------------------
// MainActivity.kt
// 作成日: 2026-09-07
// 変更日: 2026-09-10
// Ver: 1.1（時刻差分方式・レスポンシブ対）
// ------------------------------------------------------------

package com.example.sobatimer

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import android.content.Intent
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tts: TextToSpeech
    private lateinit var timerText: TextView
    private lateinit var countdownText: TextView

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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        timerText = findViewById(R.id.timerText)
        countdownText = findViewById(R.id.countdownText)

        loadSettings()

        tts = TextToSpeech(this) { tts.language = Locale.JAPANESE }

        val startBtn = findViewById<Button>(R.id.startBtn)
        val stopBtn = findViewById<Button>(R.id.stopBtn)
        val settingsBtn = findViewById<Button>(R.id.settingsBtn)
        val specBtn = findViewById<Button>(R.id.specBtn)

        // ------------------------------------------------------------
        // ★ レスポンシブ対応（画面サイズに応じて4隅に配置）
        // ------------------------------------------------------------
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

            // ------------------------------------------------------------
            // ★ 最小限の修正：巨大フォント（scaledDensity → applyDimension）
            // ------------------------------------------------------------
            val px = h * 0.25f
            val sp = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                px,
                resources.displayMetrics
            )
            timerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp)
        }

        // ------------------------------------------------------------
        // ★ 開始ボタン（iOS版と完全一致）
        // ------------------------------------------------------------
        startBtn.setOnClickListener {
            seconds = 0
            timerText.text = "00:00"
            timerText.setTextColor(Color.WHITE)

            handler.removeCallbacksAndMessages(null)
            isRunning = false
            startTime = 0L

            startBtn.isEnabled = false

            speakTwice("準備が整ったようですので開始します", "")
            startCountdown()
        }

        settingsBtn.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        specBtn.setOnClickListener {
            startActivity(Intent(this, SpecActivity::class.java))
        }

        stopBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("確認")
                .setMessage("タイマーを終了しますか？")
                .setPositiveButton("終了") { _, _ ->
                    stopTimerAndShowElapsed()
                    startBtn.isEnabled = true
                }
                .setNegativeButton("キャンセル", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadSettings()
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

    private fun startTimer() {

        isRunning = true
        startTime = System.currentTimeMillis()

        val messages = mapOf(
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

        handler.post(object : Runnable {
            override fun run() {
                if (!isRunning) return

                val now = System.currentTimeMillis()
                seconds = ((now - startTime) / 1000).toInt()

                val min = seconds / 60
                val sec = seconds % 60

                if (seconds == 2400) {
                    timerText.setTextColor(Color.RED)
                }

                timerText.text = String.format("%02d:%02d", min, sec)

                messages[seconds]?.let { (first, second) ->
                    speakTwice(first, second)
                }

                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun stopTimerAndShowElapsed() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)

        val min = seconds / 60
        val sec = seconds % 60

        timerText.setTextColor(Color.GREEN)
        timerText.text = String.format("%02d:%02d", min, sec)
    }

    override fun onDestroy() {
        tts.shutdown()
        super.onDestroy()
    }
}
