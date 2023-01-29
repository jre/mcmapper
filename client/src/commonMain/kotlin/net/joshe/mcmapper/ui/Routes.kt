package net.joshe.mcmapper.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.unit.dp
import net.joshe.mcmapper.mapdata.*

fun RouteNode.getImage() = when (this) {
    is StopNode -> getImage()
    is GateNode -> getImage()
    is PoiNode -> getImage()
}

fun StopNode.getImage(iconSize: Float = 0f) = null

fun GateNode.getImage(iconSize: Float = 10f) = ImageVector.Builder(
    defaultWidth = iconSize.dp,
    defaultHeight = iconSize.dp,
    viewportWidth = iconSize,
    viewportHeight = iconSize
).apply {
    path(fill = SolidColor(BannerColor.PURPLE.color), stroke = SolidColor(Color.Black)) {
        val edge = 1f
        val center = iconSize / 2
        val radius = (iconSize / 2) - 1
        moveTo(edge, center)
        arcTo(radius, radius, 0f, true, true, iconSize - edge, center)
        arcTo(radius, radius, 0f, true, true, edge, center)
        close()
    }
}.build()

fun PoiNode.getImage(iconSize: Float = 6f) = ImageVector.Builder(
    defaultWidth = iconSize.dp,
    defaultHeight = iconSize.dp,
    viewportWidth = iconSize,
    viewportHeight = iconSize
).apply {
    path(fill = SolidColor(Color.Black), stroke = SolidColor(Color.Black)) {
        val edge = 1f
        val center = iconSize / 2
        val radius = (iconSize / 2) - 1
        moveTo(edge, center)
        arcTo(radius, radius, 0f, true, true, iconSize - edge, center)
        arcTo(radius, radius, 0f, true, true, edge, center)
        close()
    }
}.build()

fun getRoutesImage(routes: RoutesMetadata, map: MapMetadata) = map.mapSize().let { mapSize ->
    val minPos = routes.minPos().toMapLayoutPos(map)
    val maxPos = routes.maxPos().toMapLayoutPos(map)
    println("rendering routes image at mapSize=${mapSize} minPos=${minPos} maxPos=${maxPos}")
    ImageVector.Builder(
        defaultWidth = (maxPos.x - minPos.x).dp,
        defaultHeight = (maxPos.y - minPos.y).dp,
        viewportWidth = (maxPos.x - minPos.x).toFloat(),
        viewportHeight = (maxPos.y - minPos.y).toFloat(),
    ).apply {
        for (routePath in routes.paths) {
            require(routePath.path.size >= 2)
            val startPos = NetherPos(routePath.path[0].x, routePath.path[0].z).toMapLayoutPos(map)
            //println("start route with ${routePath.size} points at ${routePath[0]} -> ${startPos}")
            val pathBuilder = PathBuilder()
                .moveTo(startPos.x.toFloat(), startPos.y.toFloat())
            for (point in routePath.path.listIterator(1))
                NetherPos(point.x, point.z).toMapLayoutPos(map).let { pos ->
                    //println("  to ${point} -> ${pos}")
                    pathBuilder.lineTo(pos.x.toFloat(), pos.y.toFloat())
                }
            addPath(pathData = pathBuilder.getNodes(), stroke = SolidColor(Color.Red))
        }
        /* path(stroke = SolidColor(Color.Yellow)) {
            moveTo(0f, 0f)
            lineTo(mapSize.width.value, 0f)
            lineTo(mapSize.width.value, mapSize.height.value)
            lineTo(0f, mapSize.height.value)
            lineTo(0f, 0f)
        } */
        /* path(stroke = SolidColor(Color.Green)) {
            moveTo(minPos.x.toFloat(), minPos.y.toFloat())
            lineTo(maxPos.x.toFloat(), minPos.y.toFloat())
            lineTo(maxPos.x.toFloat(), maxPos.y.toFloat())
            lineTo(minPos.x.toFloat(), maxPos.y.toFloat())
            lineTo(minPos.x.toFloat(), minPos.y.toFloat())
        } */
    }.build()
}
