package net.joshe.mcmapper.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import net.joshe.mcmapper.mapdata.*

@Composable
fun MapMetadata.mapSize(/*routes: RoutesMetadata?*/) = with(LocalDensity.current) {
    DpSize(((maxVisibleWorldPos.x - minVisibleWorldPos.x) / scaleFactor).toDp(),
        ((maxVisibleWorldPos.z - minVisibleWorldPos.x) / scaleFactor).toDp())
}

fun RoutesMetadata.minPos() = NetherPos(minPos.x, minPos.z)
fun RoutesMetadata.maxPos() = NetherPos(maxPos.x, maxPos.z)

data class MapLayoutPos(val x: Int, val y: Int)

fun TilePos.toMapLayoutPos(map: MapMetadata) = toWorldPosTopLeft(map).toMapLayoutPos(map)

private fun WorldPos.toMapLayoutPos(map: MapMetadata) = MapLayoutPos(
    x = ((x - map.minVisibleWorldPos.x) / map.scaleFactor),
    y = ((z - map.minVisibleWorldPos.x) / map.scaleFactor))

fun NetherPos.toMapLayoutPos(map: MapMetadata) = toWorldPos().toMapLayoutPos(map)

enum class MapLayer(val zIndex: Float) {
    TILE(0f),
    POINTER(1f),
    BANNER(2f),
    ROUTE(3f),
    NODE(4f),
    TEXT(5f),
}

data class MapLayoutInfo(val pos: MapLayoutPos, val layer: MapLayer, val center: Boolean, val debug: Boolean = false) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) = this@MapLayoutInfo
}

class MapLayoutScope(private val map: MapMetadata, private val routes: RoutesMetadata?) {
    fun Modifier.iconPosition(icon: Icon) = this.then(
        MapLayoutInfo(
        pos = WorldPos(icon.pos.x, icon.pos.z).toMapLayoutPos(map),
        layer = when(icon) {
            is PointerIcon -> MapLayer.POINTER
            is BannerIcon -> MapLayer.BANNER
        },
        center = true),
    )

    fun Modifier.routeNodePosition(node: RouteNode): Modifier {
        NetherPos(node.pos.x, node.pos.z).toMapLayoutPos(map).let {
            println("route node at (x=${node.pos.x}, z=${node.pos.z}) -> ${it} \"${node.label.replace('\n', ' ')}\"")
        }
        return then(
            MapLayoutInfo(pos = NetherPos(node.pos.x, node.pos.z).toMapLayoutPos(map), layer = MapLayer.NODE, center = true)
        )
    }

    fun Modifier.routeGlobalOverlayPosition() = this.then(
        MapLayoutInfo(
        //pos = routes?.minPos()?.toMapLayoutPos(map) ?: MapLayoutPos(0, 0),
        pos = MapLayoutPos(0, 0),
        layer = MapLayer.ROUTE,
        debug = true,
        center = false)
    )

    fun Modifier.tilePosition(pos: TilePos) = this.then(
        MapLayoutInfo(pos = pos.toMapLayoutPos(map), layer = MapLayer.TILE, center = false)
    )

    fun Modifier.tileIdPosition(pos: TilePos) = this.then (
        MapLayoutInfo(pos = pos.toMapLayoutPos(map).let { mlp ->
            MapLayoutPos(mlp.x + mapTilePixels / 2, mlp.y + mapTilePixels / 2)
        }, layer = MapLayer.TEXT, center = true)
    )
}

@Composable
fun MapLayout(map: MapMetadata, routes: RoutesMetadata?,
              modifier: Modifier = Modifier, content: @Composable MapLayoutScope.() -> Unit) {
    val scope = MapLayoutScope(map = map, routes = routes)
    Layout(
        content = { scope.content() },
        modifier = modifier,
    ) { measurables, constraints ->
        val embiggen = Constraints(
            minWidth = constraints.minWidth,
            minHeight = constraints.minHeight,
            maxWidth = Constraints.Infinity,
            maxHeight = Constraints.Infinity,
        )
        //println("laying out map with (${constraints.maxHeight},${constraints.maxWidth}) ${constraints.hasBoundedHeight},${constraints.hasBoundedWidth} ${constraints.hasFixedWidth},${constraints.hasFixedHeight}")
        //println(" embiggening with with (${embiggen.maxHeight},${embiggen.maxWidth}) ${embiggen.hasBoundedHeight},${embiggen.hasBoundedWidth} ${embiggen.hasFixedWidth},${embiggen.hasFixedHeight}")
        layout(constraints.maxWidth, constraints.maxHeight) {
            for (item in measurables)
                (item.parentData as MapLayoutInfo).let { li ->
                    item.measure(embiggen).let { placeable ->
                        if (li.debug)
                            println("layout (${placeable.width},${placeable.height}) (${placeable.measuredWidth},${placeable.measuredHeight}) at ${li.pos}")
                        placeable.place(
                            x = li.pos.x - if (li.center) placeable.width / 2 else 0,
                            y = li.pos.y - if (li.center) placeable.height / 2 else 0,
                            zIndex = li.layer.zIndex
                        )
                    }
                }
        }
    }
}
