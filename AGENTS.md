# Android App Agent Guidelines

This file is the agent guide for any coding agent working on this repo (Claude Code at
`claude.ai/code`, Cursor, Copilot, etc.). `CLAUDE.md` is a symlink to this file, so improvements
land in one place. Project README at `README.md` covers user-facing setup and the server
plugins the app expects.

## Common Commands

All commands assume you're at the repository root with `ANDROID_HOME` / `ANDROID_SDK_ROOT`
pointing at an SDK that has `platforms;android-36.1`, `build-tools;36.1.0`, and `platform-tools`
installed.

- `./gradlew :fm-dx-app:assembleDebug` — debug APK at
  `android/fm-dx-app/build/outputs/apk/debug/fm-dx-app-debug.apk`.
- `./gradlew :fm-dx-app:installDebug` — install on the connected device. With multiple devices
  (e.g. emulator + a wireless-debugged phone) build first, then
  `adb -s <serial> install -r android/fm-dx-app/build/outputs/apk/debug/fm-dx-app-debug.apk`.
- `./gradlew :fm-dx-app:test` — JVM unit tests. Reports under
  `android/fm-dx-app/build/reports/tests/testDebugUnitTest/index.html`.
- `./gradlew :fm-dx-app:test --tests "org.fmdx.app.audio.MediaItemBuilderTest.nowPlayingFields_prefersPsForTitleAndStation"` —
  run one test by FQN (use `*` wildcards as needed).
- `./gradlew :fm-dx-app:lintDebug` — Android Lint. Plain-text report at
  `android/fm-dx-app/build/reports/lint-results-debug.txt`.
- `./gradlew --stop` — kill Gradle / Kotlin daemons if the compile cache misbehaves.

## Architecture Overview

The app is a single-activity Compose Material 3 client for `fm-dx-webserver` remote tuners.

- **`FmDxApp` (`org.fmdx.app.FmDxApp`)** is the `Application` subclass. It owns one process-wide
  `FmDxSessionController` accessed via `(application as FmDxApp).sessionController`.
- **`FmDxSessionController` (`data/FmDxSessionController.kt`)** is the single source of truth for
  connection / RDS / freq state. It owns the control WebSocket (via
  `FmDxRepository.connectControl`), the live `TunerState`, station-logo lookup
  (`findStationLogo` in `FmDxRepository.kt` — local `/logos/{ITU}/{PI}.{ext}` first, then the
  central `tef.noobish.eu` directory listing), latency monitor, and `KEY_LAST_SERVER_URL`
  persistence in the `fm_dx_prefs` SharedPreferences. Exposes `state: StateFlow<FmDxSessionState>`.
- **`MainViewModel`** keeps only UI-only state (settings, public-server picker, spectrum + plugin
  socket, audio-playing) and `combine(...)`s it with `sessionController.state` into the public
  `uiState`. Tuning commands forward through `sessionController.sendCommand` /
  `mutateTunerState`.
- **`audio/PlaybackService`** is a `MediaSessionService`. It (1) plays audio via the custom
  `WebSocketMediaSourceFactory` (which maps `MediaItem.mediaId == server base URL` → `ws://...`
  audio stream URL), (2) collects `sessionController.state` and pushes live now-playing metadata
  by calling `player.replaceMediaItem(0, …)` on its own session player (do NOT route metadata
  through a `SessionCommand` from the ViewModel — that path was removed), and (3) bridges
  `Player.Listener.onMediaItemTransition` → `sessionController.connect(url)` so RDS / freq /
  metadata work even when the activity is dead. The system notification uses the monochrome
  silhouette via `DefaultMediaNotificationProvider.setSmallIcon(R.drawable.ic_launcher_monochrome)`.
- **`audio/MediaItemBuilder.kt`** is the single source of truth for now-playing field mapping
  (`nowPlayingFieldsFor` / `mediaMetadataFor` / `buildMediaItemForServer`). Reuse it for any
  new metadata surface; tests live in `MediaItemBuilderTest`.
- **Server plugin tolerance**: the app expects fm-dx-webserver and gracefully degrades when the
  Spectrum Graph plugin is absent (the spectrum tab is hidden — `MainViewModel.refreshSpectrum`
  marks `isSpectrumAvailable = false` on null/empty data) or the Highpoint Station Logo plugin
  is absent (`findStationLogo` falls back to the central `tef.noobish.eu` directory then a
  default placeholder).

## Development Practices
- Prefer stable, non-deprecated Android and Kotlin APIs. When you must touch deprecated code, leave
  a short comment describing why it is required and reference the tracking task if one exists.
- Keep Gradle and manifest settings aligned with the module’s current compile/target SDK
  (API 36.1). Only bump versions when explicitly asked and update all related config together.
- Lean on Kotlin idioms: scope functions, sealed hierarchies, and structured concurrency with
  `CoroutineScope`s that respect lifecycle boundaries.
- Any new functionality should ship with relevant automated verification (unit tests, instrumentation
  tests, or lint rules). Mirror existing test patterns in the module.
- Prefer Compose Material 3 components already in use, match the project theme, and avoid bringing
  in new UI toolkits without approval.
- Honor the existing style guide: reuse established typography, colors, spacing, and component
  patterns unless product explicitly requests a deviation.
- Status/metric headers (e.g., `RdsLabelText`, server info rows such as “Connected users” or
  “Latency”)
  must use the theme’s primary color (`MaterialTheme.colorScheme.primary`), while body text under
  the
  header stays on-surface/on-surfaceVariant, so new diagnostics match the existing spec.
