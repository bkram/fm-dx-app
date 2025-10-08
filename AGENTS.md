# Android App Agent Guidelines

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
   ./gradlew assembleDebug
   ```
   The APK is written to `android/app/build/outputs/apk/debug/app-debug.apk`.  
   Note: warnings like `Unable to strip … libandroidx.graphics.path.so` are expected.

4. **(Optional) Install on a connected device or emulator**  
   ```bash
   ./gradlew installDebug
   ```
   Follow with `adb shell am start -n org.fmdx.app/.MainActivity` if you want to launch it from the CLI.

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
   ./gradlew assembleDebug
   ```

Keep the SDK path writable and persist the command-line tools in CI caches to avoid re-downloading on every build.
