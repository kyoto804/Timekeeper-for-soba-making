// ------------------------------------------------------------
// SettingsActivity.kt
// 作成日: 2026-09-07
// Ver: 1.0
// ------------------------------------------------------------

package com.example.sobatimer

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    // ------------------------------------------------------------
    // onCreate（初期化）
    // ------------------------------------------------------------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // ------------------------------------------------------------
        // UI 要素取得
        // ------------------------------------------------------------
        val minA = findViewById<EditText>(R.id.minA)
        val minB = findViewById<EditText>(R.id.minB)
        val minC = findViewById<EditText>(R.id.minC)

        val msgA1 = findViewById<EditText>(R.id.msgA1)
        val msgA2 = findViewById<EditText>(R.id.msgA2)

        val msgB1 = findViewById<EditText>(R.id.msgB1)
        val msgB2 = findViewById<EditText>(R.id.msgB2)

        val msgC1 = findViewById<EditText>(R.id.msgC1)
        val msgC2 = findViewById<EditText>(R.id.msgC2)

        val pref = getSharedPreferences("settings", MODE_PRIVATE)

        // ------------------------------------------------------------
        // 現在の設定値を表示
        // ------------------------------------------------------------
        minA.setText(pref.getInt("minA", 600).toString())
        minB.setText(pref.getInt("minB", 1200).toString())
        minC.setText(pref.getInt("minC", 1800).toString())

        msgA1.setText(pref.getString("msgA1", "10分経過"))
        msgA2.setText(pref.getString("msgA2", "10分経過です"))

        msgB1.setText(pref.getString("msgB1", "20分経過"))
        msgB2.setText(pref.getString("msgB2", "20分経過です"))

        msgC1.setText(pref.getString("msgC1", "30分経過"))
        msgC2.setText(pref.getString("msgC2", "30分経過です"))

        // ------------------------------------------------------------
        // 保存ボタン
        // ------------------------------------------------------------
        findViewById<Button>(R.id.saveBtn).setOnClickListener {

            pref.edit().apply {
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

        // ------------------------------------------------------------
        // 初期値に戻すボタン
        // ------------------------------------------------------------
        findViewById<Button>(R.id.resetBtn).setOnClickListener {

            pref.edit().apply {
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
