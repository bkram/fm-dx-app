# Architecture Overview

This document highlights the major pieces involved in the current FM-DX App design, with special
focus on how the pass-through telemetry feature is implemented.

## Connection & state ownership

- **`FmDxSessionController`** (`data/FmDxSessionController.kt`), an Application-scoped singleton, is
  the single source of truth for connection / RDS / frequency state for BOTH connection kinds. It
  exposes `state: StateFlow<FmDxSessionState>` (carrying `ConnectionType.SERVER` / `USB`).
  - **Server** (`connect(url)`): owns the control WebSocket via `FmDxRepository.connectControl`
      (`/text`, JSON → `TunerState`), the latency monitor, and station-logo lookup.
  - **Direct USB** (`connectUsb()`): opens a `TunerControlTransport` and speaks the XDR/xdrd line
      protocol straight to the tuner. Persists/restores the last USB frequency.
  - Tuner commands flow through a buffered channel (`sendCommand`) so neither the server nor the
      serial link is overloaded. The transport `onState` preserves user-set `eq`/`ims`/
      `stereoForced`/`antennaIndex` (the raw protocol never echoes them).

## UI Layer

- **Compose screens** live under `android/fm-dx-app/src/main/java/org/fmdx/app`. `MainUi.kt` renders
  all sections (connection, tuner, information/RDS, spectrum, server info, settings, about). The
  Connection screen offers a **Remote / Direct** segmented choice (persisted); server-only surfaces
  (spectrum, station logo, transmitter info, ECC/AF, server diagnostics) are gated off the USB path.
- **`MainViewModel`** keeps UI-only state (settings, public-server picker, spectrum + plugin socket,
  audio-playing, live USB-attach presence) and `combine(...)`s it with `sessionController.state`
  into the public `uiState`. It decides when to start/stop the pass-through and USB-audio services.
- **Playback**: server audio runs through `PlaybackService` (Media3) via a `MediaController`; USB
  audio runs through `UsbAudioEngine` inside the `UsbAudioService` microphone foreground service.

## Direct USB tuner stack

- `data/tuner/` — transport-agnostic codec: `TunerControlTransport` (interface), `XdrLineParser`
  (`T`/`S`/`P`/`R`/`o` lines → `TunerState`), `RdsDecoder` (raw RDS groups → PS/RadioText/PTY/TP/TA/
  MS + DI). Pure Kotlin, unit-tested.
- `data/usb/` — `UsbTunerTransport` (CDC-ACM via usb-serial-for-android) and `UsbTunerDiscovery`
  (matches the TEF668X Headless tuner, VID `0x1209` / PID `0x6687`).
- `audio/UsbAudioEngine` + `audio/UsbAudioService` — capture the tuner's USB Audio Class output and
  render it, surviving screen-off via a `microphone` foreground service.

## Networking & Data

- `FmDxRepository` wraps OkHttp creation for control and plugin sockets, spectrum downloads, static
  station info, and station logos (server path only).
- Models (`TunerState`, `SpectrumPoint`, etc.) convert WebSocket payloads into Kotlin data classes;
  the USB/serial path produces the same `TunerState` via `XdrLineParser`/`RdsDecoder`.
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
