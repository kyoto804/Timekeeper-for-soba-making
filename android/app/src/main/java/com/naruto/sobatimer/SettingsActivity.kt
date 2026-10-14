package com.naruto.sobatimer

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.widget.RadioGroup
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val fontScaleSeek = findViewById<SeekBar>(R.id.fontScaleSeek)
        val fontScaleLabel = findViewById<TextView>(R.id.fontScaleLabel)

        val countdownSec = findViewById<EditText>(R.id.countdownSec)

        val minA = findViewById<EditText>(R.id.minA)
        val minB = findViewById<EditText>(R.id.minB)
        val minC = findViewById<EditText>(R.id.minC)

        val msgA1 = findViewById<EditText>(R.id.msgA1)
        val msgA2 = findViewById<EditText>(R.id.msgA2)
        val msgB1 = findViewById<EditText>(R.id.msgB1)
        val msgB2 = findViewById<EditText>(R.id.msgB2)
        val msgC1 = findViewById<EditText>(R.id.msgC1)
        val msgC2 = findViewById<EditText>(R.id.msgC2)

        val finishGroup = findViewById<RadioGroup>(R.id.finishGroup)
        val skipConfirmSwitch = findViewById<Switch>(R.id.skipConfirmSwitch)

        val volumeSeek = findViewById<SeekBar>(R.id.volumeSeek)
        val volumeLabel = findViewById<TextView>(R.id.volumeLabel)
        val testPlayBtn = findViewById<Button>(R.id.testPlayBtn)

        val pref = getSharedPreferences("settings", MODE_PRIVATE)

        val currentScale = pref.getFloat("fontScale", 1.0f)
        val progress = ((currentScale - 0.5f) * 100).toInt()
        fontScaleSeek.progress = progress
        fontScaleLabel.text = "フォント倍率: %.2f".format(currentScale)

        fontScaleSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, p: Int, fromUser: Boolean) {
                val scale = p / 100f + 0.5f
                fontScaleLabel.text = "フォント倍率: %.2f".format(scale)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        countdownSec.setText(pref.getInt("countdownSec", 5).toString())

        minA.setText(pref.getInt("minA", 600).toString())
        minB.setText(pref.getInt("minB", 1200).toString())
        minC.setText(pref.getInt("minC", 1800).toString())

        msgA1.setText(pref.getString("msgA1", "10分経過"))
        msgA2.setText(pref.getString("msgA2", "10分経過です"))
        msgB1.setText(pref.getString("msgB1", "20分経過"))
        msgB2.setText(pref.getString("msgB2", "20分経過です"))
        msgC1.setText(pref.getString("msgC1", "30分経過"))
        msgC2.setText(pref.getString("msgC2", "30分経過です"))

        when (pref.getInt("finishMin", 40)) {
            40 -> finishGroup.check(R.id.rb40)
            35 -> finishGroup.check(R.id.rb35)
            30 -> finishGroup.check(R.id.rb30)
        }

        skipConfirmSwitch.isChecked = pref.getBoolean("skipConfirm", false)

        val currentVolume = pref.getInt("voiceVolume", 100)
        volumeSeek.progress = currentVolume
        volumeLabel.text = "音量: $currentVolume"

        volumeSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, p: Int, fromUser: Boolean) {
                volumeLabel.text = "音量: $p"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // -----------------------------------------------------------
        // ★ テスト再生（msgA1）
        // -----------------------------------------------------------
        testPlayBtn.setOnClickListener {
            val volume = volumeSeek.progress / 100f
            val safeVolume = volume.coerceIn(0.0f, 1.0f)

            // 変数 tts を明示的に宣言する
            lateinit var ttsInstance: TextToSpeech
            ttsInstance = TextToSpeech(this) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    ttsInstance.language = Locale.JAPANESE
                    ttsInstance.setPitch(1.0f)
                    ttsInstance.setSpeechRate(1.0f)

                    val params = Bundle()
                    params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, safeVolume)

                    ttsInstance.speak(
                        msgA1.text.toString(),
                        TextToSpeech.QUEUE_FLUSH,
                        params,
                        "testPlay"
                    )
                }
            }
        }
        // -----------------------------------------------------------
        // 保存
        // -----------------------------------------------------------
        findViewById<Button>(R.id.saveBtn).setOnClickListener {

            val scale = fontScaleSeek.progress / 100f + 0.5f

            val selectedFinishMin = when (finishGroup.checkedRadioButtonId) {
                R.id.rb40 -> 40
                R.id.rb35 -> 35
                R.id.rb30 -> 30
                else -> 40
            }

            pref.edit().apply {

                putFloat("fontScale", scale)
                putInt("finishMin", selectedFinishMin)
                putInt("countdownSec", countdownSec.text.toString().toIntOrNull() ?: 5)

                putInt("voiceVolume", volumeSeek.progress)
                putBoolean("skipConfirm", skipConfirmSwitch.isChecked)

                putInt("minA", minA.text.toString().toIntOrNull() ?: 600)
                putInt("minB", minB.text.toString().toIntOrNull() ?: 1200)
                putInt("minC", minC.text.toString().toIntOrNull() ?: 1800)

                putString("msgA1", msgA1.text.toString())
                putString("msgA2", msgA2.text.toString())
                putString("msgB1", msgB1.text.toString())
                putString("msgB2", msgB2.text.toString())
                putString("msgC1", msgC1.text.toString())
                putString("msgC2", msgC2.text.toString())

                apply()
            }

            finish()
        }

        // -----------------------------------------------------------
        // 初期値に戻す
        // -----------------------------------------------------------
        findViewById<Button>(R.id.resetBtn).setOnClickListener {

            pref.edit().apply {

                putInt("finishMin", 40)
                putInt("countdownSec", 5)
                putInt("voiceVolume", 100)
                putBoolean("skipConfirm", false)

                putInt("minA", 600)
                putInt("minB", 1200)
                putInt("minC", 1800)

                putString("msgA1", "10分経過")
                putString("msgA2", "10分経過です")
                putString("msgB1", "20分経過")
                putString("msgB2", "20分経過です")
                putString("msgC1", "30分経過")
                putString("msgC2", "30分経過です")

                apply()
            }

            finishGroup.check(R.id.rb40)
            countdownSec.setText("5")
            skipConfirmSwitch.isChecked = false

            volumeSeek.progress = 100
            volumeLabel.text = "音量: 100"

            minA.setText("600")
            minB.setText("1200")
            minC.setText("1800")

            msgA1.setText("10分経過")
            msgA2.setText("10分経過です")
            msgB1.setText("20分経過")
            msgB2.setText("20分経過です")
            msgC1.setText("30分経過")
            msgC2.setText("30分経過です")
        }
    }
}