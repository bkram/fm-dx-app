## Local USB Tuner Support (Headless TEF)

Goal: allow the Android app to talk directly to the TEF-based headless tuner over USB-C (serial control + USB audio) without a separate fm-dx-webserver host.

### Hardware
- Phone/tablet with USB OTG support.
- USB-C OTG adapter and, if needed, powered hub (the tuner often draws >500 mA).
- TEF headless tuner exposing one composite USB interface (serial control + USB audio).

### Software Tasks
1. **USB enumeration**
   - Request permission via `UsbManager`.
   - Claim the serial interface (CDC/bulk endpoints) and the USB audio interface.

2. **Serial control channel**
   - Use `usb-serial-for-android` or custom bulk I/O.
   - Reuse existing parsing (tuning commands, signal/RDS updates) to populate `TunerState`.

3. **Audio playback**
   - Option A: let Android route the USB Audio Class stream to speakers automatically.
   - Option B: capture the stream in-app (via `AudioRecord`/`AudioTrack` or ExoPlayer) for gain/visualization.

4. **RDS decoding**
   - Bundle `librdsparser` via JNI/NDK (preferred) so we keep identical behaviour to the webserver.
   - Alternative: port or adopt a pure Kotlin RDS parser (more work / less fidelity).

5. **Power & UX**
   - Provide guidance for external power, cable management, heat considerations.
   - Add UI for “local tuner mode” (connection status, permission prompts).

### Open Questions
- How to store tuner-specific configuration (audio gain, default frequency) locally.
- Whether to expose a fallback webserver mode simultaneously.

### librdsparser JNI Integration
- Build `librdsparser` (https://github.com/kkonradpl/librdsparser) for Android ABIs via the NDK.
- Expose JNI wrappers for the same functions the webserver uses:
  - Lifecycle: `rdsparser_new`, `rdsparser_clear`, `rdsparser_parse_string`, `rdsparser_set_text_correction`, `rdsparser_set_text_progressive`, `rdsparser_free`.
  - Callback registration: `rdsparser_register_pi / pty / tp / ta / ms / ecc / country / af / ps / rt / ptyn / ct`.
  - Data access: `rdsparser_get_pi`, `_get_pty`, `_get_tp`, `_get_ta`, `_get_ms`, `_get_ecc`, `_get_country`, `_get_ps`, `_get_rt`, `_get_ptyn`.
  - Lookup helpers: `rdsparser_country_lookup_name`, `_lookup_iso`, `rdsparser_pty_lookup_*`, `rdsparser_string_get_length`, `_get_content`, `_get_errors`.
  - Clock/time: `rdsparser_ct_get_year / month / day / hour / minute / offset`.
- Kotlin layer mirrors the existing Node callbacks, updating `TunerState` and RDS fields so UI behaviour stays identical.

### Git Submodules
- Add `external/librdsparser` as a submodule pointing at `https://github.com/kkonradpl/librdsparser.git` so native sources track upstream.
- Add `external/usb-serial-for-android` (or equivalent) as a submodule for USB CDC/FTDI support, referenced from the app module during the local tuner build.

## Server Address History

Goal: cache and surface the last 20 fm-dx-webserver endpoints so users can quickly reconnect without retyping URLs, while keeping the entry UI Compose-native and aligned with the current Tuner tab styling.

### UX Requirements
- Primary action remains a text input for a new server URL with validation + custom styling already used in Settings.
- Below the input, show a vertically constrained list (max 20) of recent addresses with radio buttons or chips that match the existing Material 3 appearance (green inline labels, surface text for values).
- Provide a clear affordance to remove an entry (swipe-to-dismiss or overflow menu) without cluttering the primary flow.

### Data & Persistence
- Store the most recent URLs in `DataStore` (preferences) scoped to the app; enforce uniqueness while preserving recency order.
- Trim the list to at most 20 entries whenever a new address is added; promote reselected entries to the top.
- Consider migrating any legacy `SharedPreferences` entries if present.

### Compose Implementation
- Use `rememberSaveable` + `LaunchedEffect` to bind text field state with the persisted history so rotations keep the draft intact.
- Expose history via a `LazyColumn` wrapped in the existing card and spacing defaults (`UiDefaults`).
- Emit selection events through `MainViewModel`, updating `UiState` so other components receive the current endpoint immediately.

### Open Questions
- Should we pre-populate with sample/demo servers on first launch?
- Do we surface metadata (last connected time, latency badge) alongside each address for power users?
