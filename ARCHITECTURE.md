# Luma

Luma is an offline Android focus and productivity app built from scratch. The local HTML interface runs in a WebView, while native Android code handles timing and device integrations.

## Boundaries

- Local HTML, CSS, and JavaScript render the touch interface. There are no remote pages, third-party scripts, accounts, or Internet permission.
- A small JavaScript bridge sends explicit commands to a native timer engine. Native Android code owns timing, session history, notifications, alarms, audio, and optional app blocking.
- TimerEngine is independent of Android for deterministic JVM checks. Elapsed time uses the monotonic clock. Pausing, app recreation, and Doze do not depend on browser animation timers.
- Task, calendar, and settings data live in private app preferences, separately from native timer and session state. Export and import use Android's document picker.
- `standby.js` is presentation-only and reads the same timer snapshot as the regular screen. Pure model helpers decide automatic entry and position shifts. Its `standbyDisplay` bridge controls immersive system bars, per-window dimming, and keep-awake behavior. Android timing is unchanged. Pausing or completing a session releases keep-awake, and leaving the activity restores brightness. No overlay, global brightness, or lock-screen permissions are needed.
- App blocking is opt-in through Android Accessibility settings. The service observes only foreground package changes, never page text, and returns to Home for explicitly selected apps during a work session. System settings, launchers, phone, and Luma itself are excluded.

## Verification

Run `node tests/model.test.cjs` for date, recurrence, and task logic. The PowerShell build runs JVM checks of the timer before compiling and signing the APK. The interface can be previewed by serving `app/src/main/assets` locally. The browser adapter is a preview fallback and does not provide the phone's background timer.

Device checks are still required for Samsung screen lock and Doze behavior, notification permission, Accessibility access, task reminders, audio focus, and document import and export. Browser previews do not verify Android system integrations.
