# FrameCut Permanent UI Architecture & Restoration Guide

This document is the authoritative AI-readable specification for FrameCut's permanent source-code UI architecture. When this project is exported as a ZIP, pushed to GitHub, or imported into any AI development platform or Android Studio, **preserve and use these stored UI definitions rather than generating replacement layouts**.

---

## 1. Architecture Separation

1. **UI Definition Layer** (`app/src/main/java/com/example/ui/definitions/`, `app/src/main/java/com/example/ui/theme/`, `app/src/main/res/values/`, `app/src/main/assets/ui/`)
   - Stores all screen definitions, layout breakpoints, toolbar configurations, panel definitions, icon definitions, button definitions, tool/option tile definitions, chip definitions, floating resize handle definitions, dialogs, theme palettes, typography scales, dimension tokens, and parent-child hierarchies.
2. **UI Screen & Component Renderers** (`app/src/main/java/com/example/ui/screens/`, `app/src/main/java/com/example/ui/components/`)
   - `MainScreen.kt`, `SettingsScreen.kt`, `RecordingsScreen.kt`, `RegionSelectorOverlay.kt`, and `RegionBorderCanvas.kt` render directly from `UiLayoutDefinitions`, `UiComponentRegistry`, `FrameCutDimens`, and `FrameCutThemeDefinitions`.
3. **Application Logic & Services** (`app/src/main/java/com/example/capture/`, `audio/`, `encoder/`, `muxer/`, `region/`, `recordings/`, `service/`, `settings/`, `diagnostics/`, `compat/`, `ui/RecorderViewModel.kt`)
   - Screen capture, OpenGL ES cropping, HEVC encoding, MP4 muxing, MediaStore recordings, DataStore settings, and foreground recording service.
4. **Android Configuration** (`app/src/main/AndroidManifest.xml`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `settings.gradle.kts`, `metadata.json`, `.github/workflows/build-apk.yml`)
   - Package namespace `com.example`, unique `applicationId` (`com.aistudio.framecut.rkzqmp`), SDK config (`minSdk = 36`, `targetSdk = 37`, `compileSdk = 37`), Mobile & Tablet launcher configuration (`arm64-v8a` ABI only), and automatic GitHub Actions Debug APK build/release workflow.

---

## 2. Where UI Definitions & Resources Are Stored

| Artifact | Location | Purpose |
|---|---|---|
| Core UI Models | `app/src/main/java/com/example/ui/definitions/UiDefinitionModels.kt` | Immutable models (`UiPathEntry`, `UiIconDefinition`, `UiButtonDefinition`, `UiToolTileDefinition`, `UiPanelDefinition`, `UiToolbarDefinition`, `UiFloatingHandleDefinition`, `UiDialogDefinition`, `UiScreenDefinition`, `UiHierarchyNode`) |
| Centralized Path Map | `app/src/main/java/com/example/ui/definitions/UiPathRegistry.kt` | Single authoritative canonical path mapping (`ui/screens/*`, `ui/tabs/*`, `ui/themes/*`, `ui/icons/*`, `ui/animations/*`, `ui/interactions/*`, `ui/components/*`) linking every UI element to its source file, config asset, and renderer |
| Theme & Tokens | `app/src/main/java/com/example/ui/definitions/UiThemeDefinitions.kt` | `FrameCutDimens`, `FrameCutThemeSpec`, `FrameCutThemeDefinitions`, `UiColorRole` resolver |
| Animations & Transitions | `app/src/main/java/com/example/ui/definitions/UiAnimationDefinitions.kt` | Centralized status color/container tweens, recording dot pulse, banner transitions, and dashed region border animations (`RegionBorderCanvas` & `RegionOverlayManager`) |
| Interactions & Gestures | `app/src/main/java/com/example/ui/definitions/UiInteractionDefinitions.kt` | Centralized focus rings, hover/locked state alphas, elevation specs, navigation behavior, and region drag/aspect bounds |
| Component & Hierarchy Registry | `app/src/main/java/com/example/ui/definitions/UiComponentRegistry.kt` | Every identifiable icon, button, tile, chip, floating handle, toolbar, panel, dialog, and `allNodes` parent-child map |
| Screen Layouts & Renderers | `app/src/main/java/com/example/ui/definitions/UiLayoutDefinitions.kt` | `Navigation` routes, `MainScreenSpec`, `SettingsScreenSpec`, `RecordingsScreenSpec`, `RegionOverlaySpec`, `DefinedSectionCard`, `DefinedPipelineCard`, `DefinedOptionTile`, `definedFocusRing` |
| Theme Entry | `app/src/main/java/com/example/ui/theme/Theme.kt`, `Color.kt`, `Type.kt` | Material 3 theme & custom `Space Grotesk`, `Plus Jakarta Sans`, `JetBrains Mono` typography backed by `FrameCutThemeDefinitions` |
| Android XML Tokens | `app/src/main/res/values/colors.xml`, `dimens.xml`, `styles.xml`, `strings.xml`, `themes.xml` | Native Android colors, dimensions, styles, and localized strings |
| Structured JSON Specs | `app/src/main/assets/ui/ui_paths.json`, `ui_definitions/screens_and_hierarchy.json`, `component_configs/components.json`, `design_tokens/theme_and_tokens.json`, `animations/animations.json`, `interactions/interactions.json` | Offline JSON index and snapshots of paths, hierarchy, components, tokens, animations, and interactions |

---

