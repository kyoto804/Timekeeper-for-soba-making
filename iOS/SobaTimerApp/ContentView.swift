//
//  ContentView.swift
//  SobaTimerApp
//
//  Created by hiroshi on 2026/09/13.
//  Updated on 2026/09/29
//
//  【追加機能】
//  ・終了時間（40 / 35 / 30）の選択機能を追加
//  ・終了時の確認メッセージを省略
//  ・一時停止機能追加
import SwiftUI
import AVFoundation
import Combine

// MARK: - メイン画面（タイマー画面：横固定）

struct ContentView: View {

    @Environment(\.rotationLock) var rotationLock

    @State private var seconds = 0
    @State private var isRunning = false
    @State private var countdown = 5
    @State private var isCountingDown = false
    @State private var timerColor = Color.white
    @State private var startTime: Date?
    @State private var showStopAlert = false
    @State private var isPaused = false
    @State private var pausedAt: Date?
    @State private var showPauseConfirm = false   // ★追加：一時停止の確認ダイアログ

    @AppStorage("minA") private var minA = 600
    @AppStorage("msgA1") private var msgA1 = "10分経過"
    @AppStorage("msgA2") private var msgA2 = "10分経過です"

    @AppStorage("minB") private var minB = 1200
    @AppStorage("msgB1") private var msgB1 = "20分経過"
    @AppStorage("msgB2") private var msgB2 = "20分経過です"

    @AppStorage("minC") private var minC = 1800
    @AppStorage("msgC1") private var msgC1 = "30分経過"
    @AppStorage("msgC2") private var msgC2 = "30分経過です"
    
    @AppStorage("countdownSec") private var countdownSec = 5
    @AppStorage("timerFontSize") private var timerFontSize = 100
    // ★追加：終了時間（40 / 35 / 30）
    @AppStorage("finishMin") private var finishMin = 40
    @AppStorage("skipConfirm") private var skipConfirm = false   // ← ★追加
    @AppStorage("voiceVolume") private var voiceVolume = 100   // 0〜150
    
