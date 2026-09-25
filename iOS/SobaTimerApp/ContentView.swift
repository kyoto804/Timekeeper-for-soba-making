//
//  ContentView.swift
//  SobaTimerApp
//
//  Created by hiroshi on 2026/09/13.
//  Updated on 2026/09/25
//
//  【追加機能】
//  ・終了時間（40 / 35 / 30）の選択機能を追加
//
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
    @AppStorage("voiceEnabled") private var voiceEnabled = true
    @AppStorage("timerFontSize") private var timerFontSize = 100
    // ★追加：終了時間（40 / 35 / 30）
    @AppStorage("finishMin") private var finishMin = 40
        
    @StateObject private var speechManager = SpeechManager()
    let timer = Timer.publish(every: 1, on: .main, in: .common).autoconnect()

    var body: some View {

        NavigationStack {
            ZStack {
                Color.black.ignoresSafeArea()

                GeometryReader { geo in

                    // 左上：開始ボタン
                    Button(isRunning || isCountingDown ? "計測中" : "開始") { startPressed() }
                        .font(.system(size: geo.size.height * 0.05))
                        .padding()
                        .background(
                            (isRunning || isCountingDown)
                                ? Color.gray.opacity(0.3)
                                : Color.blue
                        )
                        .foregroundColor(
                            (isRunning || isCountingDown)
                                ? Color.white.opacity(0.4)
                                : Color.white
                        )
                        .cornerRadius(10)
                        .disabled(isRunning || isCountingDown)
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
                    Button("終了") { showStopAlert = true }
                        .font(.system(size: geo.size.height * 0.05))
                        .padding()
                        .background(Color.red.opacity(0.8))
                        .foregroundColor(.white)
                        .cornerRadius(10)
                        .position(x: geo.size.width * 0.15,
                                  y: geo.size.height * 0.85)

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

                        if isCountingDown {
                            Text("開始まで: \(countdown)")
                                .font(.system(size: geo.size.height * 0.06))
                                .foregroundColor(.red)
                        }

                        Text(timeString(seconds))
                            .font(.system(size: geo.size.height * 0.50 * (CGFloat(timerFontSize) / 100)))
                            .monospacedDigit() // ★秒数変化による左右ガタつき防止
                            .foregroundColor(timerColor)
                            .bold()
                            .minimumScaleFactor(0.5)
                            .lineLimit(1)
                            .id(timerFontSize)
                    }
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

        timerColor = .green
    }

    func tick() {

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

        // ★minC は 40分モードのときだけ有効
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
        // ★音声OFFなら何も喋らない
        if !voiceEnabled { return }

        speechManager.speakTwice(first, second)
    }
}

// MARK: - 設定画面（縦固定）

struct SettingsView: View {

    @Environment(\.rotationLock) var rotationLock

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
    @AppStorage("voiceEnabled") private var voiceEnabled = true
    @AppStorage("timerFontSize") private var timerFontSize = 100
    // ★追加：終了時間選択
    @AppStorage("finishMin") private var finishMin = 40
    @State private var showResetAlert = false

    var body: some View {

        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                Group {
                    Text("フォント倍率")
                        .foregroundColor(.white)
                        .font(.title3)

                    HStack {
                        Slider(
                            value: Binding(
                                get: { Double(timerFontSize) },
                                set: { timerFontSize = Int($0) }
                            ),
                            in: 50...150
                        )

                        // ★フォント倍率を横に表示（0.50〜1.50）
                        Text(String(format: "倍率：%.2f", Double(timerFontSize) / 100.0))
                            .foregroundColor(.white)
                            .font(.title2)
                            .frame(width: 120, alignment: .leading)
                    }
                }
                // ★終了時間選択
                Group {
                    Text("終了時間　40分　35分　30分")
                        .foregroundColor(.white)
                        .font(.title3)

                    Picker("終了時間", selection: $finishMin) {
                        Text("40分").tag(40)
                        Text("35分").tag(35)
                        Text("30分").tag(30)
                    }
                    .pickerStyle(.segmented)
                    .background(
                        RoundedRectangle(cornerRadius: 6)
                            .fill(Color.gray.opacity(0.25))   // ← ★黒からグレーへ
                    )
                    .tint(.white)   // 選択中タブは黄色
                    .overlay(
                        RoundedRectangle(cornerRadius: 6)
                            .stroke(Color.gray.opacity(0.7), lineWidth: 1.4)
                    )
                    .background(
                        RoundedRectangle(cornerRadius: 6)
                            .fill(Color.gray.opacity(0.25))   // ← ★未選択タブの背景を薄いグレーに
                    )
                }

                Group {
                    Text("カウントダウン秒数")
                        .foregroundColor(.white)
                        .font(.title3)

                    TextField("秒数", value: $countdownSec, format: .number)
                        .keyboardType(.numberPad)
                        .textFieldStyle(.roundedBorder)
                }

                Text("経過時間(秒) メッセージ1 メッセージ2")
                    .foregroundColor(.white)
                    .font(.system(size: 18))
                    .padding(.bottom, 10)

                Group {
                    TextField("秒数", value: $minA, format: .number)
                        .keyboardType(.numberPad)
                        .textFieldStyle(.roundedBorder)

                    TextField("メッセージ1", text: $msgA1)
                        .textFieldStyle(.roundedBorder)

                    TextField("メッセージ2", text: $msgA2)
                        .textFieldStyle(.roundedBorder)
                }

                Group {
                    TextField("秒数", value: $minB, format: .number)
                        .keyboardType(.numberPad)
                        .textFieldStyle(.roundedBorder)

                    TextField("メッセージ1", text: $msgB1)
                        .textFieldStyle(.roundedBorder)

                    TextField("メッセージ2", text: $msgB2)
                        .textFieldStyle(.roundedBorder)
                }

                Group {
                    TextField("秒数", value: $minC, format: .number)
                        .keyboardType(.numberPad)
                        .textFieldStyle(.roundedBorder)

                    TextField("メッセージ1", text: $msgC1)
                        .textFieldStyle(.roundedBorder)

                    TextField("メッセージ2", text: $msgC2)
                        .textFieldStyle(.roundedBorder)
                }
                Group {
                    Toggle("音声ガイドを有効にする", isOn: $voiceEnabled)
                        .foregroundColor(.white)
                }

                Button(action: saveSettings) {
                    Text("保存")
                        .font(.title2)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.gray.opacity(0.7))
                        .foregroundColor(.white)
                        .cornerRadius(8)
                }
                .padding(.top, 20)

                Button(action: { showResetAlert = true }) {
                    Text("初期値に戻す")
                        .font(.title2)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.gray.opacity(0.7))
                        .foregroundColor(.white)
                        .cornerRadius(8)
                }
                .alert("初期値に戻しますか？", isPresented: $showResetAlert) {
                    Button("戻す", role: .destructive) { resetSettings() }
                    Button("キャンセル", role: .cancel) {}
                }

                Spacer().frame(height: 80)
            }
            .padding(24)
        }
        .background(Color.black)
        .navigationTitle("設定")
        .toolbarColorScheme(.dark, for: .navigationBar)
          .onAppear { rotationLock.lockPortrait() }      // 設定画面を開いたら「縦」
    }

    func saveSettings() {}

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
        voiceEnabled = true
        finishMin = 40
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
    
    func speakTwice(_ first: String, _ second: String) {
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .default, options: [.duckOthers])
            try session.setActive(true)
        } catch {
            print("オーディオセッションの設定に失敗しました: \(error)")
        }
        
        let utterance1 = AVSpeechUtterance(string: first)
        utterance1.voice = AVSpeechSynthesisVoice(language: "ja-JP")
        synthesizer.speak(utterance1)
        
        if !second.isEmpty {
            let utterance2 = AVSpeechUtterance(string: second)
            utterance2.voice = AVSpeechSynthesisVoice(language: "ja-JP")
            utterance2.postUtteranceDelay = 0.4
            synthesizer.speak(utterance2)
        }
    }
}