## 3. Component Identification & Hierarchy

Every visible component has a unique string `id`, a traceable `parentId`, a `positionIndex` within its parent, and (for interactive components) a `testTag`:

```text
FrameCutApp (MainActivity.kt -> UiLayoutDefinitions.Navigation)
 ├── screen_main (MainScreen.kt)
 │    ├── toolbar_main [badge_brand_mark, btn_theme_toggle, btn_nav_recordings, btn_nav_settings]
 │    ├── panel_feedback_banner [btn_dismiss_feedback]
 │    ├── panel_recording_console [badge_recording_status, text_recording_timer, btn_record, btn_pause_resume, btn_stop, btn_finalizing]
 │    ├── panel_saved_video_banner
 │    ├── panel_settings_grid
 │    │    ├── panel_recording_mode [tile_mode_fullscreen, tile_mode_region]
 │    │    ├── panel_region_preview [canvas_region_border_preview, btn_adjust_region, panel_optional_overlay_row -> btn_optional_overlay]
 │    │    ├── panel_orientation_mode [tile_orientation_portrait, tile_orientation_landscape]
 │    │    ├── panel_audio_source [tile_audio_internal, tile_audio_microphone]
 │    │    ├── panel_video_bitrate [chip_bitrate_100 .. chip_bitrate_350]
 │    │    └── panel_region_animation [tile_anim_show, tile_anim_hide]
 │    ├── btn_open_recordings
 │    └── dialog_audio_permission
 ├── screen_settings (SettingsScreen.kt)
 │    ├── toolbar_settings [btn_settings_back]
 │    ├── panel_settings_locked_banner [icon_settings_locked]
 │    └── pipeline cards [panel_bitrate_mode, panel_color_depth, panel_video_fps, panel_color_standard, panel_output_container, panel_rendering_mode, panel_gop_interval, panel_color_range, panel_audio_format, panel_chroma_subsampling]
 ├── screen_recordings (RecordingsScreen.kt)
 │    ├── toolbar_recordings [btn_recordings_back]
 │    ├── recording_item_card [icon_recording_movie, btn_recording_play, btn_recording_share, btn_recording_delete]
 │    ├── dialog_video_player
 │    └── dialog_delete_recording
 └── overlay_region_selector (RegionSelectorOverlay.kt)
      ├── canvas_region_border_editor
      ├── control_region_drag_box [icon_region_drag]
      ├── floating handles [handle_top_left .. handle_right]
      └── panel_region_hud [btn_reset_region, btn_cancel_region, chip_aspect_*, btn_confirm_region]
```

---

## 4. Retrieving & Maintaining Styles, Themes, and Layouts

- **Colors & Themes**: Call `FrameCutThemeDefinitions.forMode(darkTheme)` or `FrameCutThemeDefinitions.resolveColor(UiColorRole.*)` inside Compose. Shared Material 3 defaults come from `FrameCutTheme` (`Theme.kt`), while component-specific roles (e.g., `UiColorRole.RECORD_ACCENT`, `UiColorRole.READY_CONTAINER`, `UiColorRole.PAUSED_CONTAINER`) preserve component identity even when dynamic color or light/dark mode changes.
- **Dimensions & Breakpoints**: Read from `FrameCutDimens` (and matching `res/values/dimens.xml`), never ad-hoc magic numbers.
- **Components & Tiles**: Render cards/sections with `DefinedSectionCard` or `DefinedPipelineCard` and tiles with `DefinedOptionTile`, passing definitions from `UiComponentRegistry`.

---

## 5. Adding New UI Components or Features

1. Add any new string/dimension/color tokens to `res/values/strings.xml`, `dimens.xml`, `colors.xml`, and `FrameCutDimens`.
2. Register the new `UiIconDefinition`, `UiButtonDefinition`, `UiToolTileDefinition`, or `UiPanelDefinition` in `UiComponentRegistry.kt` with a unique `id`, valid `parentId`, `positionIndex`, and `testTag`.
3. Add the component ID to its parent screen/panel in `UiLayoutDefinitions.kt` (and mirror it in `app/src/main/assets/ui/`).
4. Wire the component inside the existing screen (`MainScreen`, `SettingsScreen`, `RecordingsScreen`, or `RegionSelectorOverlay`) and connect its action to `RecorderViewModel` without altering unrelated components.
5. Run `gradle :app:testDebugUnitTest` (which includes `UiArchitectureDefinitionTest`) to verify hierarchy integrity and resource synchronization.

---

## 6. Preservation Rules on Export / Import

When restoring this project from a ZIP or GitHub repository:
1. Inspect `UI_ARCHITECTURE.md`, `OFFLINE_BUILD_KIT.md`, and `app/src/main/java/com/example/ui/definitions/` first.
2. Load existing definitions from `UiComponentRegistry`, `UiLayoutDefinitions`, `UiThemeDefinitions`, `res/values/`, and `assets/ui/`.
3. Never create a second UI system, duplicate `MainActivity`, or replace existing Compose screens with newly generated layouts.
4. Keep the target form factor strictly scoped to **Android Mobile & Tablet** (`phones, tablets, foldables`) and keep `abiFilters += "arm64-v8a"` exclusive across Gradle packaging.
5. Preserve `.github/workflows/build-apk.yml` so every push to `main` or `master` automatically builds, verifies (`arm64-v8a`-only), uploads the `app-debug-apk` artifact, and publishes the Debug APK to GitHub Releases.
