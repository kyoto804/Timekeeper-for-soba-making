/* -----------------------------------------------------------
   MainActivity.kt
   作成日: 2026-09-07
   更新日: 2026-10-25

   Ver 1.1
      - 時刻差分方式に変更
      - レスポンシブ対応
      - Foreground Service 対応
      - 3600秒で自動停止
      - コメント統一（短文化）
      - WindowInsetsController 対応

   Ver 1.4
      - 音量設定を追加（SettingsActivity と連動）

   Ver 1.5
      - 一時停止機能を追加（開始後に表示／停止→再開／終了で非表示）
      - 一時停止前に確認ダイアログを追加
      - 一時停止中はタイマー色をオレンジに変更
      - 再開時はタイマー色を白に戻す

   Ver 2.0
      - 経過時間（カウントダウン機能）の選択に対応
      - 設定画面で選択されたモード（カウントアップ／カウントダウン）を自動反映
      - 終了時間として45分・50分・55分の選択肢をフルサポート

   機能概要
      - フォント指定 / カウントダウン秒数設定 / 音声 ON/OFF
      - 10分・20分・30分の経過時間を音声案内
      - 35〜55分の残り時間を自動アナウンス
      - そば打ち中でも見やすい大きな画面
      - 時間とメッセージを自由に設定可能
      - 一時停止／再開機能（Ver 1.5）
      - カウントダウン機能の選択対応（Ver 2.0）
----------------------------------------------------------- */

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
import android.content.res.ColorStateList
import android.view.View
import androidx.activity.enableEdgeToEdge

class MainActivity : AppCompatActivity() {

    // UI
    private lateinit var tts: TextToSpeech
    private lateinit var timerText: TextView
    private lateinit var countdownText: TextView
    private lateinit var startBtn: Button
    private lateinit var stopBtn: Button
    private lateinit var settingsBtn: Button
    private lateinit var specBtn: Button
    private lateinit var pauseBtn: Button

    // 状態
    private val handler = Handler(Looper.getMainLooper())
    private var seconds = 0
    private var isRunning = false
    private var isPaused = false
    private var isFinished = false
    private var startTime: Long = 0L
    private var pausedAt: Long = 0L

    // 設定値（変数としてあらかじめ保持）
    private var finishMin = 40
    private var isCountdown = false
    private var countdownSec = 5
    private var minA = 600
    private var minB = 1200
    private var minC = 1800
    private var msgA1 = "10分経過"
    private var msgA2 = "10分経過です"
    private var msgB1 = "20分経過"
    private var msgB2 = "20分経過です"
    private var msgC1 = "30分経過"
    private var msgC2 = "30分経過です"

    private lateinit var pref: android.content.SharedPreferences

    // タイマーRunnable
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            if (isPaused) return

            val now = System.currentTimeMillis()
            seconds = ((now - startTime) / 1000).toInt()

            val finishSec = finishMin * 60

            val displaySec: Int
            if (isCountdown) {
                // カウントダウン（残り時間）
                displaySec = finishSec - seconds
                if (displaySec <= 0) {
                    timerText.text = "00:00"
                    timerText.setTextColor(Color.RED)
                    stopTimerAndShowElapsed()
                    return
                }
            } else {
                // カウントアップ（経過時間）
                displaySec = seconds
                if (seconds == finishSec) {
                    timerText.setTextColor(Color.RED)
                }
            }

            val min = displaySec / 60
            val sec = displaySec % 60

            timerText.text = String.format("%02d:%02d", min, sec)

            messages[seconds]?.let { (first, second) ->
                speakTwice(first, second)
            }

            if (seconds >= 3600) {
                stopTimerAndShowElapsed()
                return
            }

