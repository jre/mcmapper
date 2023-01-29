package net.joshe.mcmapper

import io.ktor.http.*
import io.ktor.util.date.GMTDate
import java.awt.image.BufferedImage
import java.awt.image.IndexColorModel
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.joshe.mcmapper.mapdata.*

fun getRootMetaFile(rootDir: File)
        = File(rootDir, WorldPaths.getRootMetadataPath().joinToString(File.separator))

class WorldFiles(private val baseDir: File, val worldId: String) {
    private fun qualify(parts: List<String>) = File(baseDir, parts.joinToString(File.separator))

    fun getTilePngFile(id: Int) = qualify(WorldPaths.getTileBitmapPath(worldId, id))
    fun getWorldMetaFile() = qualify(WorldPaths.getWorldMetadataPath(worldId))
}

private var colorModel: IndexColorModel? = null

fun getTileColorModel() : IndexColorModel {
    if (colorModel == null) {
        val colors = baseColors.flatMap { it.expand() }.toByteArray()
        colorModel = IndexColorModel(8, colors.size / 4, colors, 0, true)
    }
    return colorModel!!
}

class ConverterTile(
    val scale: Int,
    val dimension: String,
    val t: TileMetadata,
    val srcModified: GMTDate,
    val pngData: ByteArray?,
)

fun getTilePos(pos: Position, scale: Int) : TilePos {
    val span = mapTilePixels * scaleFactor(scale)
    val skewedX = pos.x + mapTileOffset
    val skewedZ = pos.z + mapTileOffset
    val kludgedX = if (skewedX < 0) skewedX - span - 1 else skewedX
    val kludgedZ = if (skewedZ < 0) skewedZ - span - 1 else skewedZ
    return TilePos(kludgedX / span, kludgedZ / span)
}

fun MapMetadata.converterTile(tile: TileMetadata, srcModified: GMTDate) = ConverterTile(
    scale = scale,
    dimension = dimension,
    t = tile,
    pngData = null,
    srcModified = srcModified,
)

fun getTilePngData(rawImage: ByteArray) : ByteArray {
    val img = BufferedImage(mapTilePixels, mapTilePixels, BufferedImage.TYPE_BYTE_INDEXED, getTileColorModel())
    img.raster.setPixels(0, 0, mapTilePixels, mapTilePixels, rawImage.map { it.toInt() }.toIntArray())
    return ByteArrayOutputStream().let { out ->
        ImageIO.write(img, "PNG", out)
        out.toByteArray()
    }
}

fun WorldConf.findMap(tile: ConverterTile) : String? {
    for ((key, map) in maps.entries)
        if (tile.scale == map.scale && tile.dimension == map.dimension)
            return key
    return null
}

fun File.withTempFile(writer: (File) -> Unit) {
    val tmp = File(parentFile, ".tmp-${name}")
    tmp.delete()
    try {
        parentFile.mkdirs()
        writer(tmp)
        tmp.renameTo(this)
    } catch (e: Throwable) {
        tmp.delete()
        throw e
    }
}

fun readTiles(conf: WorldConf, srcDataPath: File, paths: WorldFiles, oldMeta: WorldMetadata?)
        : Map<String,Map<TilePos,ConverterTile>> {
    val count = readIdcounts(srcDataPath)
    val tiles = conf.maps.mapValues { mutableMapOf<TilePos,ConverterTile>() }
    val idCache = mutableMapOf<Int, ConverterTile>()

    oldMeta?.maps?.values?.forEach { map ->
        tiles[map.mapId]?.putAll(map.tiles.entries.map { (pos, tile) ->
            Pair(pos, map.converterTile(tile, mapTileSrcFile(srcDataPath, tile.id).lastModifiedGMTDate()))
        })
        tiles[map.mapId]?.forEach { (_, tile) ->
            idCache[tile.t.id] = tile
        }
    }

    for (id in 0 .. count) {
        val meta = readMap(srcDataPath, id, paths, idCache) ?: continue
        val mapKey = conf.findMap(meta) ?: continue
        val duplicate = tiles.getValue(mapKey)[meta.t.pos]
        if (duplicate == null || duplicate.t.id == meta.t.id || duplicate.t.hidden > meta.t.hidden ||
            (duplicate.t.hidden == meta.t.hidden && duplicate.srcModified < meta.srcModified))
            tiles.getValue(mapKey)[meta.t.pos] = meta
    }

    return tiles
}