    @StateObject private var speechManager = SpeechManager()
    let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {

        NavigationStack {
            ZStack {
                Color.black.ignoresSafeArea()

                GeometryReader { geo in

                    // 左上：開始ボタン
                    Button(isRunning || isCountingDown || isPaused ? "計測中" : "開始") {
                        if !isPaused {   // ★一時停止中は開始させない
                            startPressed()
                        }
                    }
                        .font(.system(size: geo.size.height * 0.05))
                        .padding()
                        .background(
                            (isRunning || isCountingDown || isPaused)
                                ? Color.gray.opacity(0.3)
                                : Color.blue
                        )
                        .foregroundColor(
                            (isRunning || isCountingDown || isPaused)
                                ? Color.white.opacity(0.4)
                                : Color.white
                        )
                        .cornerRadius(10)
                        .disabled(isRunning || isCountingDown || isPaused)
                        .position(x: geo.size.width * 0.15,
                                  y: geo.size.height * 0.15)

                    // 右上：設定ボタン
                    NavigationLink("設定", destination: SettingsView())
                        .font(.system(size: geo.size.height * 0.05))
                        .padding()
                        .background(Color.gray.opacity(0.4))
                        .foregroundColor(.white)
                        .cornerRadius(10)
                        .position(x: geo.size.width * 0.85,
                                  y: geo.size.height * 0.15)

                    // 左下：終了ボタン（目立つ赤色）
                    Button("終了") {
                        if skipConfirm {
                            stopPressed()   // ← 即終了
                        } else {
                            showStopAlert = true
                        }
                    }
                        .font(.system(size: geo.size.height * 0.05))
                        .padding()
                        .background(Color.red.opacity(0.8))
                        .foregroundColor(.white)
                        .cornerRadius(10)
                        .position(x: geo.size.width * 0.15,
                                  y: geo.size.height * 0.85)
                                  
                    // 下部中央：一時停止／再開ボタン
                    if isRunning || isPaused {
                        Button(isPaused ? "再開" : "一時停止") {
                            if isPaused {
                                // 再開は即実行
                                pauseResumePressed()
                            } else {
                                // 一時停止は YES/NO を出す
                                showPauseConfirm = true
                            }
                        }
                        .font(.system(size: geo.size.height * 0.05))
                        .padding()
                        .background(
                            isPaused
                                ? Color.green.opacity(0.8)   // ★再開＝緑
                                : Color.yellow.opacity(0.8)  // ★一時停止＝黄色
                        )
                        .foregroundColor(.black)
                        .cornerRadius(10)
                        .position(x: geo.size.width * 0.50,
                                  y: geo.size.height * 0.85)
                        .disabled(!isRunning && !isPaused)
                    }
                    // 右下：仕様ボタン
                    NavigationLink("仕様", destination: SpecView())
                        .font(.system(size: geo.size.height * 0.05))
                        .padding()
                        .background(Color.gray.opacity(0.4))
                        .foregroundColor(.white)
                        .cornerRadius(10)
                        .position(x: geo.size.width * 0.85,
                                  y: geo.size.height * 0.85)

                    // 中央：巨大タイマー
                    VStack(spacing: 40) {
                        // カウントダウン表示の高さを固定
                        ZStack {
                            if isCountingDown {
                                Text("開始まで: \(countdown)")
                                    .font(.system(size: geo.size.height * 0.08))
                                    .foregroundColor(.red)
                                    .offset(y: geo.size.height * 0.12)   // ← ★ここで 1 行下げる
                            }
                        }
                        .frame(height: geo.size.height * 0.06)   // ← ★高さ固定
                        
                        Text(timeString(seconds))
                            .font(.system(size: geo.size.height * 0.50 * (CGFloat(timerFontSize) / 100)))
                            .monospacedDigit() // ★秒数変化による左右ガタつき防止
                            .foregroundColor(timerColor)
                            .bold()
                            .minimumScaleFactor(0.5)
                            .lineLimit(1)
                            .id(timerFontSize)
                    }
                    .offset(y: -geo.size.height * 0.08)   // ← ★ここで 1 行分上に移動
                    .allowsHitTesting(false)   // ← これでボタンが押せるようになる
                    .frame(width: geo.size.width, height: geo.size.height)
                }
            }
            .onReceive(timer) { _ in tick() }
            // タイマー表示時は「横」
            .onAppear {rotationLock.lockLandscape()}
            // バー類の完全非表示
            .toolbar(.hidden, for: .navigationBar)
            .statusBarHidden(true)
            .persistentSystemOverlays(.hidden)

            // 終了確認ダイアログ
            .alert("終了しますか？", isPresented: $showStopAlert) {
                Button("終了する", role: .destructive) { stopPressed() }
                Button("キャンセル", role: .cancel) {}
            }
            .alert("一時停止しますか？", isPresented: $showPauseConfirm) {
                Button("停止する", role: .destructive) { pauseResumePressed() }
                Button("キャンセル", role: .cancel) {}
            }
        }
    }

    func startPressed() {
        seconds = 0
        timerColor = .white
        startTime = nil

        isCountingDown = true
        // ★固定値 5 → 設定値 countdownSec
        countdown = countdownSec

        speakTwice("準備が整ったようですので開始します", "")
    }

    func stopPressed() {
        UIApplication.shared.isIdleTimerDisabled = false
        isRunning = false
        isCountingDown = false
        isPaused = false
        timerColor = .green
    }

    func tick() {
        if isPaused { return }   // ★追加：一時停止中は止める

        if isCountingDown {
            if countdown == 1 {
                speakTwice("よーーい", "はじめ")
            }
            if countdown == 0 {
                isCountingDown = false
                startTimer()
                return
            }
            countdown -= 1
            return
        }

        if isRunning, let start = startTime {
            let elapsed = Int(Date().timeIntervalSince(start))
            seconds = elapsed
            checkMessages()
        }
    }

    func startTimer() {
        UIApplication.shared.isIdleTimerDisabled = true
        startTime = Date()
        seconds = 0
        isRunning = true
        timerColor = .white
    }

    func timeString(_ sec: Int) -> String {
        let m = sec / 60
        let s = sec % 60
        return String(format: "%02d:%02d", m, s)
    }

