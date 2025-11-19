# Architecture Overview

This document highlights the major pieces involved in the current FM-DX App design, with special
focus on how the pass-through telemetry feature is implemented.

## UI Layer

- **Compose screens** live under `android/fm-dx-app/src/main/java/org/fmdx/app`. `MainUi.kt` renders
  all sections (connection, tuner, spectrum, settings) and captures user interaction callbacks.
- **`MainViewModel`** owns the app state. It:
    - Persists user settings and recent servers via `SharedPreferences`.
    - Owns OkHttp-derived sockets for control (`/text`) and spectrum plugin (`/data_plugins`)
      connections, parsing JSON into strongly-typed models.
    - Sends tuner commands through a buffered channel to avoid overloading the remote server.
    - Applies telemetry preferences and decides when to start or stop the pass-through foreground
      service based on connection state.
- **Playback flow** lives in `PlaybackService` (Media3), with the view-model binding to the session
  via a `MediaController`.

## Networking & Data

- `FmDxRepository` wraps OkHttp creation for control and plugin sockets, spectrum downloads, static
  station info, and station logos.
- Models (`TunerState`, `SpectrumPoint`, etc.) convert WebSocket payloads into Kotlin data classes
  for easy consumption by the UI.
- Spectrum scan requests run through the plugin connection and support fallback polling if the
  server does not send immediate results.

## Pass-Through Telemetry

- The legacy telemetry spec is documented in `docs/udp_payload.md`.
- **`PassThroughService`** (
  `android/fm-dx-app/src/main/java/org/fmdx/app/telemetry/PassThroughService.kt`) is a dedicated
  foreground service that keeps the UDP loop alive even when the UI is backgrounded:
    - Starts/stops with a persistent notification and the `FOREGROUND_SERVICE_DATA_SYNC` capability.
    - Hosts its own control/plugin connections to mirror the tuner state, scanner events, and GPS
      data required by `PassThroughTelemetrySender`.
    - Manages reconnect attempts if either socket fails.
- **`PassThroughTelemetrySender`** emits the documented 27-column CSV once per second, handling GPS
  precedence (live GPS > cached QTH > fallback WebView) and writing to `127.0.0.1:9100`.
- The view-model triggers the service whenever “Pass through” is enabled and the user is connected.
  This keeps telemetry in sync with the current server URL, even if audio is not playing.

## GPS Support

- `GpsStore` and `GpsWebViewHelper` provide shared state for live GPS and static QTH fallback.
- The service updates the store from plugin events; when there are no live GPS packets, it spins up
  a headless WebView to scrape coordinates from the tuner’s cached UI, mirroring the original FMDX
  Connector behavior.

## Future Considerations

- Today both the UI and the pass-through service maintain their own sockets. If we consolidate
  connection ownership into the service (or another shared component), we can eliminate duplicate
  traffic and simplify state propagation.
- As the project grows, consider adding a top-level “system diagram” describing shared components (
  PlaybackService, PassThroughService, repositories) and how they communicate to keep this document
  current.