fun writeTilePngs(paths: WorldFiles, converterTiles: Map<String,Map<TilePos,ConverterTile>>)
        : Map<String, Map<TilePos, TileMetadata>> {
    var tileCount = 0
    var updatedTiles = 0
    val newTiles = converterTiles.mapValues { (_, map) ->
        map.mapValues { (_, tile) ->
            tileCount++
            if (tile.pngData == null)
                tile.t
            else {
                updatedTiles++
                val tilePngFile = paths.getTilePngFile(tile.t.id)
                tilePngFile.withTempFile { it.writeBytes(tile.pngData) }
                tile.t.copy(modified = tilePngFile.lastModifiedGMTDate())
            }
        }
    }
    println("wrote ${updatedTiles}/${tileCount} tile images")
    return newTiles
}

fun createMapMetadata(conf: WorldConf, tiles: Map<String,Map<TilePos,TileMetadata>>)
        = conf.maps.mapValues { (mapKey, mapConf) ->
    val mapTiles = tiles.getValue(mapKey)
    val coords = mapTiles.keys
    MapMetadata(
        mapId = mapKey,
        label = mapConf.label,
        dimension = mapConf.dimension,
        scale = mapConf.scale,
        showRoutes = mapConf.routes,
        minPos = TilePos(coords.minOfOrNull{(x,_) -> x} ?: 0, coords.minOfOrNull{(_,z) -> z} ?: 0),
        maxPos = TilePos(coords.maxOfOrNull{(x,_) -> x} ?: 0, coords.maxOfOrNull{(_,z) -> z} ?: 0),
        tiles = mapTiles,
    )
}

fun convertWorld(conf: WorldConf, paths: WorldFiles, oldMeta: WorldMetadata?) : WorldMetadata {
    val srcDataPath = File(conf.path, "data")
    val converterTiles = readTiles(conf, srcDataPath, paths, oldMeta)
    val worldTiles = writeTilePngs(paths, converterTiles)

    val worldMetaFile = paths.getWorldMetaFile()
    val newMeta = WorldMetadata(
        worldId = paths.worldId,
        label = conf.label,
        defaultMap = conf.defaultMap,
        maps = createMapMetadata(conf, worldTiles),
        routes = RoutesMetadata(nodes = conf.nodes, paths = conf.routePaths),
    )
    if (oldMeta == newMeta)
        println("world metadata unchanged, not writing ${worldMetaFile}")
    else
        worldMetaFile.withTempFile { metaFile ->
            println("writing updated world metadata to ${worldMetaFile}")
            metaFile.writeText(Json.encodeToString(newMeta))
        }

    return newMeta
}

inline fun <reified T> readOldMeta(path: File, label: String) : T? =
    if (!path.exists()) {
        println("existing ${label} metadata file not found at ${path}")
        null
    } else try {
        Json.decodeFromString<T>(path.readText())
    } catch (e: SerializationException) {
        println("failed to deserialize existing ${label} metadata at ${path}: ${e}")
        null
    }


fun convertAllWorlds(allWorlds: AllWorldsConf, rootDir: File) {
    // XXX all json maps should be written with keys sorted
    val worldStubs = mutableMapOf<String, RootMetadata.WorldStub>()
    val rootMetaFile = getRootMetaFile(rootDir)
    val oldRootMeta = readOldMeta<RootMetadata>(rootMetaFile, "root")

    for ((key, conf) in allWorlds.worlds) {
        val paths = WorldFiles(rootDir, key)
        val worldMetaFile = paths.getWorldMetaFile()
        val oldWorldMeta = readOldMeta<WorldMetadata>(worldMetaFile, "world")

        println("converting world \"${key}\"")
        val worldMeta = convertWorld(conf, paths, oldWorldMeta)
        worldStubs[key] = RootMetadata.WorldStub(
            label = worldMeta.label,
            modified = worldMetaFile.lastModifiedGMTDate(),
        )
    }

    val newRootMeta = RootMetadata(
        defaultWorld = allWorlds.defaultWorld,
        worlds = worldStubs,
    )
    if (oldRootMeta == newRootMeta)
        println("root metadata unchanged, not writing ${rootMetaFile}")
    else
        rootMetaFile.withTempFile { metaFile ->
            println("writing updated root metadata to ${rootMetaFile}")
            metaFile.writeText(Json.encodeToString(newRootMeta))
        }
}
