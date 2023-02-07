package net.joshe.mcmapper.mapdata

import io.ktor.util.date.*
import kotlin.math.max
import kotlin.math.min
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.math.pow

const val magicId = "mcmapper root-level metadata"
const val rootMetaVersion = 4
const val worldMetaVersion = 5
const val mapTilePixels = 128
const val mapTileOffset = mapTilePixels / 2

object WorldPaths {
    fun getTileBitmapPath(world: String, id: Int)
            = listOf("worlds", world, "tiles", "tile_${id}.png")
    fun getWorldMetadataPath(world: String) = listOf("worlds", world, "world.json")
    fun getRootMetadataPath() = listOf("metadata.json")
}

enum class BannerColor(val key: String, val r: Int, val g: Int, val b: Int) {
    WHITE("white", 255, 255, 255),
    ORANGE("orange", 216, 127, 51),
    MAGENTA("magenta", 178, 76, 216),
    LIGHTBLUE("light_blue", 102, 153, 216),
    YELLOW("yellow", 229, 229, 51),
    LIME("lime", 127, 204, 25),
    PINK("pink", 242, 127, 165),
    GRAY("gray", 76, 76, 76),
    LIGHTGRAY("light_gray", 153, 153, 153),
    CYAN("cyan", 76, 127, 153),
    PURPLE("purple", 127, 63, 178),
    BLUE("blue", 51, 76, 178),
    BROWN("brown", 102, 76, 51),
    GREEN("green", 102, 127, 51),
    RED("red", 153, 51, 51),
    BLACK("black", 25, 25, 25),
    ;

    companion object {
        fun fromString(name: String) = BannerColor.values().find { it.key == name }
    }
}

fun dimensionalPosition(dimension: String, x: Int, z: Int) =
    if (dimension == "minecraft:the_nether") NetherPos(x, z) else WorldPos(x, z)

fun scaleFactor(scale: Int) = 2.0.pow(scale).toInt()

@Serializable
sealed class Position : Comparable<Position> {
    abstract val x: Int
    abstract val z: Int

    override fun compareTo(other: Position) = x.compareTo(other.x).let { cmp ->
        if (cmp != 0) cmp else z.compareTo(other.z)
    }
}

@Serializable
@SerialName("tile")
data class TilePos(override val x: Int, override val z: Int) : Position() {
    operator fun rangeTo(that: TilePos) = TilePosProgression(that, this)

    class TilePosIterator(private val start: TilePos, private val endInclusive: TilePos) : Iterator<TilePos> {
        private var started = false
        private var currentX = start.x
        private var currentZ = start.z

        override fun hasNext() = !started || (currentX <= endInclusive.x && currentZ <= endInclusive.z)

        override fun next(): TilePos {
            if (!started)
                started = true
            else if (currentX < endInclusive.x)
                currentX++
            else {
                currentX = start.x
                currentZ++
            }
            return TilePos(currentX, currentZ)
        }
    }

    class TilePosProgression(override val endInclusive: TilePos, override val start: TilePos)
        : Iterable<TilePos>, ClosedRange<TilePos> {
        override fun iterator() = TilePosIterator(start, endInclusive)
    }
}

@Serializable
@SerialName("world")
data class WorldPos(override val x: Int, override val z: Int) : Position()

@Serializable
@SerialName("nether")
data class NetherPos(override val x: Int, override val z: Int) : Position()

@Serializable
sealed class Icon { abstract val pos: Position }

@Serializable
@SerialName("banner")
data class BannerIcon(
    override val pos: Position,
    @Serializable(with = BannerColorAsStringSerializer::class) val color: BannerColor,
    val label: String) : Icon()

@Serializable
@SerialName("pointer")
data class PointerIcon(override val pos: Position, val rotation: Int) : Icon()

@Serializable
data class TileMetadata(
    val id: Int,
    val pos: TilePos,
    val hidden: Int,
    @Serializable(with = GMTDateAsLongSerializer::class)
    val modified: GMTDate,
    val icons: List<Icon>,
)

