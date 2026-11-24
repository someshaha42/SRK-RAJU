package com.framecut.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object FrameCutShapes {
    val BrandBadgeShape = RoundedCornerShape(8.dp)
    val ChipShape = RoundedCornerShape(10.dp)
    val CardThumbShape = RoundedCornerShape(12.dp)
    val SubRowShape = RoundedCornerShape(12.dp)
    val TileShape = RoundedCornerShape(14.dp)
    val ActionButtonShape = RoundedCornerShape(16.dp)
    val BannerShape = RoundedCornerShape(16.dp)
    val RecordingCardShape = RoundedCornerShape(16.dp)
    val EmptyStateBoxShape = RoundedCornerShape(18.dp)
    val SectionCardShape = RoundedCornerShape(20.dp)
    val ConsoleCardShape = RoundedCornerShape(24.dp)
    val DialogShape = RoundedCornerShape(20.dp)

    val MaterialShapes = Shapes(
        small = ChipShape,
        medium = TileShape,
        large = SectionCardShape,
        extraLarge = ConsoleCardShape,
    )
}
