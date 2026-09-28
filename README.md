# Luma for Android

Luma is an original offline focus timer and productivity app built from scratch. It combines a local HTML interface with native Java timing, notifications, reminders, audio, and optional Accessibility-based app blocking. It has no external application libraries, server, analytics, or Internet permission.

Version **1.7** introduces a Luma launcher mark: a progress arc around the letter L with a small light accent. Adaptive foreground and background layers let Android apply the launcher's shape, while a monochrome layer supports themed icons. The notification icon uses the same mark. Version 1.6's interaction haptics, streamlined controls, and compact Settings layout remain in place, along with the fixed Timer layout and finish-time placement. Running countdowns request an Android Live Update on supported Android 16 or later devices; the existing ongoing notification remains available when the system or manufacturer does not promote it. Version 1.0 data and backups remain compatible.

## Files

- `app/src/main/assets/`: touch interface and browser preview adapter.
- `app/src/main/assets/expressive.css`: shared color, shape, type, and motion rules.
- `design/luma-icon.svg`: editable master for the launcher icon mark.
- `app/src/main/res/mipmap-anydpi-v26/` and `res/drawable/`: adaptive, themed, legacy, and notification icons.
- `INTER-OFL.txt`: license for the bundled Inter font.
- `ROBOTO-FLEX-OFL.txt`: license for the bundled Roboto Flex clock digits.
- `app/src/main/java/com/focus/personal/`: native app, timer engine, and Android integrations.
- `app/src/main/AndroidManifest.xml` and `res/`: Android manifest and resources.
- `tests/`: deterministic timer and data model checks.
- `build.ps1`: compile, test, package, align, sign, and verify with official Android SDK tools.
- `ARCHITECTURE.md`: component boundaries and verification limits.

The Android package ID remains `com.focus.personal` to preserve the existing installation identity and local app data.

## Build on Windows

Requirements: JDK 21, Android platform API 35, Android Build Tools 35.0.0, Node.js for model checks, and PowerShell. These are development requirements. The phone only needs the APK.

In the prepared workspace, the source folder is `workspace/outputs/luma`. Temporary build output and the local signing key live in `workspace/work`. The APK is written to `workspace/outputs/Luma.apk`.

```powershell
.\build.ps1 -JdkPath 'C:\path\to\jdk-21' -SdkPath 'C:\path\to\Android\Sdk'
```

With the portable toolchain in the workspace, run `outputs\luma\build.ps1` from the workspace root. The build does not require Gradle, dependency downloads, or an account. It compiles against Android 35 and supports Android 8.0 or later. Runtime checks on the target phone are still required.

The build creates a local signing key at `work/signing/luma.jks`. Keep that key to install later builds over this app without uninstalling. It is not included in the source archive. The local key password is defined in the build script. Use your own signing arrangements for distribution.

## Check logic

```powershell
node tests/model.test.cjs
```

`build.ps1` also compiles and runs `tests/TimerEngineTest.java`. Timer checks cover monotonic elapsed time, paused intervals, distractions, single completion, break transitions, long-break cadence, stopwatch duration, early finish, and invalid inputs. Model checks cover recurrence boundaries, task completion, backup validation, and StandBy display policy.

## Browser preview

From the repository root, run:

```powershell
python -m http.server 8765 --bind 127.0.0.1 --directory app/src/main/assets
```

Open `http://127.0.0.1:8765`. The preview stores sample data in browser local storage. The Android app instead uses private native storage and its own monotonic timer engine. Browser checks cannot establish Android notification or background execution behavior.

## Implemented behavior and limits

The interface runs in an Android WebView, while native code owns timing, session history, notifications, reminders, audio, and optional app blocking. Luma keeps user data on the device. See `ARCHITECTURE.md` for component boundaries and device-level checks.
