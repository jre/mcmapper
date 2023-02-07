package net.joshe.mcmapper.ui

import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.unit.dp
import net.joshe.mcmapper.mapdata.*

val BannerColor.color get() = Color(r, g, b)

fun Icon.getImage() = when (this) {
    is PointerIcon -> getImage()
    is BannerIcon -> getImage()
}

fun PointerIcon.getImage(iconSize: Float = 10f) = ImageVector.Builder(
    defaultWidth = iconSize.dp,
    defaultHeight = iconSize.dp,
    viewportWidth = iconSize,
    viewportHeight = iconSize
).apply {
    group(rotate = rotation.toFloat(), pivotX = iconSize / 2, pivotY = iconSize / 2) {
        path(fill = SolidColor(Color.Green), stroke = SolidColor(Color.Black)) {
            val leftX = iconSize / 4
            val width = iconSize / 2
            val midY = iconSize / 2
            moveTo(leftX, midY)
            curveTo(leftX, 0f, leftX + width, 0f, leftX + width, midY)
            lineTo(leftX + width / 2, midY * 2)
            lineTo(leftX, midY)
            close()
        }
    }
}.build()

fun BannerIcon.getImage(iconSize: Float = 10f): ImageVector {
    val fillColor = color.color
    val topWidth = iconSize * 0.875f
    val bannerWidth = iconSize * .375f
    val bannerHeight = iconSize
    val leftX = topWidth / 2f - bannerWidth / 2f
    val path = PathBuilder()
        .lineTo(topWidth, 0f)
        .moveTo(leftX, 0f)
        .lineToRelative(0f, bannerHeight)
        .lineToRelative(bannerWidth, 0f)
        .lineToRelative(0f, -bannerHeight)
        .close()
    return ImageVector.Builder(
        defaultWidth = topWidth.dp,
        defaultHeight = bannerHeight.dp,
        viewportWidth = topWidth,
        viewportHeight = bannerHeight,
    ).apply {
        addPath(
            pathData = path.getNodes(),
            fill = SolidColor(fillColor),
            stroke = SolidColor(Color.Black),
        )
    }.build()
}
