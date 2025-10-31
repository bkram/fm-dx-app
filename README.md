# FMDX Android App

FMDX is the native Android companion for `fm-dx-webserver`, delivering the same control, monitoring,
and spectrum visualisation features that ship with the console and Electron clients. The app is
written in Kotlin with Jetpack Compose Material 3, ExoPlayer, and structured coroutines, and it
requires Android 10 (API 29) or newer so every connection can negotiate TLS 1.3 without bundling a
custom security provider.

## Overview
- Configure and persist the server URL directly from the UI.
- Stream audio via the `/audio` WebSocket with ExoPlayer while reading live tuner state.
- Toggle iMS/EQ, cycle antennas, and adjust frequency with 10 kHz resolution.
- Inspect real-time signal levels (dBf, dBµV, dBm), RDS/RadioText, and transmitter metadata.
- Visualise spectrum data from the Spectrum Graph plugin and trigger scans when available.

## Requirements
- JDK 21 (matching the module’s Java toolchain).
- Android SDK preview packages for API 36.1:
  - `platforms;android-36.1`
  - `build-tools;36.1.0`
  - `platform-tools`
- `ANDROID_HOME` or `ANDROID_SDK_ROOT` pointing at the SDK.
- Device or emulator on Android 10+ (API 29+) to guarantee TLS 1.3.

Install missing SDK packages with `sdkmanager`:

```bash
sdkmanager --sdk_root="$ANDROID_SDK_ROOT" \
  "platforms;android-36.1" \
  "build-tools;36.1.0" \
  "platform-tools"
```

## Command-Line Workflow
1. Prime the Gradle wrapper (downloads the configured distribution if absent):
   ```bash
   ./gradlew --version
   ```
2. Assemble the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
   The APK is written to `android/app/build/outputs/apk/debug/app-debug.apk`.
3. (Optional) Install to a connected device or emulator:
   ```bash
   ./gradlew installDebug
   adb shell am start -n org.fmdx.app/.MainActivity
   ```
4. Stop Gradle daemons if cache/daemon errors occur:
   ```bash
   ./gradlew --stop
   ```

## Android Studio
1. Open Android Studio and choose **File → Open…**.
2. Select the repository root (`fm-dx-app`). Studio detects the single `app` module located at
   `android/app`.
3. Let Gradle sync against the API 36.1 SDK.
4. Choose a device running Android 10+ and press **Run**.

## Configuration
- On first launch, enter the `fm-dx-webserver` base URL (e.g. `https://radio-host:8080/`). The app
  normalises the URL and establishes both control and plugin WebSocket connections.
- Audio playback falls back to the WebSocket MP3 stream (`{"type":"fallback","data":"mp3"}`) exposed
  by the server.
- Spectrum scanning mirrors the behaviour of the desktop client and requires the Spectrum Graph
  plugin on the server.

## Testing
- Run JVM unit tests with:
  ```bash
  ./gradlew test
  ```
- Execute instrumentation tests (device/emulator required) with:
  ```bash
  ./gradlew connectedDebugAndroidTest
  ```

## Docker / CI
For containerised builds, `docker-build.sh` documents the Debian-based workflow used in CI. The
script installs JDK 21, the Android command-line tools, the API 36.1 SDK components, and then calls:

```bash
./gradlew --version
./gradlew assembleDebug
```

Persist the SDK directory between runs to avoid repeated downloads.

## Troubleshooting
- Ensure the host TLS endpoint supports TLS 1.3; earlier protocol versions cause the app to abort.
- Verify the preview SDK paths if Gradle cannot locate Android 36.1.
- Clear Gradle caches only as a last resort; prefer `./gradlew --stop` to restart daemons.
