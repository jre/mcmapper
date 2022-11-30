package net.joshe.mcmapper

import java.io.File
import java.io.FileInputStream
import me.nullicorn.nedit.NBTReader
import me.nullicorn.nedit.type.NBTCompound
import net.joshe.mcmapper.metadata.*

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

private fun readTileMetadata(id: Int, tag: NBTCompound) : ConverterTile? {
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
    val colors = tag.getByteArray("colors")

    val icons = mutableListOf<TileMetadata.Icon>()
    tag.getList("frames")?.forEach { frame ->
        if (frame is NBTCompound) {
            val x = frame.getNumber("Pos.X", null)
            val z = frame.getNumber("Pos.Z", null)
            val rot = frame.getInt("Rotation", 0)
            if (x != null && z != null)
                icons.add(TileMetadata.Pointer(x = x.toInt(), z = z.toInt(), rotation = rot))
        }
    }
    tag.getList("banners")?.forEach { banner ->
        if (banner is NBTCompound) {
            val x = banner.getNumber("Pos.X", null)
            val z = banner.getNumber("Pos.Z", null)
            val color = banner.getString("Color", null)
            val label = banner.getString("Name", "")
            if (x != null && z != null && color != null)
                icons.add(TileMetadata.Banner(x = x.toInt(), z = z.toInt(), color = color, label = label))
        }
    }

    val image = tag.getByteArray("colors")
    if (image.size != mapTilePixels * mapTilePixels)
        return null

    return ConverterTile(
        t = TileMetadata(
            id = id,
            x = tag.getInt("xCenter", 0),
            z = tag.getInt("zCenter", 0),
            icons = icons),
        rawImage = image,
        scale = tag.getInt("scale", 0),
        dimension = dimension,
        explored = colors.size - colors.count{it == unexploredColor })
}

fun readMap(mapDir: File, id: Int) : ConverterTile? {
    require(id >= 0)
    val stream = FileInputStream(File(mapDir, "map_${id}.dat"))
    val root = NBTReader.read(stream)
    stream.close()
    return readTileMetadata(id, root.getCompound("data"))
}
