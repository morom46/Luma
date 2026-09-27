# Focus Personal

Personal Android recreation based on Engross's public Google Play screenshots, reviewed 27 September 2026. It is independently implemented and does not connect to Engross services.

## Boundaries

- Local HTML/CSS/JavaScript renders the touch interface in an Android WebView. No remote pages, third-party scripts, accounts, or network permission.
- A small JavaScript bridge sends explicit commands to a native timer engine. Native Android code owns timing, session history, notifications, alarms, audio, and optional app blocking.
- TimerEngine is independent of Android for deterministic JVM tests. Elapsed time uses the monotonic clock; pausing, app recreation, and Doze do not depend on browser animation timers.
- Task/calendar data and settings are stored in private app preferences, separately from native timer/session state. Export and import use Android's document picker.
- `standby.js` is presentation-only and reads the same timer snapshot as the regular screen. Pure model helpers decide automatic entry and position shifts. Its `standbyDisplay` bridge controls foreground immersive bars, per-window dimming and keep-awake; Android timing is unchanged. Pausing/completion releases keep-awake, and leaving the activity restores brightness. No overlay, global brightness or lock-screen permissions are needed.
- App blocking is opt-in via Android Accessibility settings. The service observes only foreground package changes, never page text, and returns to Home for explicitly selected apps during a work session. System settings, launchers, phone and the app itself are excluded.

## Verification

Run `node tests/model.test.cjs` for date, recurrence, and task logic. The PowerShell build runs JVM tests of the timer before compiling and signing the APK. The interface can be previewed by serving `app/src/main/assets` locally; the browser adapter is a preview fallback, not the phone's background timer.

Device-level checks still required: Samsung screen-lock/Doze behavior, notification permission, Accessibility access, task-reminder delivery, audio focus, and document import/export. Public screenshots cannot establish pixel-identical parity with screens not published by the original developer.
