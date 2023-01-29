package net.joshe.mcmapper

import io.ktor.util.date.GMTDate
import java.io.File
import java.io.FileInputStream
import me.nullicorn.nedit.NBTReader
import me.nullicorn.nedit.type.NBTCompound
import net.joshe.mcmapper.mapdata.*

fun File.lastModifiedGMTDate() = GMTDate(timestamp = lastModified())

fun readIdcounts(mapDir: File): Int {
    val root = NBTReader.readFile(File(mapDir, "idcounts.dat"))
    return if (root.getInt("DataVersion", 0) < 1926)
        root.getInt("map", -1)
    else
        root.getInt("data.map", -1)
}

private val dimensionIds: Map<Int, String> = mapOf(
    0 to "minecraft:overworld",
    -1 to "minecraft:the_nether",
    1 to "minecraft:the_end"
)

private fun readTileMetadata(id: Int, tag: NBTCompound, nbtModified: GMTDate, pngModified: GMTDate) : ConverterTile? {
    require("xCenter" in tag && "zCenter" in tag && "scale" in tag &&
            "dimension" in tag && "colors" in tag)
    if (tag.getInt("unlimitedTracking", 0) == 1)
        return null
    val dimension = tag.getNumber("dimension", null).let { dimensionId ->
        if (dimensionId == null)
            tag.getString("dimension")
        else
            dimensionIds.getOrDefault(dimensionId.toInt(), null)
    } ?: return null

    val icons = mutableListOf<Icon>()
    tag.getList("frames")?.forEach { frame ->
        if (frame is NBTCompound) {
            val x = frame.getNumber("Pos.X", null)
            val z = frame.getNumber("Pos.Z", null)
            val rot = frame.getInt("Rotation", 0)
            if (x != null && z != null)
                icons.add(PointerIcon(dimensionalPosition(dimension, x = x.toInt(), z = z.toInt()),
                    rotation = rot))
        }
    }
    tag.getList("banners")?.forEach { banner ->
        if (banner is NBTCompound) {
            val x = banner.getNumber("Pos.X", null)
            val z = banner.getNumber("Pos.Z", null)
            val color = banner.getString("Color", null)
            val label = banner.getString("Name", "")
            if (x != null && z != null && color != null)
                icons.add(BannerIcon(dimensionalPosition(dimension, x = x.toInt(), z = z.toInt()),
                    color = color, label = label))
        }
    }

    val image = tag.getByteArray("colors")
    if (image.size != mapTilePixels * mapTilePixels)
        return null

    val scale = tag.getInt("scale", 0)
    val pos = getTilePos(dimensionalPosition(dimension,
        x = tag.getInt("xCenter", 0),
        z = tag.getInt("zCenter", 0)), scale)

    return ConverterTile(
        t = TileMetadata(
            id = id,
            pos = pos,
            icons = icons,
            modified = pngModified,
            hidden = image.count { it == unexploredColor },
        ),
        pngData = if (pngModified > nbtModified) null else getTilePngData(image),
        scale = scale,
        dimension = dimension,
        srcModified = nbtModified,
    )
}

fun mapTileSrcFile(mapDir: File, id: Int) = File(mapDir, "map_${id}.dat")

fun readMap(mapDir: File, id: Int, paths: WorldFiles, tileCache: Map<Int, ConverterTile>) : ConverterTile? {
    require(id >= 0)
    val pngMod = paths.getTilePngFile(id).lastModifiedGMTDate()
    val mapFile = mapTileSrcFile(mapDir, id)
    val nbtMod = mapFile.lastModifiedGMTDate()
    if (nbtMod < pngMod && id in tileCache)
        return tileCache.getValue(id)
    val stream = FileInputStream(mapFile)
    val root = NBTReader.read(stream)
    stream.close()
    return readTileMetadata(id, root.getCompound("data"), nbtMod, pngMod)
}
