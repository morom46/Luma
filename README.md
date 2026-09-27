# Focus Personal — Android

An offline personal recreation of the core Engross workflows for a Samsung Galaxy S24. It combines a local HTML interface with native Java timing, notifications, reminders, audio and optional Accessibility-based app blocking. There are no external application libraries, servers, analytics or internet permission.

Version **1.1** adds true-black AMOLED surfaces and an optional StandBy-style clock/session display. It opens automatically in landscape while a session runs on the Timer screen, or manually in either orientation. Red night colour, fading controls, minute-by-minute position shifts and native per-window dimming are included. It stays awake only while running in the foreground and does not replace Samsung's lock screen or Always On Display. Existing version 1.0 data and backups remain compatible.

## Files

- `app/src/main/assets/`: touch interface and browser-preview adapter.
- `app/src/main/java/com/focus/personal/`: native app, timer engine and Android integrations.
- `app/src/main/AndroidManifest.xml`, `res/`: Android manifest and resources.
- `tests/`: deterministic timer and data-model checks.
- `build.ps1`: compile, test, package, align, sign and verify with official Android SDK tools.
- `ARCHITECTURE.md`: component boundaries and verification limits.

## Build on Windows

Requirements: JDK 21, Android platform API 35, Android Build-Tools 35.0.0, Node.js for model tests, PowerShell. These are development requirements; the phone only needs the APK.

The delivered folder layout is `workspace/outputs/engross-personal`. Temporary build output and the personal signing key live in `workspace/work`; the APK is written to `workspace/outputs/Focus-Personal.apk`.

```powershell
.\build.ps1 -JdkPath 'C:\path\to\jdk-21' -SdkPath 'C:\path\to\Android\Sdk'
```

With the portable toolchain prepared in this workspace, simply run `outputs\engross-personal\build.ps1` from the workspace. The build needs no Gradle, dependency download or account. It compiles against Android 35 and supports Android 8.0 or later, including the Android versions supported by the S24. Runtime verification on the actual phone is still required.

The build generates a local personal-use signing key at `work/signing/focus-personal.jks`. Keep that file if you want later APK updates to install over this build without uninstalling. It is not included in the source ZIP. The local key password is defined in the build script; use your own signing arrangements for distribution.

## Check logic

```powershell
node tests/model.test.cjs
```

`build.ps1` also compiles and runs `tests/TimerEngineTest.java`. Timer tests cover monotonic elapsed time, paused intervals, distractions, completion exactly once, break transitions, long-break cadence, stopwatch duration, early finish and invalid inputs. Model tests cover recurrence boundaries, task completion, backup validation and StandBy display policy.

## Browser preview

```powershell
python -m http.server 8765 --bind 127.0.0.1 --directory app/src/main/assets
```

Open `http://127.0.0.1:8765`. The preview stores test data in browser local storage. The Android app instead uses private native storage and its own monotonic timer engine. Browser checks cannot establish Android notification, background-execution or Accessibility behavior.

## Implemented behavior and limits

See the accompanying `INSTALL-ON-GALAXY-S24.md` for installation, features, limitations and phone checks. This is independent code and is not affiliated with Engross Apps. The original reference is [Engross's public Play Store page](https://play.google.com/store/apps/details?id=com.engross).