@Serializable
data class MapMetadata(
    val mapId: String,
    val label: String,
    val scale: Int,
    val dimension: String,
    val minPos: TilePos,
    val maxPos: TilePos,
    val showRoutes: Boolean = false,
    @Serializable(with = TileMapListSerializer::class)
    val tiles: Map<TilePos, TileMetadata>,
) {
    val scaleFactor = scaleFactor(scale)
    val tileSize = scaleFactor * mapTilePixels
}

@Serializable
sealed class RouteNode { abstract val pos: NetherPos; abstract val label: String; }

@Serializable
@SerialName("gate")
data class GateNode(override val pos: NetherPos, override val label: String,
                    val exitPos: WorldPos? = null) : RouteNode()

@Serializable
@SerialName("poi")
data class PoiNode(override val pos: NetherPos, override val label: String) : RouteNode()

@Serializable
@SerialName("stop")
data class StopNode(override val pos: NetherPos, override val label: String) : RouteNode()

@Serializable
data class RoutePath(val first: String, val last: String, val path: List<NetherPos>)

@Serializable
data class RoutesMetadata(val nodes: Map<String, RouteNode>, val paths: List<RoutePath>) {
    val minPos = NetherPos(
        min(nodes.values.minOfOrNull{it.pos.x}?:0, paths.minOfOrNull{rp->rp.path.minOf{it.x}}?:0),
        min(nodes.values.minOfOrNull{it.pos.z}?:0, paths.minOfOrNull{rp->rp.path.minOf{it.z}}?:0))
    val maxPos = NetherPos(
        max(nodes.values.maxOfOrNull{it.pos.x}?:0, paths.maxOfOrNull{rp->rp.path.maxOf{it.x}}?:0),
        max(nodes.values.maxOfOrNull{it.pos.z}?:0, paths.maxOfOrNull{rp->rp.path.maxOf{it.z}}?:0))
}

@Serializable
data class WorldMetadata(
    val maps: Map<String, MapMetadata>,
    val routes: RoutesMetadata,
    val defaultMap: String,
    val label: String,
    val worldId: String,
    @Required val version: Int = worldMetaVersion,
) {
    init {
        require(version == worldMetaVersion)
        require(defaultMap in maps)
    }
}

@Serializable
data class RootMetadata(
    val worlds: Map<String, WorldStub>,
    val defaultWorld: String? = null,
    @Required val identity: String = magicId,
    @Required val version: Int = rootMetaVersion,
) {
    init {
        require(identity == magicId)
        require(version == rootMetaVersion)
        require(defaultWorld == null || defaultWorld in worlds)
    }

    @Serializable
    data class WorldStub(
        val label: String,
        @Serializable(with = GMTDateAsLongSerializer::class)
        val modified: GMTDate,
    )
}

object TileMapListSerializer : KSerializer<Map<TilePos, TileMetadata>> {
    private val delegateSerializer = ListSerializer<TileMetadata>(TileMetadata.serializer())
    @OptIn(ExperimentalSerializationApi::class)
    override val descriptor = SerialDescriptor("Map<TilePos,TileMetadata>", delegateSerializer.descriptor)

    override fun serialize(encoder: Encoder, value: Map<TilePos,TileMetadata>) =
        encoder.encodeSerializableValue(delegateSerializer, value.values.toList())

    override fun deserialize(decoder: Decoder) =
        decoder.decodeSerializableValue(delegateSerializer).associateBy { tile -> tile.pos }
}

object GMTDateAsLongSerializer : KSerializer<GMTDate> {
    override val descriptor = PrimitiveSerialDescriptor("GMTDate", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder) =
        GMTDate(timestamp = decoder.decodeLong()).truncateToSeconds()

    override fun serialize(encoder: Encoder, value: GMTDate) =
        encoder.encodeLong(value.timestamp)
}

object BannerColorAsStringSerializer : KSerializer<BannerColor> {
    override val descriptor = PrimitiveSerialDescriptor("BannerColor", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder) = BannerColor.fromString(decoder.decodeString())!!

    override fun serialize(encoder: Encoder, value: BannerColor) = encoder.encodeString(value.key)
}
