# Roadmap

## 1.7.0 – Audio Recording

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

## 1.8 – QR-Based Quick Connect

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

## 1.9 – RDS Spy Capture Export

- **Feature goals**: capture live RDS groups from the control channel and export them in the RDS
  Spy `.spy` format for offline analysis or sharing with DX communities.
- **Technical steps**
    - Mirror the current desktop helper (see `docs/rds-spy-capture.cpp`) by tapping into the
      control socket’s RDS group stream and buffering the `G:` payloads on-device.
    - Provide a Compose UI surface that shows the most recent blocks, allows starting/stopping
      captures, and exports the backlog to `.spy` files stored via `MediaStore` with consistent
      naming conventions.
    - Add background processing safeguards so captures stay alive when the app is backgrounded, and
      expose share/delete actions alongside basic metadata (station, timestamp range).
    - Write JVM unit tests for the formatter and timestamping logic plus instrumentation tests that
      verify storage permission prompts and file export flows.
