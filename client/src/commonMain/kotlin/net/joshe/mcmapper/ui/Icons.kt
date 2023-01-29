package net.joshe.mcmapper.ui

import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.unit.dp
import net.joshe.mcmapper.mapdata.*

enum class BannerColor(val key: String, val color: Color) {
    WHITE("white", Color(255, 255, 255)),
    ORANGE("orange", Color(216, 127, 51)),
    MAGENTA("magenta", Color(178, 76, 216)),
    LIGHTBLUE("light_blue", Color(102, 153, 216)),
    YELLOW("yellow", Color(229, 229, 51)),
    LIME("lime", Color(127, 204, 25)),
    PINK("pink", Color(242, 127, 165)),
    GRAY("gray", Color(76, 76, 76)),
    LIGHTGRAY("light_gray", Color(153, 153, 153)),
    CYAN("cyan", Color(76, 127, 153)),
    PURPLE("purple", Color(127, 63, 178)),
    BLUE("blue", Color(51, 76, 178)),
    BROWN("brown", Color(102, 76, 51)),
    GREEN("green", Color(102, 127, 51)),
    RED("red", Color(153, 51, 51)),
    BLACK("black", Color(25, 25, 25)),
    ;

    companion object {
        fun fromString(name: String) = BannerColor.values().find { it.key == name } ?: WHITE
    }
}

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
    val fillColor = BannerColor.fromString(color).color
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
