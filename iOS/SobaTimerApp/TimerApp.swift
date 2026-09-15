//
//  TimerApp.swift
//  SobaTimerApp
//
//  Created by hiroshi on 2026/09/13.
//
import SwiftUI
import UIKit

@main
struct TimerApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(\.rotationLock, RotationLock())
        }
    }
}

// MARK: - 画面回転制御

class AppDelegate: NSObject, UIApplicationDelegate {
    static var orientationLock = UIInterfaceOrientationMask.all

    func application(_ application: UIApplication, supportedInterfaceOrientationsFor window: UIWindow?) -> UIInterfaceOrientationMask {
        return AppDelegate.orientationLock
    }
}

struct RotationLockKey: EnvironmentKey {
    static let defaultValue = RotationLock()
}

extension EnvironmentValues {
    var rotationLock: RotationLock {
        get { self[RotationLockKey.self] }
        set { self[RotationLockKey.self] = newValue }
    }
}

class RotationLock {

    func lockLandscape() {
        AppDelegate.orientationLock = .landscapeRight // または .landscape
        applyOrientation(.landscapeRight, rawValue: UIInterfaceOrientation.landscapeRight.rawValue)
    }

    func lockPortrait() {
        AppDelegate.orientationLock = .portrait
        applyOrientation(.portrait, rawValue: UIInterfaceOrientation.portrait.rawValue)
    }

    func unlockAll() {
        AppDelegate.orientationLock = .all
    }

    private func applyOrientation(_ mask: UIInterfaceOrientationMask, rawValue: Int) {
        // 画面遷移アニメーションとの衝突を防ぐため、1フレーム遅らせて実行
        DispatchQueue.main.async {
            if #available(iOS 16.0, *) {
                guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene else { return }
                
                // 1. 幾何情報の更新をリクエスト
                windowScene.requestGeometryUpdate(.iOS(interfaceOrientations: mask)) { error in
                    print("回転制御エラー: \(error.localizedDescription)")
                }
                
                // 2. 最前面のViewControllerに画面向きの再評価を強制（これが重要）
                windowScene.keyWindow?.rootViewController?.setNeedsUpdateOfSupportedInterfaceOrientations()
            } else {
                UIDevice.current.setValue(rawValue, forKey: "orientation")
                UIViewController.attemptRotationToDeviceOrientation()
            }
        }
    }
}
