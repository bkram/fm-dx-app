# Android App Agent Instructions
- Prefer stable, non-deprecated Android and Kotlin APIs. If a deprecated symbol is unavoidable,
  document why in code comments and look for an alternative first.
- Align new Gradle or manifest configuration with the module's existing compile and target SDK (
  currently API 36.1) unless specifically instructed otherwise.
- Favor Kotlin idioms (e.g., scope functions, sealed types) and keep coroutine code structured
  concurrency friendly.
- Update or add automated checks (lint, unit tests) relevant to changes in this module when
  introducing new functionality.

## Building the Project
To build and install the Android app locally, follow these steps from the repository root:

1. Install the Android SDK (API level 36.1) and ensure that `ANDROID_HOME` or `ANDROID_SDK_ROOT`
   points at it. The build expects the preview platform and tools, so confirm these packages are
   present (use `sdkmanager --sdk_root="$ANDROID_SDK_ROOT"` if needed):
   - `platforms;android-36.1`
   - `build-tools;36.1.0`
   - `platform-tools`
2. Prime the Gradle wrapper (downloads the wrapper JAR and distribution if missing):
   ```bash
   ./gradlew --version
   ```
3. Build the debug APK (first run may take ~9 minutes while the Kotlin compiler falls back to a
   non-daemon mode and emits cache-closing warnings):
   ```bash
   ./gradlew assembleDebug
   ```
   The resulting artifact is written to `android/app/build/outputs/apk/debug/app-debug.apk`. During
   the build you may also see `Unable to strip … libandroidx.graphics.path.so`; this is expected and
   the library is packaged as-is.
4. (Optional) Install the debug build on a connected device or running emulator:
   ```bash
   ./gradlew installDebug
   ```
5. If the Kotlin compiler continues to abort with cache errors on subsequent runs, stop any cached
   daemons before retrying:
   ```bash
   ./gradlew --stop
   ```
