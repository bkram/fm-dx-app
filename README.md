# FMDX Android App

This directory contains a native Android application that offers the same control and monitoring
features as the existing console and Electron clients. The app connects to an fm-dx-webserver
instance, streams audio over the WebSocket interface, displays live tuner state, and renders
spectrum data collected through the Spectrum Graph plugin.

## Features

- Configure the server URL directly from the UI.
- Live updates through the `/text` control WebSocket.
- Audio playback via the `/audio` WebSocket using ExoPlayer.
- iMS/EQ toggles, antenna cycling, and frequency adjustments down to 0.01 MHz.
- Real-time signal meter with selectable units (dBf, dBµV, dBm).
- RDS, RadioText, and transmitter information panels with error highlighting.
- Spectrum view backed by the Spectrum Graph plugin, and on-demand scanning, pending availability.

## Project structure

The Android app is part of a larger Gradle project. The structure is as follows:

```
.
├── android/
│   └── app/             # Android application module
├── build.gradle         # Root build configuration
├── gradle/              # Gradle wrapper configuration
├── gradlew, gradlew.bat # Wrapper scripts
└── settings.gradle      # Gradle settings
```

The Gradle wrapper jar is intentionally excluded from version control. The provided `gradlew`
scripts download the matching wrapper jar automatically by reading the version from
`gradle-wrapper.properties`.

## Building

1. Install the Android SDK (API level 34 or later) and ensure `ANDROID_HOME`/`ANDROID_SDK_ROOT` is
   set.
2. Bootstrap the Gradle wrapper from the project root (skip if Android Studio already synced the
   project):

   ```bash
   ./gradlew --version
   ```

   The first invocation downloads the wrapper jar (if missing) plus the Gradle distribution
   referenced in `gradle-wrapper.properties`.

3. Build the debug APK from the project root:

   ```bash
   ./gradlew assembleDebug
   ```

   The first invocation downloads required Gradle and Android dependencies. The resulting APK is
   located at `android/app/build/outputs/apk/debug/`.

4. To install on a connected device or emulator:

   ```bash
   ./gradlew installDebug
   ```

## Running from Android Studio

1. Open Android Studio and choose **File → Open**.
2. Select the project's root directory (`fmdx-app/app`).
3. Let Gradle sync the project. Once completed you can build and run on any device running Android
   7.0 (API 24) or newer.

## Configuration

- On first launch, enter the fm-dx-webserver address (for example `http://radio-host:8080/`).
- The application automatically normalises the URL and establishes both control and plugin WebSocket
  connections.
- Spectrum scanning requires the fm-dx-webserver Spectrum Graph plugin to be installed, matching the
  behaviour of the existing desktop client.

## Notes

- Audio streaming relies on the WebSocket MP3 fallback (`{"type":"fallback","data":"mp3"}`) provided
  by the server.
- The app throttles command transmission to match the console client's 8 commands per second limit.
- If the server does not expose spectrum data, the graph displays a baseline covering 83–108 MHz
  with zeroed signal levels.

