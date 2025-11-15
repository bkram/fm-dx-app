# Roadmap

## 1.5.5 – Help & Onboarding Tab

- **Feature goals**: introduce a Help tab that provides quick-start documentation, explaining how to
  connect, tune, and troubleshoot—geared toward first-time users.
- **Technical steps**
    - Add a new tab entry in `SectionTab` (likely after About) with its own composable (e.g.,
      `HelpSection`) that displays structured content (cards/accordions) following the Material 3
      design already used.
    - Draft the help copy (sections like “Getting connected”, “Tuning & audio”, “Common issues”) in
      `strings.xml`, keeping text concise and localizable; provide previews so designers can review
      without running the app.
    - Consider embedding contextual actions (open website, contact support) via deep links and
      ensure accessibility (heading semantics, TalkBack-friendly ordering).
    - Add screenshot/regression tests if feasible (Compose screenshot tests) or unit tests
      validating that the Help tab renders even when the app is offline.

## 1.6 – Enhanced Server Diagnostics

- **Feature goals**: split the existing server view into “Connection” vs “Server info” tabs and
  enrich the latter with concurrent user counts and control-socket round-trip latency.
- **Technical steps**
    - Introduce a tabbed layout in `MainUi.kt` (e.g., `TabRow` with “Connection”/“Server Info”),
      keeping connection controls isolated from read-only diagnostics; update previews and
      navigation state accordingly.
    - Extend `FmDxRepository` and control WebSocket parsing to expose users/latency metrics (based
      on server payloads or ping-heartbeats) and persist them in `UiState`, smoothing latency (EMA)
      for UI stability.
    - Populate the Server Info tab with the new telemetry plus the existing metadata cards, ensuring
      Compose components mirror the established styles and accessibility semantics.
    - Add analytics/logging hooks so regressions can be monitored, and document the new output in
      `README.md`.

## 1.6.5 – Audio Recording

- **Feature goals**: allow users to record the currently tuned audio stream for later
  playback/export.
- **Technical steps**
    - Introduce a recording controller in the audio layer (`WebSocketAudioPlayer` or a sibling
      class) that can persist PCM/encoded chunks to app storage, respecting scoped-storage rules.
    - Provide lifecycle-aware controls in the player UI (e.g., a toggle button) that request runtime
      permissions (`READ_MEDIA_AUDIO` / `WRITE_EXTERNAL_STORAGE` on legacy) and show recording
      state.
    - Handle file management: choose a naming convention, surface the destination via `MediaStore`,
      and expose share/delete actions.
    - Add instrumentation/unit tests covering the recording pipeline (buffer boundaries, error
      cases), and ensure background recording behaves correctly when tuning or disconnecting.

## 1.7 – QR-Based Quick Connect

- **Feature goals**: let users scan a QR in the system camera app and deep-link directly into FM DX
  with the embedded server URL; no in-app scanner.
- **Technical steps**
    - Define a public URL/URI scheme (e.g., `https://app.fmdx.org/connect?server=` or
      `fmdx://connect?server=`) and add the matching intent filters/App Links so Android exposes
      “Open in FM DX” when the Camera app detects the QR.
    - Implement deep-link handling in the launcher activity that parses the incoming URI, normalizes
      it via `sanitizeUrl`, and triggers `MainViewModel.connect()` automatically.
    - Provide UX guardrails (confirmation sheet, error dialogs) for unexpected/invalid URLs and log
      metrics for deep-link usage.
    - Add unit tests for the URI parser plus instrumentation tests validating that intent extras
      route through the proper entry point; document the QR payload format for partners.
