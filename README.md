# FM-DX Android App

FM-DX App is a native Android client for FM-DX radio tuners. It connects **two ways**:

1. **Remote server** — to an [`fm-dx-webserver`](https://github.com/NoobishSVK/fm-dx-webserver)
   instance over the network (control + audio + spectrum), the same experience as the console and
   Electron clients.
2. **Direct / USB** — to a **TEF668X Headless USB Tuner**
   ([FM-DX Tuner](https://github.com/kkonradpl/FM-DX-Tuner) firmware) plugged straight into the
   phone with a USB-C/OTG cable: tuning and RDS over the tuner's CDC-ACM serial port, and audio from
   the tuner's own USB Audio Class output.

The app is written in Kotlin with Jetpack Compose Material 3, Media3/ExoPlayer, and structured
coroutines. It targets Android 10 (API 29) and newer.

## Features

- **Remote or Direct connection** — pick "Remote server" or "Direct / USB" on the Connection
  screen; the choice and the last-used frequency are remembered.
- Tune with 10 kHz resolution via the frequency wheel; toggle **CEQ**, **IMS**, and **stereo/mono**.
- Real-time signal levels (dBf, dBµV, dBm), stereo pilot, and RDS — PS, PI, RadioText, PTY, TP/TA,
  MS and the decoder-identification (DI) flags.
- Stream audio: from the server's `/audio` WebSocket (Remote), or captured from the tuner's USB
  audio interface and kept playing with the screen off (Direct).
- Spectrum visualisation and scanning, station logos, transmitter metadata, connected-user count
  and live latency — **Remote (fm-dx-webserver) only**.
- Mirror tuner telemetry to the TEF Logger app via the built-in UDP pass-through option.
- Browse the curated public FM-DX server list from `servers.fmdx.org`.

## Direct USB tuner

- Hardware: a **TEF668X Headless USB Tuner** (USB VID `0x1209` / PID `0x6687`) — the FM-DX Tuner
  firmware build that exposes a composite **CDC-ACM serial + USB Audio** device. Other USB-serial
  devices are intentionally ignored.
- Connect it to the phone with a USB-C / OTG adapter, open **Connection → Direct / USB**, and tap
  **Connect USB tuner**. The card shows live "detected / not detected" status.
- Approve the **USB permission** prompt, and the **microphone** prompt the first time you play audio
  (Android gates USB-audio capture behind `RECORD_AUDIO`; it is the tuner's line audio, not the
  built-in mic).
- Control uses the XDR/xdrd line protocol directly over serial; no `fm-dx-webserver` or `xdrd`
  daemon is involved. Server-only features (spectrum, logos, transmitter database, public-server
  list, server diagnostics) are hidden in this mode.

## Remote server prerequisites

The Remote mode talks to an [`fm-dx-webserver`](https://github.com/NoobishSVK/fm-dx-webserver). For
the full experience (logos and spectrum) the host should have:

- [Spectrum Graph plugin](https://github.com/AmateurAudioDude/FM-DX-Webserver-Plugin-Spectrum-Graph)
- [Station logo plugin](https://github.com/Highpoint2000/webserver-station-logos)

Without them the spectrum tab and station artwork fall back to placeholder content.

## Build requirements

- JDK 21 (matching the module's Java toolchain).
- Android SDK with `platforms;android-37`, `platform-tools`, and `build-tools` (the module compiles
  against API 37; `build-tools;36.1.0` is the currently used package).
- `ANDROID_HOME` or `ANDROID_SDK_ROOT` pointing at the SDK.
- A device or emulator on Android 10+ (API 29+). Note: USB host (OTG) is required to use a direct
  USB tuner — the Android **emulator cannot pass through USB**, so the Direct mode can only be
  exercised on physical hardware.

Install missing SDK packages with `sdkmanager`:

```bash
sdkmanager --sdk_root="$ANDROID_SDK_ROOT" "platforms;android-37" "platform-tools" "build-tools;36.1.0"
```

## Command-line workflow

1. Prime the Gradle wrapper: `./gradlew --version`
2. Assemble the debug APK: `./gradlew :fm-dx-app:assembleDebug`
   (output: `android/fm-dx-app/build/outputs/apk/debug/fm-dx-app-debug.apk`)
3. Install to a device/emulator: `./gradlew :fm-dx-app:installDebug` then
   `adb shell am start -n org.fmdx.app/.MainActivity`
4. Stop daemons on cache errors: `./gradlew --stop`

## Android Studio

1. **File → Open…** the repository root; Studio detects the `fm-dx-app` module at `android/fm-dx-app`.
2. Let Gradle sync against the API 37 SDK.
3. Pick a device on Android 10+ and press **Run**.

## Testing

- JVM unit tests: `./gradlew :fm-dx-app:test`
  (reports under `android/fm-dx-app/build/reports/tests/testDebugUnitTest/index.html`).
- Instrumentation tests (device/emulator): `./gradlew connectedDebugAndroidTest`.

## Docker / CI

For containerised builds, `docker-build.sh` documents the Debian workflow: install JDK 21, the
Android command-line tools and the API 37 SDK components, then
`./gradlew :fm-dx-app:assembleDebug`. Persist the SDK directory between runs.

## Troubleshooting

- **Remote**: ensure the host endpoint negotiates TLS 1.3 (HTTPS hosts); HTTP/cleartext hosts are
  supported for local servers.
- **Direct / USB — "No USB tuner detected"**: reseat the OTG cable and confirm it is a TEF668X
  Headless tuner (VID `0x1209` / PID `0x6687`). The phone's single USB-C port can't be cabled to a
  computer and the tuner at the same time — use wireless debugging while testing the tuner.
- **Direct / USB — no sound**: grant the microphone permission.
- Prefer `./gradlew --stop` over clearing Gradle caches when daemons misbehave.
