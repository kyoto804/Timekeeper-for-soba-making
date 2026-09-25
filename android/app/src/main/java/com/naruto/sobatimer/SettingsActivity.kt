// -----------------------------------------------------------
// SettingsActivity.kt
// 作成日: 2026-09-07
// Ver: 1.2（カウントダウン秒数＋音声ON/OFF追加）
// -----------------------------------------------------------

package com.naruto.sobatimer

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import android.widget.SeekBar
import android.widget.TextView
import android.widget.RadioGroup   // ★ 追加

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // -----------------------------------------------------------
        // UI 要素取得
        // -----------------------------------------------------------
        val fontScaleSeek = findViewById<SeekBar>(R.id.fontScaleSeek)
        val fontScaleLabel = findViewById<TextView>(R.id.fontScaleLabel)
        
        val countdownSec = findViewById<EditText>(R.id.countdownSec)
        val soundSwitch = findViewById<Switch>(R.id.soundSwitch)

        val minA = findViewById<EditText>(R.id.minA)
        val minB = findViewById<EditText>(R.id.minB)
        val minC = findViewById<EditText>(R.id.minC)

        val msgA1 = findViewById<EditText>(R.id.msgA1)
        val msgA2 = findViewById<EditText>(R.id.msgA2)

        val msgB1 = findViewById<EditText>(R.id.msgB1)
        val msgB2 = findViewById<EditText>(R.id.msgB2)

        val msgC1 = findViewById<EditText>(R.id.msgC1)
        val msgC2 = findViewById<EditText>(R.id.msgC2)

        // ★ 終了時間（40 / 35 / 30）
        val finishGroup = findViewById<RadioGroup>(R.id.finishGroup)
        val pref = getSharedPreferences("settings", MODE_PRIVATE)

        // -----------------------------------------------------------
        // 現在の設定値を表示
        // -----------------------------------------------------------

        // ★ 現在の倍率を読み込み
        val currentScale = pref.getFloat("fontScale", 1.0f)
        val progress = ((currentScale - 0.5f) * 100).toInt()
        fontScaleSeek.progress = progress
        fontScaleLabel.text = "フォント倍率: %.2f".format(currentScale)

        // ★ スライダー変更時
        fontScaleSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, p: Int, fromUser: Boolean) {
                val scale = p / 100f + 0.5f
                fontScaleLabel.text = "フォント倍率: %.2f".format(scale)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        countdownSec.setText(pref.getInt("countdownSec", 5).toString())
        soundSwitch.isChecked = pref.getBoolean("soundEnabled", true)

        minA.setText(pref.getInt("minA", 600).toString())
        minB.setText(pref.getInt("minB", 1200).toString())
        minC.setText(pref.getInt("minC", 1800).toString())

        msgA1.setText(pref.getString("msgA1", "10分経過"))
        msgA2.setText(pref.getString("msgA2", "10分経過です"))

        msgB1.setText(pref.getString("msgB1", "20分経過"))
        msgB2.setText(pref.getString("msgB2", "20分経過です"))

        msgC1.setText(pref.getString("msgC1", "30分経過"))
        msgC2.setText(pref.getString("msgC2", "30分経過です"))

        // ★ 終了時間（40 / 35 / 30）
        val finishMin = pref.getInt("finishMin", 40)
        when (finishMin) {
            40 -> finishGroup.check(R.id.rb40)
            35 -> finishGroup.check(R.id.rb35)
            30 -> finishGroup.check(R.id.rb30)
        }
        // -----------------------------------------------------------
        // 保存ボタン
        // -----------------------------------------------------------
        findViewById<Button>(R.id.saveBtn).setOnClickListener {
            val scale = fontScaleSeek.progress / 100f + 0.5f
            // ★ ラジオボタンの選択値
            val selectedFinishMin = when (finishGroup.checkedRadioButtonId) {
                R.id.rb40 -> 40
                R.id.rb35 -> 35
                R.id.rb30 -> 30
                else -> 40
            }
            pref.edit().apply {
                // ★ スライダーの値を
                putFloat("fontScale", scale)
                // ★ 終了時間保存
                putInt("finishMin", selectedFinishMin)
                // ★ カウントダウン秒数
                putInt("countdownSec", countdownSec.text.toString().toIntOrNull() ?: 5)
                // ★ 音声 ON/OFF
                putBoolean("soundEnabled", soundSwitch.isChecked)

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
        // 初期値に戻すボタン
        // -----------------------------------------------------------
        findViewById<Button>(R.id.resetBtn).setOnClickListener {

            pref.edit().apply {
                // ★ 終了時間を初期値（40分）に戻す
                putInt("finishMin", 40)
                putInt("countdownSec", 5)
                putBoolean("soundEnabled", true)

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
            soundSwitch.isChecked = true
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
