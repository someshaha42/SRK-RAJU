package com.framecut.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.framecut.ui.core.UiColorRole

@Immutable
data class StudioColors(
    val recordAccent: Color,
    val recordDeep: Color,
    val recordContainer: Color,
    val pausedAccent: Color,
    val pausedContainer: Color,
    val readyAccent: Color,
    val readyContainer: Color,
    val regionPreviewScrim: Color = Color.Black.copy(alpha = 0.45f),
    val regionEditorScrim: Color = Color.Black.copy(alpha = 0.55f),
    val regionOverlayBackdrop: Color = Color(0xF00A0C10),
    val regionCanvasFrameBorder: Color = Color.White.copy(alpha = 0.25f),
    val regionNeutralGuide: Color = Color.White.copy(alpha = 0.45f),
    val dragBadgeBackdrop: Color = Color(0xB80D0F14),
)

val DarkStudioColors = StudioColors(
    recordAccent = StudioCrimson,
    recordDeep = StudioCrimsonDeep,
    recordContainer = StudioCrimsonContainerDark,
    pausedAccent = StudioAmber,
    pausedContainer = StudioAmberContainerDark,
    readyAccent = StudioEmerald,
    readyContainer = StudioEmeraldContainerDark,
)

val LightStudioColors = StudioColors(
    recordAccent = StudioCrimsonDeep,
    recordDeep = StudioCrimsonDeep,
    recordContainer = StudioCrimsonContainerLight,
    pausedAccent = StudioAmber,
    pausedContainer = StudioAmberContainerLight,
    readyAccent = StudioEmeraldDark,
    readyContainer = StudioEmeraldContainerLight,
)

val LocalStudioColors = staticCompositionLocalOf { DarkStudioColors }

private val DarkColorScheme = darkColorScheme(
    background = StudioDarkBackground,
    surface = StudioDarkSurface,
    surfaceVariant = StudioDarkSurfaceVariant,
    surfaceContainerLow = StudioDarkSurface,
    surfaceContainerHigh = StudioDarkSurfaceVariant,
    surfaceContainerHighest = Color(0xFF252B3B),
    outline = StudioDarkOutline,
    outlineVariant = StudioDarkOutlineVariant,
    onSurface = StudioDarkOnSurface,
    onSurfaceVariant = StudioDarkOnSurfaceVariant,
    primary = StudioCrimson,
    onPrimary = Color.White,
    primaryContainer = StudioCrimsonContainerDark,
    onPrimaryContainer = StudioCrimsonOnContainerDark,
    secondary = StudioSteelBlueLight,
    onSecondary = Color(0xFF0A1D3D),
    secondaryContainer = StudioSteelBlueContainerDark,
    onSecondaryContainer = StudioSteelBlueOnContainerDark,
    tertiary = StudioEmerald,
    onTertiary = Color(0xFF04261B),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFECACA),
)

private val LightColorScheme = lightColorScheme(
    background = StudioLightBackground,
    surface = StudioLightSurface,
    surfaceVariant = StudioLightSurfaceVariant,
    surfaceContainerLow = StudioLightSurface,
    surfaceContainerHigh = StudioLightSurfaceVariant,
    surfaceContainerHighest = Color(0xFFE0E5EE),
    outline = StudioLightOutline,
    outlineVariant = StudioLightOutlineVariant,
    onSurface = StudioLightOnSurface,
    onSurfaceVariant = StudioLightOnSurfaceVariant,
    primary = StudioCrimsonDeep,
    onPrimary = Color.White,
    primaryContainer = StudioCrimsonContainerLight,
    onPrimaryContainer = StudioCrimsonOnContainerLight,
    secondary = StudioSteelBlue,
    onSecondary = Color.White,
    secondaryContainer = StudioSteelBlueContainerLight,
    onSecondaryContainer = StudioSteelBlueOnContainerLight,
    tertiary = StudioEmeraldDark,
    onTertiary = Color.White,
    error = StudioCrimsonDeep,
    onError = Color.White,
    errorContainer = StudioCrimsonContainerLight,
    onErrorContainer = StudioCrimsonOnContainerLight,
)

@Composable
fun FrameCutTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val studioColors = if (darkTheme) DarkStudioColors else LightStudioColors

    CompositionLocalProvider(LocalStudioColors provides studioColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FrameCutTypography,
            shapes = FrameCutShapes.MaterialShapes,
            content = content,
        )
    }
}

@Composable
fun resolveThemeColor(role: UiColorRole): Color {
    val cs = MaterialTheme.colorScheme
    val sc = LocalStudioColors.current
    return when (role) {
        UiColorRole.BACKGROUND -> cs.background
        UiColorRole.SURFACE -> cs.surface
        UiColorRole.SURFACE_VARIANT -> cs.surfaceVariant
        UiColorRole.OUTLINE -> cs.outline
        UiColorRole.OUTLINE_VARIANT -> cs.outlineVariant
        UiColorRole.ON_SURFACE -> cs.onSurface
        UiColorRole.ON_SURFACE_VARIANT -> cs.onSurfaceVariant
        UiColorRole.PRIMARY -> cs.primary
        UiColorRole.ON_PRIMARY -> cs.onPrimary
        UiColorRole.PRIMARY_CONTAINER -> cs.primaryContainer
        UiColorRole.ON_PRIMARY_CONTAINER -> cs.onPrimaryContainer
        UiColorRole.SECONDARY -> cs.secondary
        UiColorRole.ON_SECONDARY -> cs.onSecondary
        UiColorRole.SECONDARY_CONTAINER -> cs.secondaryContainer
        UiColorRole.ON_SECONDARY_CONTAINER -> cs.onSecondaryContainer
        UiColorRole.TERTIARY -> cs.tertiary
        UiColorRole.ERROR -> cs.error
        UiColorRole.ERROR_CONTAINER -> cs.errorContainer
        UiColorRole.RECORD_ACCENT -> sc.recordAccent
        UiColorRole.RECORD_DEEP -> sc.recordDeep
        UiColorRole.RECORD_CONTAINER -> sc.recordContainer
        UiColorRole.PAUSED_ACCENT -> sc.pausedAccent
        UiColorRole.PAUSED_CONTAINER -> sc.pausedContainer
        UiColorRole.READY_ACCENT -> sc.readyAccent
        UiColorRole.READY_CONTAINER -> sc.readyContainer
    }
}