    func checkMessages() {
        let finishSec = finishMin * 60

        // ★終了時間に応じた残り5分の設定
        let remain5Sec: Int
        let remain5Msg1: String

        switch finishMin {
        case 40:
            remain5Sec = 2100   // 35分
            remain5Msg1 = "35分経過"
        case 35:
            remain5Sec = 1800   // 30分
            remain5Msg1 = "30分経過"
        case 30:
            remain5Sec = 1500   // 25分
            remain5Msg1 = "25分経過"
        default:
            remain5Sec = 2100
            remain5Msg1 = "35分経過"
        }
        var messages: [Int: (String, String)] = [
            minA: (msgA1, msgA2),
            minB: (msgB1, msgB2),

            // ★残り5分（モードごとに変化）
            remain5Sec: (remain5Msg1, "残り5分です"),

            finishSec - 240: ("残り4分", "残り4分です"),
            finishSec - 180: ("残り3分", "残り3分です"),
            finishSec - 120: ("残り2分", "残り2分です"),
            finishSec - 60: ("残り1分", "残り1分です"),

            finishSec - 30: ("残り30秒", "残り30秒です"),
            finishSec - 20: ("残り20秒", "残り20秒です"),
            finishSec - 10: ("残り10秒", "残り10秒です"),

            finishSec: ("終了", "終了です"),
            3600: ("60分経過しました。終了します。", "お疲れさまでした")
        ]
        // ★minC は「40分モード」
        if finishMin == 40 {
            messages[minC] = (msgC1, msgC2)
        }

        if seconds == finishSec {
            timerColor = .red
        }

        if let (a, b) = messages[seconds] {
            speakTwice(a, b)
        }

        if seconds >= 3600 {
            stopPressed()
        }
    }

    func speakTwice(_ first: String, _ second: String) {
        // ★音量0なら完全ミュート
        if voiceVolume == 0 { return }
        let volume = Float(voiceVolume) / 100.0
        speechManager.speakTwice(first, second,
                                 volume: volume)
    }
    func pauseResumePressed() {
        if isPaused {
            // ▼ 再開
            if let pausedAt = pausedAt, let start = startTime {
                let pausedDuration = Date().timeIntervalSince(pausedAt)
                startTime = start.addingTimeInterval(pausedDuration)
            }
            isPaused = false
            pausedAt = nil
            isRunning = true

            timerColor = .white   // ★再開 → 白に戻す

        } else {
            // ▼ 一時停止
            pausedAt = Date()
            isPaused = true
            isRunning = false

            timerColor = .orange  // ★一時停止 → オレンジ
        }
    }
}

// MARK: - 設定画面（縦固定）

struct SettingsView: View {

    @Environment(\.rotationLock) var rotationLock
    @Environment(\.dismiss) private var dismiss
    
    @AppStorage("minA") private var minA = 600
    @AppStorage("msgA1") private var msgA1 = "10分経過"
    @AppStorage("msgA2") private var msgA2 = "10分経過です"

    @AppStorage("minB") private var minB = 1200
    @AppStorage("msgB1") private var msgB1 = "20分経過"
    @AppStorage("msgB2") private var msgB2 = "20分経過です"

    @AppStorage("minC") private var minC = 1800
    @AppStorage("msgC1") private var msgC1 = "30分経過"
    @AppStorage("msgC2") private var msgC2 = "30分経過です"
    
    @AppStorage("countdownSec") private var countdownSec = 5
    @AppStorage("timerFontSize") private var timerFontSize = 100
    @AppStorage("skipConfirm") private var skipConfirm = false   // ← ★追加
    @AppStorage("voiceVolume") private var voiceVolume = 100   // 0〜150
    // ★追加：終了時間選択
    @AppStorage("finishMin") private var finishMin = 40
    @State private var showResetAlert = false

