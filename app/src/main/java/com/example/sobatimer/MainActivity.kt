// ------------------------------------------------------------
// MainActivity.kt
// 作成日: 2026-09-07
// Ver: 1.0
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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import java.util.Locale

class MainActivity : AppCompatActivity() {

    // ------------------------------------------------------------
    // 変数定義
    // ------------------------------------------------------------
    private lateinit var tts: TextToSpeech
    private lateinit var timerText: TextView
    private lateinit var countdownText: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var seconds = 0
    private var isRunning = false

    private var minA = 600
    private var minB = 1200
    private var minC = 1800

    private var msgA1 = "10分経過"
    private var msgA2 = "10分経過です"

    private var msgB1 = "20分経過"
    private var msgB2 = "20分経過です"

    private var msgC1 = "30分経過"
    private var msgC2 = "30分経過です"


    // ------------------------------------------------------------
    // onCreate（初期化）
    // ------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        timerText = findViewById(R.id.timerText)
        countdownText = findViewById(R.id.countdownText)

        loadSettings()

        tts = TextToSpeech(this) { tts.language = Locale.JAPANESE }

        // ★ 開始ボタン
        findViewById<Button>(R.id.startBtn).setOnClickListener {
            findViewById<Button>(R.id.startBtn).isEnabled = false
            speakTwice("準備が整ったようですので開始します", "")
            startCountdown()
        }

        findViewById<Button>(R.id.settingsBtn).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<Button>(R.id.specBtn).setOnClickListener {
            startActivity(Intent(this, SpecActivity::class.java))
        }

        // ★ 終了ボタン（確認ダイアログ）
        findViewById<Button>(R.id.stopBtn).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("確認")
                .setMessage("タイマーを終了しますか？")
                .setPositiveButton("終了") { _, _ ->
                    stopTimerAndShowElapsed()
                    findViewById<Button>(R.id.startBtn).isEnabled = true
                }
                .setNegativeButton("キャンセル", null)
                .show()
        }
    }


    // ------------------------------------------------------------
    // 設定ロード
    // ------------------------------------------------------------
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


    // ------------------------------------------------------------
    // 音声処理（2回読み上げ）
    // ------------------------------------------------------------
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


    // ------------------------------------------------------------
    // カウントダウン処理
    // ------------------------------------------------------------
    private fun startCountdown() {
        var count = 5
        countdownText.text = "開始まで: $count"
        timerText.setTextColor(Color.WHITE)

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


    // ------------------------------------------------------------
    // タイマー処理
    // ------------------------------------------------------------
    private fun startTimer() {
        isRunning = true
        seconds = 0

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

                val min = seconds / 60
                val sec = seconds % 60

                // ★ 40分経過した瞬間だけ赤にする
                if (seconds == 2400) {
                    timerText.setTextColor(Color.RED)
                }

                timerText.text = String.format("%02d:%02d", min, sec)

                messages[seconds]?.let { (first, second) ->
                    speakTwice(first, second)
                }

                seconds++
                handler.postDelayed(this, 1000)
            }
        })
    }


    // ------------------------------------------------------------
    // 終了処理
    // ------------------------------------------------------------
    private fun stopTimerAndShowElapsed() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)

        val min = seconds / 60
        val sec = seconds % 60

        timerText.setTextColor(Color.GREEN)
        timerText.text = String.format("%02d:%02d", min, sec)
    }


    // ------------------------------------------------------------
    // 終了時クリーンアップ
    // ------------------------------------------------------------
    override fun onDestroy() {
        tts.shutdown()
        super.onDestroy()
    }
}
