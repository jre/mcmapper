package net.joshe.mcmapper.metadata

import kotlin.math.max
import kotlin.math.min
import kotlinx.serialization.Required
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val magicId = "mcmapper root-level metadata"
const val rootMetaVersion = 3
const val mapTilePixels = 128

object WorldPaths {
    fun getTileBitmapPath(world: String, id: Int)
            = listOf("worlds", world, "tiles", "tile_${id}.png")
    fun getTileMetadataPath(world: String, map: String, pos: Pair<Int,Int>)
            = listOf("worlds", world, "maps", map, "tile_x${pos.first}_z${pos.second}.json")
    fun getMapMetadataPath(world: String, map: String)
            = listOf("worlds", world, "maps", map, "map.json")
    fun getWorldMetadataPath(world: String) = listOf("worlds", world, "world.json")
    fun getWorldRoutesPath(world: String) = listOf("worlds", world, "routes.json")
    fun getRootMetadataPath() = listOf("metadata.json")
}

@Serializable
data class TileMetadata(
    val id: Int,
    val x: Int,
    val z: Int,
    val icons: List<Icon>,
) {
    @Serializable
    sealed class Icon { abstract val x: Int; abstract val z: Int }

    @Serializable
    @SerialName("banner")
    data class Banner(override val x: Int, override val z: Int, val color: String, val label: String) : Icon()

    @Serializable
    @SerialName("pointer")
    data class Pointer(override val x: Int, override val z: Int, val rotation: Int) : Icon()
}

@Serializable
data class MapMetadata(
    val mapId: String,
    val label: String,
    val dimension: String,
    val tileSize: Int,
    val minX: Int,
    val maxX: Int,
    val minZ: Int,
    val maxZ: Int,
    val showRoutes: Boolean = false,
)

@Serializable
sealed class RouteNode {
    abstract val x: Int
    abstract val z: Int
    abstract val label: String
    fun getCoords() = Point(x, z)
}

@Serializable
@SerialName("gate")
data class GateNode(override val x: Int, override val z: Int, @SerialName("l") override val label: String) : RouteNode()

@Serializable
@SerialName("poi")
data class PoiNode(override val x: Int, override val z: Int, @SerialName("l") override val label: String) : RouteNode()

@Serializable
@SerialName("stop")
data class StopNode(override val x: Int, override val z: Int, @SerialName("l") override val label: String) : RouteNode()

@Serializable
data class Point(val x: Int, val z: Int)

@Serializable
data class RoutesMetadata(val nodes: Map<String, RouteNode>, val paths: List<List<Point>>) {
    val minX = min(nodes.values.minOf{it.x}, paths.minOfOrNull{p->p.minOf{it.x}}?:0)
    val minZ = min(nodes.values.minOf{it.z}, paths.minOfOrNull{p->p.minOf{it.z}}?:0)
    val maxX = max(nodes.values.maxOf{it.x}, paths.maxOfOrNull{p->p.maxOf{it.x}}?:0)
    val maxZ = max(nodes.values.maxOf{it.z}, paths.maxOfOrNull{p->p.maxOf{it.z}}?:0)
}

@Serializable
data class WorldMetadata(
    val maps: Map<String, String>,
    val defaultMap: String,
    val label: String,
    val worldId: String,
) {
    init { require(defaultMap in maps) }
}

@Serializable
data class RootMetadata(
    val worlds: Map<String, String>,
    val defaultWorld: String,
    @Required val identity: String = magicId,
    @Required val version: Int = rootMetaVersion,
) {
    init {
        require(identity == magicId)
        require(version == rootMetaVersion)
        require(defaultWorld in worlds)
    }
}