            handler.postDelayed(this, 1000)
        }
    }

    // メッセージ一覧
    private val messages: Map<Int, Pair<String, String>>
        get() {
            val finishSec = finishMin * 60

            val remain5Sec: Int
            val remain5Msg1: String

            when (finishMin) {
                55 -> { remain5Sec = 3000; remain5Msg1 = "50分経過" }
                50 -> { remain5Sec = 2700; remain5Msg1 = "45分経過" }
                45 -> { remain5Sec = 2400; remain5Msg1 = "40分経過" }
                40 -> { remain5Sec = 2100; remain5Msg1 = "35分経過" }
                35 -> { remain5Sec = 1800; remain5Msg1 = "30分経過" }
                30 -> { remain5Sec = 1500; remain5Msg1 = "25分経過" }            
                else -> { remain5Sec = 2100; remain5Msg1 = "35分経過" }
            }

            val cMessage = if (finishMin >= 40) {
                mapOf(minC to Pair(msgC1, msgC2))
            } else emptyMap()

            val dMessage = if (finishMin >= 50) {
                mapOf(2400 to Pair("40分", "40分経過です"))
            } else emptyMap()
            
            return mapOf(
                minA to Pair(msgA1, msgA2),
                minB to Pair(msgB1, msgB2),

                remain5Sec to Pair(remain5Msg1, "残り5分です"),

                (finishSec - 240) to Pair("残り4分", "残り4分です"),
                (finishSec - 180) to Pair("残り3分", "残り3分です"),
                (finishSec - 120) to Pair("残り2分", "残り2分です"),
                (finishSec - 60)  to Pair("残り1分", "残り1分です"),

                (finishSec - 30) to Pair("残り30秒", "残り30秒です"),
                (finishSec - 20) to Pair("残り20秒", "残り20秒です"),
                (finishSec - 10) to Pair("残り10秒", "残り10秒です"),

                finishSec to Pair("終了", "終了です"),
                3600 to Pair("60分経過しました。終了します。", "お疲れさまでした")
            ) + cMessage + dMessage
        }

    // onCreate
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pref = getSharedPreferences("settings", MODE_PRIVATE)
        hideSystemBars()

        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        timerText = findViewById(R.id.timerText)
        countdownText = findViewById(R.id.countdownText)
        loadSettings()

        // Android 13 通知権限
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
        pauseBtn = findViewById(R.id.pauseBtn)

        // pauseBtn 初期化
        pauseBtn.isEnabled = false
        pauseBtn.visibility = View.INVISIBLE
        pauseBtn.backgroundTintList =
            ColorStateList.valueOf(Color.parseColor("#CCCC00"))
        pauseBtn.text = "一時停止"

        // レスポンシブ配置
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

            pauseBtn.x = w * 0.425f
            pauseBtn.y = h * 0.80f

            val scale = pref.getFloat("fontScale", 1.0f)
            val px = h * 0.25f * scale
            val sp = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                px,
                resources.displayMetrics
            )
            timerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp)
        }

        // 開始
        startBtn.setOnClickListener {
            if (isRunning || isPaused) return@setOnClickListener

            isRunning = true
            startBtn.isEnabled = false
            startBtn.text = "計測中"
            startBtn.backgroundTintList =
                ColorStateList.valueOf(Color.parseColor("#444444"))
            isFinished = false
            stopBtn.text = "終了"

            seconds = 0

            if (isCountdown) {
                finishMin = pref.getInt("finishMin", 40)
                timerText.text = String.format("%02d:00", finishMin)
            } else {
                timerText.text = "00:00"
            }
            timerText.setTextColor(Color.WHITE)

            handler.removeCallbacks(timerRunnable)
            startTime = 0L

            speakTwice("準備が整ったようですので開始します", "")
            startCountdown()
        }

        // 設定
        settingsBtn.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // 仕様
        specBtn.setOnClickListener {
            startActivity(Intent(this, SpecActivity::class.java))
        }

        // 終了
        stopBtn.setOnClickListener {
            if (isFinished) {
                resetTimer()
                return@setOnClickListener
            }
            val skipConfirm = pref.getBoolean("skipConfirm", false)
            if (skipConfirm) {
                stopTimerAndShowElapsed()
                isRunning = false
                startBtn.isEnabled = true
                startBtn.text = "開始"
                startBtn.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor("#0066FF"))
                return@setOnClickListener
            }

            AlertDialog.Builder(this)
                .setTitle("確認")
                .setMessage("タイマーを終了しますか？")
                .setPositiveButton("終了") { _, _ ->
                    stopTimerAndShowElapsed()
                    isRunning = false
                    startBtn.isEnabled = true
                    startBtn.text = "開始"
                    startBtn.backgroundTintList =
                        ColorStateList.valueOf(Color.parseColor("#0066FF"))
                }
                .setNegativeButton("キャンセル", null)
                .show()
        }

        // 一時停止 / 再開
        pauseBtn.setOnClickListener {
            if (!isPaused) {
                val skipConfirm = pref.getBoolean("skipConfirm", false)
                if (skipConfirm) {
                    isPaused = true
                    pausedAt = System.currentTimeMillis()
                    isRunning = false

                    pauseBtn.text = "再開"
                    pauseBtn.backgroundTintList =
                        ColorStateList.valueOf(Color.parseColor("#00CC66"))

                    timerText.setTextColor(Color.parseColor("#FFA500"))
                    return@setOnClickListener
                }
                AlertDialog.Builder(this)
                    .setTitle("確認")
                    .setMessage("一時停止しますか？")
                    .setPositiveButton("停止") { _, _ ->

                        isPaused = true
                        pausedAt = System.currentTimeMillis()
                        isRunning = false

                        pauseBtn.text = "再開"
                        pauseBtn.backgroundTintList =
                            ColorStateList.valueOf(Color.parseColor("#00CC66"))

                        timerText.setTextColor(Color.parseColor("#FFA500"))
                    }
                    .setNegativeButton("キャンセル", null)
                    .show()

            } else {

                val pausedDuration = System.currentTimeMillis() - pausedAt
                startTime += pausedDuration

                isPaused = false
                isRunning = true

                pauseBtn.text = "一時停止"
                pauseBtn.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor("#CCCC00"))

                timerText.setTextColor(Color.WHITE)
                handler.post(timerRunnable)
            }
        }
    }

    // バナー削除
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
        startBtn.post { updateFontScale() }
    }

    private fun loadSettings() {
        // 設定値を一度まとめて読み込み、変数にキャッシュする
        finishMin = pref.getInt("finishMin", 40)
        isCountdown = pref.getBoolean("isCountdown", false)
        countdownSec = pref.getInt("countdownSec", 5)

        minA = pref.getInt("minA", 600)
        msgA1 = pref.getString("msgA1", "10分経過")!!
        msgA2 = pref.getString("msgA2", "10分経過です")!!

        minB = pref.getInt("minB", 1200)
        msgB1 = pref.getString("msgB1", "20分経過")!!
        msgB2 = pref.getString("msgB2", "20分経過です")!!

        minC = pref.getInt("minC", 1800)
        msgC1 = pref.getString("msgC1", "30分経過")!!
        msgC2 = pref.getString("msgC2", "30分経過です")!!

        if (!isRunning && !isPaused) {
            countdownText.text = if (countdownSec == 0) "" else "開始まで: $countdownSec"

            if (isCountdown) {
                timerText.text = String.format("%02d:00", finishMin)
            } else {
                timerText.text = "00:00"
            }
            timerText.setTextColor(Color.WHITE)
        }
    }

    // 音声
    private fun speakTwice(first: String, second: String) {
        val volume = pref.getInt("voiceVolume", 100) / 100f

        if (volume <= 0f) return

        val safeVolume = volume.coerceIn(0.0f, 1.0f)

        val params = Bundle()
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, safeVolume)

        tts.speak(first, TextToSpeech.QUEUE_FLUSH, params, "utt1")

        val delay = when {
            first.length <= 6 -> 350L
            first.length <= 12 -> 500L
            else -> 700L
        }

        handler.postDelayed({
            tts.speak(second, TextToSpeech.QUEUE_ADD, params, "utt2")
        }, delay)
    }

    // カウントダウン
    private fun startCountdown() {
        var count = countdownSec

        handler.post(object : Runnable {
            override fun run() {

                if (!isRunning) return

                when {
                    count > 1 -> countdownText.text = "開始まで: $count"
                    count == 1 -> {
                        countdownText.text = "開始まで: 1"
                        speakTwice("よーーい", "はじめ")
                    }
                    count == 0 -> {
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
        pauseBtn.visibility = View.VISIBLE
        pauseBtn.isEnabled = true

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
        isPaused = false
        handler.removeCallbacks(timerRunnable)

        val serviceIntent = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        startService(serviceIntent)
        
        val displaySec: Int
        if (isCountdown) {
             // カウントダウン（残り時間）
             displaySec = (finishMin * 60) - seconds
         } else {
             // カウントアップ（経過時間）
             displaySec = seconds
        } 

        val min = displaySec / 60
        val sec = displaySec % 60

        timerText.setTextColor(Color.GREEN)
        timerText.text = String.format("%02d:%02d", min, sec)

        startBtn.isEnabled = true
        isFinished = true
        stopBtn.text = "リセット"
        
        pauseBtn.visibility = View.INVISIBLE
        pauseBtn.isEnabled = false
        pauseBtn.text = "一時停止"
        pauseBtn.backgroundTintList =
            ColorStateList.valueOf(Color.parseColor("#CCCC00"))
    }
    
    private fun resetTimer() {
        seconds = 0

        isRunning = false
        isPaused = false
        isFinished = false

        startTime = 0L
        pausedAt = 0L

        if (isCountdown) {
            timerText.text = String.format("%02d:00", finishMin)
        } else {
            timerText.text = "00:00"
        }
        timerText.setTextColor(Color.WHITE)

        stopBtn.text = "終了"

        pauseBtn.visibility = View.INVISIBLE
        pauseBtn.isEnabled = false

        startBtn.isEnabled = true
        startBtn.text = "開始"
        startBtn.backgroundTintList =
            ColorStateList.valueOf(Color.parseColor("#0066FF"))

        countdownText.text = if (countdownSec == 0) "" else "開始まで: $countdownSec"
    }

    override fun onDestroy() {
        tts.shutdown()
        super.onDestroy()
    }

    private fun updateFontScale() {
        val root = startBtn.rootView
        val h = root.height.toFloat()

        val scale = pref.getFloat("fontScale", 1.0f)

        val px = h * 0.25f * scale
        val sp = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            px,
            resources.displayMetrics
        )

        timerText.setTextSize(TypedValue.COMPLEX_UNIT_PX, sp)
    }
}