- Keep the UI modern and Compose-first: add or update composables with previews where practical so
  layouts stay testable, and avoid touching the audio playback stack unless a task explicitly calls
  for it.
- Keep UI logic in ViewModels and leave composables in `MainUi.kt` (or feature-specific UI
  files) so previews and UI-only changes never leak into the state layer.
- **Connection / RDS state is owned by `FmDxSessionController`**, an Application-scoped singleton
  (constructed in `org.fmdx.app.FmDxApp` and reachable via
  `(application as FmDxApp).sessionController`). It owns the control WebSocket, the live
  `TunerState`, station-logo lookup, latency monitor, and `KEY_LAST_SERVER_URL` persistence.
  `MainViewModel` observes `sessionController.state` via `combine(...)` and forwards user actions
  (`connect`, `disconnect`, `sendCommand`, `mutateTunerState`) — do not reintroduce the control
  socket, command flow, or RDS fields into the ViewModel.
- Live now-playing metadata is pushed to the system media notification by `PlaybackService`
  itself, which collects `sessionController.state` and calls
  `player.replaceMediaItem(0, …)` on its own session player (see
  `audio/PlaybackService.kt::startMetadataPump`). Do not route metadata via a `SessionCommand` from
  the ViewModel — that path was removed.
- Field mapping for the now-playing card is centralized in `audio/MediaItemBuilder.kt`
  (`nowPlayingFieldsFor` / `mediaMetadataFor` / `buildMediaItemForServer`). Reuse those when adding
  new metadata surfaces; tests live in `MediaItemBuilderTest`.
- Preserve existing card grouping within each section; don't introduce new swipe carousels or
  restructure detail panes unless a ticket requests it.
- The tuner’s frequency picker uses `NumberPicker` for the MHz column and the generic `Picker`
  for the decimal column (both from `compose-material3-picker`). Keep their typography/sizing in
  sync (headlineMedium, equal label heights) and preserve the current looping decimal behavior
  where spinning past `.9` increments MHz and spinning below `.0` decrements it. Any tweaks to
  tuning controls belong in `FrequencyControlsCard` inside `MainUi.kt`.

## Build & Install Checklist
These steps assume you are in the repository root.

1. **Install Android SDK preview packages (API 36.1)**  
   Ensure `ANDROID_HOME` or `ANDROID_SDK_ROOT` points at an SDK containing:
   - `platforms;android-36.1`
   - `build-tools;36.1.0`
   - `platform-tools`  
   Use `sdkmanager --sdk_root="$ANDROID_SDK_ROOT" <package>` if anything is missing.

2. **Prime the Gradle wrapper**  
   ```bash
   ./gradlew --version
   ```
   This downloads the wrapper JAR/distribution if necessary.

3. **Assemble the debug APK**  
   ```bash
   ./gradlew :fm-dx-app:assembleDebug
   ```
   The APK is written to `android/fm-dx-app/build/outputs/apk/debug/fm-dx-app-debug.apk`.  
   Note: warnings like `Unable to strip … libandroidx.graphics.path.so` are expected.

4. **(Optional) Install on a connected device or emulator**  
   ```bash
   ./gradlew :fm-dx-app:installDebug
   ```
   Follow with `adb shell am start -n org.fmdx.app/.MainActivity` if you want to launch it from the CLI.
   When more than one device is attached (e.g. emulator + a wireless-debugged phone), pass `-s
   <serial>` to `adb` and use the APK at
   `android/fm-dx-app/build/outputs/apk/debug/fm-dx-app-debug.apk`.

   The project ships a shared run configuration at `.idea/runConfigurations/fm_dx_app.xml`, so the
   Play ▶ button works on a fresh checkout without manually re-creating the launch entry.

5. **Troubleshooting compiler cache errors**  
   If the Kotlin compiler aborts with cache/daemon issues, stop any running daemons and retry:
   ```bash
   ./gradlew --stop
   ```

## Docker-Based Build (CI/Container)

If you need to build inside a Debian-based container, mirror `docker-build.sh`:

1. **Install prerequisites**
   ```bash
   apt-get update && apt-get install -y wget unzip curl openjdk-21-jdk
   export ANDROID_SDK_ROOT=/usr/lib/android-sdk
   export ANDROID_HOME=/usr/lib/android-sdk
   ```

2. **Download Android command-line tools**
   ```bash
   wget -O /tmp/android-commandlinetools.zip \
     https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip
   unzip -u /tmp/android-commandlinetools.zip -d "$ANDROID_SDK_ROOT"
   ```

3. **Update and install required SDK components**
   ```bash
   "$ANDROID_SDK_ROOT/cmdline-tools/bin/sdkmanager" --sdk_root="$ANDROID_SDK_ROOT" --update
   yes | "$ANDROID_SDK_ROOT/cmdline-tools/bin/sdkmanager" --sdk_root="$ANDROID_SDK_ROOT" \
     "platforms;android-36.1"
   ```

4. **Create `local.properties` with the SDK path**
   ```bash
   echo "sdk.dir=$ANDROID_SDK_ROOT" > local.properties
   ```

5. **Build**
   ```bash
   ./gradlew --version
   ./gradlew :fm-dx-app:assembleDebug
   ```

Keep the SDK path writable and persist the command-line tools in CI caches to avoid re-downloading on every build.
