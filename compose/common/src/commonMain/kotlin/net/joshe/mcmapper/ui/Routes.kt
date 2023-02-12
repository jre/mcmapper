package net.joshe.mcmapper.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.*
import androidx.compose.ui.platform.LocalDensity
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

@Composable
fun getRoutesImage(routes: RoutesMetadata, map: MapMetadata) = map.mapSize().let { mapSize ->
    //val minPos = routes.minPos().toMapLayoutPos(map)
    //val maxPos = routes.maxPos().toMapLayoutPos(map)
    println("rendering routes image at mapSize=${mapSize}")
    ImageVector.Builder(
        defaultWidth = mapSize.width, defaultHeight = mapSize.height,
        viewportWidth = mapSize.width.value, viewportHeight = mapSize.height.value
    ).apply {
        with(LocalDensity.current) {
            for (routePath in routes.paths) {
                require(routePath.path.size >= 2)
                val startPos = NetherPos(routePath.path[0].x, routePath.path[0].z).toMapLayoutPos(map)
                //println("start route with ${routePath.size} points at ${routePath[0]} -> ${startPos}")
                val pathBuilder = PathBuilder()
                    .moveTo(startPos.x.toDp().value, startPos.y.toDp().value)
                for (point in routePath.path.listIterator(1))
                    NetherPos(point.x, point.z).toMapLayoutPos(map).let { pos ->
                        //println("  to ${point} -> ${pos}")
                        pathBuilder.lineTo(pos.x.toDp().value, pos.y.toDp().value)
                    }
                addPath(pathData = pathBuilder.getNodes(), stroke = SolidColor(Color.Red))
            }
        }
    }.build()
}