    var body: some View {

        ScrollView {
            VStack(alignment: .leading, spacing: 8) {   // ← 24 → 8 に縮める

                // フォント倍率
                VStack(alignment: .leading, spacing: 4) {
                    Text("フォント倍率")
                        .foregroundColor(.white)
                        .font(.title3)

                    HStack(spacing: 8) {
                        Slider(
                            value: Binding(
                                get: { Double(timerFontSize) },
                                set: { timerFontSize = Int($0) }
                            ),
                            in: 50...150
                        )

                        Text(String(format: "倍率：%.2f", Double(timerFontSize) / 100.0))
                            .foregroundColor(.white)
                            .font(.title2)
                            .frame(width: 110, alignment: .leading)
                    }
                }

                // 終了時間選択
                VStack(alignment: .leading, spacing: 4) {
                    Text("終了時間　40分　35分　30分")
                        .foregroundColor(.white)
                        .font(.title3)

                    Picker("終了時間", selection: $finishMin) {
                        Text("40分").tag(40)
                        Text("35分").tag(35)
                        Text("30分").tag(30)
                    }
                    .pickerStyle(.segmented)
                    .tint(.white)
                    .background(
                        RoundedRectangle(cornerRadius: 6)
                            .fill(Color.gray.opacity(0.25))
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 6)
                            .stroke(Color.gray.opacity(0.7), lineWidth: 1.2)
                    )
                }

                // カウントダウン秒数
                VStack(alignment: .leading, spacing: 4) {
                    Text("カウントダウン秒数")
                        .foregroundColor(.white)
                        .font(.title3)

                    TextField("秒数", value: $countdownSec, format: .number)
                        .keyboardType(.numberPad)
                        .textFieldStyle(.roundedBorder)
                }

                // 経過時間タイトル
                Text("経過時間(秒) メッセージ1 メッセージ2")
                    .foregroundColor(.white)
                    .font(.system(size: 18))
                    .padding(.bottom, 4)   // ← 10 → 4 に縮める

                // A
                VStack(alignment: .leading, spacing: 4) {
                    TextField("秒数", value: $minA, format: .number)
                        .keyboardType(.numberPad)
                        .textFieldStyle(.roundedBorder)

                    TextField("メッセージ1", text: $msgA1)
                        .textFieldStyle(.roundedBorder)

                    TextField("メッセージ2", text: $msgA2)
                        .textFieldStyle(.roundedBorder)
                }

                // B
                VStack(alignment: .leading, spacing: 4) {
                    TextField("秒数", value: $minB, format: .number)
                        .keyboardType(.numberPad)
                        .textFieldStyle(.roundedBorder)

                    TextField("メッセージ1", text: $msgB1)
                        .textFieldStyle(.roundedBorder)

                    TextField("メッセージ2", text: $msgB2)
                        .textFieldStyle(.roundedBorder)
                }

                // C
                VStack(alignment: .leading, spacing: 4) {
                    TextField("秒数", value: $minC, format: .number)
                        .keyboardType(.numberPad)
                        .textFieldStyle(.roundedBorder)

                    TextField("メッセージ1", text: $msgC1)
                        .textFieldStyle(.roundedBorder)

                    TextField("メッセージ2", text: $msgC2)
                        .textFieldStyle(.roundedBorder)
                }

                // ★ここから音声設定（追加する場所はここ！）
                VStack(alignment: .leading, spacing: 4) {
                    Text("音量")
                        .foregroundColor(.white)
                        .font(.title3)

                    HStack(spacing: 8) {
                        Slider(
                            value: Binding(
                                get: { Double(voiceVolume) },
                                set: { voiceVolume = Int($0) }
                            ),
                            in: 0...150
                        )
                        Text("\(voiceVolume)")
                            .foregroundColor(.white)
                            .frame(width: 50)
                    }
                }
                // ★ここにテスト再生ボタンを追加（最もわかりやすい位置）
                Button(action: {
                    let volume = Float(voiceVolume) / 100.0
                    let tester = SpeechManager()
                    tester.speakTwice(msgA1, "",
                                      volume: volume)
                }) {
                    Text("テスト再生")
                        .font(.title3)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(Color.blue.opacity(0.7))
                        .foregroundColor(.white)
                        .cornerRadius(8)
                }
                .padding(.top, 8)

                Toggle("終了時の確認メッセージを省略", isOn: $skipConfirm)
                    .foregroundColor(.white)


                // 保存ボタン
                Button(action: saveSettings) {
                    Text("保存")
                        .font(.title2)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)   // ← 詰める
                        .background(Color.gray.opacity(0.7))
                        .foregroundColor(.white)
                        .cornerRadius(8)
                }
                .padding(.top, 12)

                // 初期値に戻す
                Button(action: { showResetAlert = true }) {
                    Text("初期値に戻す")
                        .font(.title2)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 10)
                        .background(Color.gray.opacity(0.7))
                        .foregroundColor(.white)
                        .cornerRadius(8)
                }
                .alert("初期値に戻しますか？", isPresented: $showResetAlert) {
                    Button("戻す", role: .destructive) { resetSettings() }
                    Button("キャンセル", role: .cancel) {}
                }

