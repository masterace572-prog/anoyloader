# Virtual-process stability changes and verification

## What changed

- Bcore reserves tracked virtual-process slots even when Android's running-process snapshot omits them, handles a null snapshot/provider result, and rejects dead Binder clients.
- Process records use the full virtual user UID consistently with their map key. A delayed Binder death no longer removes a newer record, kills a potentially reused OS PID, or deletes a replacement slot's proc metadata.
- Failed initialization releases its gate and removes half-registered records even when attach throws; dead clients are not published after a failed death-recipient registration.
- Both crash handlers stop treating arbitrary security/NPE/classloader/main/render failures as survivable. Returning from an uncaught-exception handler does **not** resume that thread; it can leave a frozen/zombie game.
- Java crash reports are written to bounded private files (up to five, 128 KiB each). The existing crash screen reads the latest report on next launcher startup. The failing process no longer tries to start a crash activity with a large Binder payload.
- Launch no longer force-stops a healthy game or sleeps on the UI thread when returning to the loader.

These are targeted source-level corrections, **not a verified fix for the reported mid-game crash**. No upstream Bcore release was substituted, and no anti-detection guarantees are made. Android app virtualization still uses Android services and APIs; it cannot truthfully be represented as an undetectable non-Android machine.

## Required device validation

This workspace has no JDK, Android SDK/NDK, emulator, or game/device logs, and the allowed network does not include Google's Android/Maven distribution endpoints. Full Android compilation and gameplay validation must run in your existing Android build environment.

Build both branded variants using JDK 17, Android SDK 34, and the NDK versions declared in Gradle. On a test device:

1. Install the APK and supported game/OBB. Record device model, Android/API version, game package + build, loader version, and native-library version.
2. Launch and play through the usual crash point; test at least 20–30 minutes. Background/foreground the game and reopen the loader without deliberately ending the session.
3. Repeat launch after a genuine process death; check that a new virtual process starts and old death notifications do not affect it. Also test a secondary virtual user if supported.
4. Confirm crash-report UI on the next app start after a Java fatal failure; copy and redact the report before sharing. Native SIGSEGV/SIGABRT and low-memory kills do not reliably reach Java handlers.
5. Capture unfiltered logs while reproducing:

   ```bash
   adb logcat -c
   adb logcat -b all -v threadtime > gameplay-crash.txt
   # Reproduce the crash, then stop logcat with Ctrl-C.
   adb shell dumpsys activity exit-info com.ryzen > exit-info.txt
   # For the other flavor use com.ogcheats.
   ```

   `exit-info` availability varies by Android version. For native failures collect a bugreport/tombstone where device permissions allow. Redact license keys, account tokens, HWIDs, and personal data before sharing.

Do not hide failures by restoring blanket exception swallowing. A reproducible stack/tombstone should drive fixes at the failing API/hook/native component instead.
