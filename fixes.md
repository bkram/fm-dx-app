# Lint and Deprecation Audit (2025-02-14, updated)

## Overview
- Latest command: `./gradlew :fm-dx-app:lintRelease` (API 36.1 toolchain)
- Status: **0 errors, 3 warnings** (cleartext allowance, unused launcher vector, unused `fmdx_background`)
- No Kotlin/Compose calls marked with `@Deprecated` in project sources.

## Action Items
1. **Compose API lint** ✅
   - `android/fm-dx-app/src/main/java/org/fmdx/app/MainActivity.kt:1722`  
     Moved `modifier` ahead of other defaulted parameters in `AnnotatedErrorText`; lint no longer flags the composable.

2. **Manifest orientation** ✅
   - `android/fm-dx-app/src/main/AndroidManifest.xml`  
     Removed `android:screenOrientation="fullSensor"` from `MainActivity` to satisfy Android 16+ guidance. Latest lint run confirms the warning is gone.

3. **String pluralization** ✅
   - `android/fm-dx-app/src/main/res/values/strings.xml`  
     `settings_current_buffers` now uses `<plurals>` with `pluralStringResource`; removed the unused `af_frequencies` entry entirely.

4. **Unused resources cleanup**
   - Strings remain trimmed (e.g. `audio_stopped`, `signal_label`, `about_links_github` removed) and the AF plural is gone.
   - Reintroduced `fmdx_background` and the original WebP launcher art to restore branding. Lint now flags `fmdx_background` and the Material `ic_launcher_foreground` vector as unused; decide whether to keep/suppress or remove the redundant assets.

5. **Adaptive icon compliance**
   - Adaptive icons now reference the restored `@mipmap/fmdx_foreground` art with `@drawable/ic_launcher_monochrome` as the monochrome layer. Density-specific WebP assets are back in place to preserve the original look.

6. **Security configuration**
   - `android/fm-dx-app/src/main/res/xml/network_security_config.xml:3`  
     `cleartextTrafficPermitted="true"` weakens transport security. Tighten unless explicit cleartext requirement exists.
     - Current status: HTTP access is a hard requirement (no PI handling), so cleartext allowance stays for now.

7. **Theme resource qualifier** ✅
   - `android/fm-dx-app/src/main/res/values/themes.xml:4`  
     Remove `tools:targetApi="l"` wrapper; minSdk already ≥ 21.

8. **Gradle dependency notation** ✅
   - `build/reports/problems/problems-report.html:653`  
     AGP still requests dependencies using multi-string notation (e.g. `'group', 'artifact', 'version'`). Update to single-string form before Gradle 10.

## Follow-Up
- Remaining warnings: `InsecureBaseConfiguration` (intentional HTTP requirement) and `UnusedResources` for `ic_launcher_foreground` and `fmdx_background`.
- Re-run `./gradlew :fm-dx-app:lintRelease` after future fixes to verify resolution.
- Consider enabling `lintOptions.abortOnError` for CI once the outstanding warnings are resolved or suppressed with justification.