                Spacer().frame(height: 40)   // ← 80 → 40 に縮める
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
        }
        .background(Color.black)
        .navigationTitle("設定")
        .toolbarColorScheme(.dark, for: .navigationBar)
          .onAppear { rotationLock.lockPortrait() }      // 設定画面を開いたら「縦」
    }

    func saveSettings() {
        dismiss()
    }

    func resetSettings() {
        minA = 600
        msgA1 = "10分経過"
        msgA2 = "10分経過です"

        minB = 1200
        msgB1 = "20分経過"
        msgB2 = "20分経過です"

        minC = 1800
        msgC1 = "30分経過"
        msgC2 = "30分経過です"
        countdownSec = 5
        finishMin = 40
        skipConfirm = false
        voiceVolume = 100
    }
}

// MARK: - 仕様画面（縦固定）

struct SpecView: View {

    @Environment(\.rotationLock) var rotationLock

    private let specText = """
【追加機能】
・フォントを指定可能
・カウントダウン秒数を指定可能
・音声の ON/OFF が可能
・終了時間の選択（40/35/30）
【開始】
・準備が整ったようですので開始します
 よーい はじめ

【経過時間（設定で変更）】
・設定した10分 10分経過／10分経過です
・設定した20分 20分経過／20分経過です
・設定した30分 30分経過／30分経過です

【残り時間 （終了時間に応じて変動）】
・35分（残り5分）35分経過／残り5分です
・36分（残り4分） 残り4分／残り4分です
・37分（残り3分） 残り3分／残り3分です
・38分（残り2分） 残り2分／残り2分です
・39分（残り1分） 残り1分／残り1分です
・残り30秒 残り30秒／残り30秒です
・残り20秒 残り20秒／残り20秒です
・残り10秒 残り10秒／残り10秒です

【終了】
・40分 終了／終了です（終了時間に応じて変動）
・60分 60分経過しました。終了します／お疲れさまでした

----------------------------------------
配布は自由です。
問題やご要望がございましたら、
sobatimerapp@gmail.com へ連絡ください。
"""

    var body: some View {

        ScrollView {
            Text(specText)
                .foregroundColor(.white)
                .font(.system(size: 18))
                .padding(24)
        }
        .background(Color.black)
        .navigationTitle("仕様")
        .toolbarColorScheme(.dark, for: .navigationBar)
          .onAppear { rotationLock.lockPortrait() }      // 仕様画面を開いたら「縦」
    }
}

// MARK: - 音声管理ヘルパー
@MainActor
class SpeechManager: NSObject, ObservableObject, AVSpeechSynthesizerDelegate {
    private let synthesizer = AVSpeechSynthesizer()
    
    override init() {
        super.init()
        synthesizer.delegate = self
    }
    
    func speakTwice(_ first: String,
                    _ second: String,
                    volume: Float) {

        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .default, options: [.duckOthers])
            try session.setActive(true)
        } catch {
            print("オーディオセッションの設定に失敗しました: \(error)")
        }

        let u1 = AVSpeechUtterance(string: first)
        u1.voice = AVSpeechSynthesisVoice(language: "ja-JP")

        // ★安全な範囲にクリップ
        u1.volume = max(0.0, min(volume, 1.5))
        synthesizer.speak(u1)

        if !second.isEmpty {
            let u2 = AVSpeechUtterance(string: second)
            u2.voice = AVSpeechSynthesisVoice(language: "ja-JP")
            u2.volume = u1.volume
            u2.postUtteranceDelay = 0.4
            synthesizer.speak(u2)
        }
    }

    
}
