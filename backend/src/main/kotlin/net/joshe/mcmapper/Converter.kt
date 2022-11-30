package net.joshe.mcmapper

import java.awt.image.BufferedImage
import java.awt.image.IndexColorModel
import java.awt.image.RenderedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.pow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.joshe.mcmapper.metadata.*

fun getRootMetaFile(rootDir: File)
        = File(rootDir, WorldPaths.getRootMetadataPath().joinToString(File.separator))

class WorldFiles(private val baseDir: File, val worldId: String) {
    private fun qualify(parts: List<String>) = File(baseDir, parts.joinToString(File.separator))

    fun getTilePngFile(id: Int) = qualify(WorldPaths.getTileBitmapPath(worldId, id))
    fun getTileMetaFile(map: String, pos: Pair<Int,Int>)
            = qualify(WorldPaths.getTileMetadataPath(worldId, map, pos))
    fun getMapMetaFile(map: String) = qualify(WorldPaths.getMapMetadataPath(worldId, map))
    fun getWorldMetaFile() = qualify(WorldPaths.getWorldMetadataPath(worldId))
    fun getRoutesFile() = qualify(WorldPaths.getWorldRoutesPath(worldId))
}

fun scaleFactor(scale: Int) = 2.0.pow(scale).toInt()

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
    val explored: Int,
    val t: TileMetadata,
    private val rawImage: ByteArray,
) {
    fun getPosition() : Pair<Int,Int> {
        val span = mapTilePixels * scaleFactor(scale)
        val roundedX = if (t.x < 0) t.x - span else t.x + span
        val roundedZ = if (t.z < 0) t.z - span else t.z + span
        return Pair(roundedX / span, roundedZ / span)
    }

    fun getImage() : RenderedImage {
        val img = BufferedImage(mapTilePixels, mapTilePixels, BufferedImage.TYPE_BYTE_INDEXED, getTileColorModel())
        img.raster.setPixels(0, 0, mapTilePixels, mapTilePixels, rawImage.map{it.toInt()}.toIntArray())
        return img
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
    }
}

fun readTiles(conf: WorldConf, srcDataPath: File): Map<String,Map<Pair<Int,Int>,ConverterTile>> {
    val count = readIdcounts(srcDataPath)
    val tiles = conf.maps.mapValues { mutableMapOf<Pair<Int,Int>,ConverterTile>() }

    for (id in 0 .. count) {
        val meta = readMap(srcDataPath, id) ?: continue
        val mapKey = conf.findMap(meta) ?: continue
        val pos = meta.getPosition()
	// XXX should use file mod date as a tiebreaker here
        if ((tiles.getValue(mapKey)[pos]?.explored ?: -1) < meta.explored)
            tiles.getValue(mapKey)[pos] = meta
    }

    return tiles
}

fun writeTiles(paths: WorldFiles, tiles: Map<String,Map<Pair<Int,Int>,ConverterTile>>) {
    for ((mapKey, map) in tiles.entries)
        for ((coords, tile) in map.entries) {
            paths.getTilePngFile(tile.t.id).withTempFile { pngFile ->
                ImageIO.write(tile.getImage(), "PNG", pngFile)
            }
            paths.getTileMetaFile(mapKey, coords).withTempFile { metaFile ->
                metaFile.writeText(Json.encodeToString(tile.t))
            }
        }
}

fun writeMapMetadata(conf: WorldConf, paths: WorldFiles, tiles: Map<String,Map<Pair<Int,Int>,ConverterTile>>) {
    for ((mapKey, mapConf) in conf.maps.entries) {
        val coords = tiles.getValue(mapKey).keys
        val mapMeta = MapMetadata(
            mapId = mapKey,
            label = mapConf.label,
            dimension = mapConf.dimension,
            tileSize = scaleFactor(mapConf.scale) * mapTilePixels,
            showRoutes = mapConf.routes,
            minX = coords.minOfOrNull{(x,_) -> x} ?: 0,
            maxX = coords.maxOfOrNull{(x,_) -> x} ?: 0,
            minZ = coords.minOfOrNull{(_,z) -> z} ?: 0,
            maxZ = coords.maxOfOrNull{(_,z) -> z} ?: 0,
        )
        paths.getMapMetaFile(mapKey).withTempFile { metaFile ->
            metaFile.writeText(Json.encodeToString(mapMeta))
        }
    }
}

fun convertWorld(conf: WorldConf, paths: WorldFiles) {
    val srcDataPath = File(conf.path, "data")
    val tiles = readTiles(conf, srcDataPath)

    writeTiles(paths, tiles)
    writeMapMetadata(conf, paths, tiles)

    paths.getRoutesFile().withTempFile { metaFile ->
        metaFile.writeText(Json.encodeToString(RoutesMetadata(
            nodes = conf.nodes,
            paths = conf.routePoints,
        )))
    }
    paths.getWorldMetaFile().withTempFile { metaFile ->
        metaFile.writeText(Json.encodeToString(WorldMetadata(
            worldId = paths.worldId,
            label = conf.label,
            defaultMap = conf.defaultMap,
            maps = conf.maps.mapValues { (_, map) -> map.label })))
    }
}

fun convertAllWorlds(allWorlds: AllWorldsConf, rootDir: File) {
    // XXX all json maps should be written with keys sorted
    for ((key, conf) in allWorlds.worlds) {
        println("writing world \"${key}\"")
        convertWorld(conf, WorldFiles(rootDir, key))
    }
    getRootMetaFile(rootDir).withTempFile { metaFile ->
        metaFile.writeText(Json.encodeToString(RootMetadata(
            defaultWorld = allWorlds.defaultWorld,
            worlds = allWorlds.worlds.mapValues { (_, world) -> world.label })))
    }
}
