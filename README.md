# FrameCut

Native Android screen and region recorder built with Kotlin, Jetpack Compose, Material 3, MediaProjection, OpenGL ES, MediaCodec, and MediaMuxer.

## Core Features

- **Full Screen & Region Capture**: Record the entire display or an adjustable custom screen rectangle with aspect-ratio presets (`Free`, `16:9`, `9:16`, `4:3`, `1:1`) and GPU-accelerated OpenGL ES cropping.
- **High-Bitrate H.265/HEVC Encoding**: Configurable video bitrate presets (`100` to `350 Mbps`), `VBR`/`CQ`, `8-bit`/`10-bit`, `30`/`60 FPS`, and `Portrait` / `Landscape` orientation modes.
- **Audio Capture**: Supports internal device playback audio (`AudioPlaybackCapture`) and microphone input (`RECORD_AUDIO`) with Mono AAC or Mono MP3 (`48 kHz`, `128 kbps`).
- **Mobile & Tablet Adaptive UI**: Responsive phone single-pane and tablet multi-pane/grid layouts backed by a permanent offline multi-format UI definition system (`Kotlin` registries, `XML` tokens/styles, local `.ttf` fonts, vector/bitmap drawables, and `JSON` asset specs).
- **Foreground Recording Service**: Persistent notification controls for Pause, Resume, and Stop, plus optional floating region border overlay (`SYSTEM_ALERT_WINDOW`).
- **Recordings Library**: Browse, play, share, and delete recorded MP4 videos saved via `MediaStore` / FileProvider.

## Tech Stack & Build

- Kotlin `2.2.10` + Jetpack Compose (Material 3)
- `MediaProjection`, `MediaCodec`, `MediaMuxer`, OpenGL ES, `DataStore` Preferences, Navigation Compose
- Target Architecture: **ARM64-only (`arm64-v8a`)** for **Android Mobile & Tablet** (`minSdk = 36`, `compileSdk = 37`, `targetSdk = 37`, AGP `9.1.1`, Gradle `9.3.1`)
- Automatic Debug APK CI: Pushing to `main` or `master` runs `.github/workflows/build-apk.yml` to build, verify (`arm64-v8a`-only), and publish `app-debug.apk` to GitHub Releases. See `OFFLINE_BUILD_KIT.md` and `UI_ARCHITECTURE.md` for architecture and offline build setup.
