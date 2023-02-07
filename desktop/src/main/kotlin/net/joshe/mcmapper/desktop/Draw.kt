package net.joshe.mcmapper.desktop

import net.joshe.mcmapper.mapdata.*
import java.awt.*
import java.awt.image.ImageObserver

data class PixelPos(val x: Int, val y: Int)

fun TilePos.toPixels(map: MapMetadata) : PixelPos {
    val worldPos = toWorldPosTopLeft(map)
    val edge = map.minVisibleWorldPos
    return PixelPos((worldPos.x - edge.x) / map.scaleFactor, (worldPos.z - edge.z) / map.scaleFactor)
}

fun WorldPos.toPixels(map: MapMetadata) = PixelPos(
    x = (x - map.minVisibleWorldPos.x) / map.scaleFactor,
    y = (z - map.minVisibleWorldPos.z) / map.scaleFactor,
)

fun BannerIcon.draw(g: Graphics, map: MapMetadata) {
    val w = 5
    val h = w * 2
    g.color = Color(color.r, color.g, color.b)
    val pp = (pos as WorldPos).toPixels(map)
    g.fillRect(pp.x - w / 2, pp.y - h / 2, w, h)
    g.color = Color.BLACK
    g.drawRect(pp.x - w / 2, pp.y - h / 2, w, h)
}

// https://docs.oracle.com/javase/7/docs/api/java/awt/geom/AffineTransform.html
fun PointerIcon.draw(g: Graphics, map: MapMetadata) {
    val pp = (pos as WorldPos).toPixels(map)
    val shape = Polygon()
    shape.addPoint(pp.x, pp.y - 5)
    shape.addPoint(pp.x + 3, pp.y + 5)
    shape.addPoint(pp.x - 3, pp.y + 5)
    g.color = Color.GREEN
    g.fillPolygon(shape)
    g.color = Color.BLACK
    g.drawPolygon(shape)
}

fun TileMetadata.draw(g: Graphics, map: MapMetadata, img: Image, observer: ImageObserver) {
    val px = pos.toPixels(map)
    //println("paint tile ${pos.x},${pos.z} at ${px.x},${px.y}")
    g.drawImage(img, px.x, px.y, observer)
}

// https://docs.oracle.com/javase/tutorial/2d/advanced/clipping.html
fun TileMetadata.drawId(g: Graphics, font: Font, map: MapMetadata, dark: Boolean) {
    val px = pos.toPixels(map)
    val idStr = id.toString()
    g.color = if (dark) Color.WHITE else Color.BLACK
    g.font = font
    g.drawString(idStr,
        px.x + mapTilePixels / 2 - g.fontMetrics.stringWidth(idStr) / 2,
        px.y + mapTilePixels / 2 - g.fontMetrics.height / 2 + g.fontMetrics.ascent)
    g.drawRect(px.x, px.y, mapTilePixels, mapTilePixels)
}

fun GateNode.draw(g: Graphics, map: MapMetadata, font: Font) {
    val radius = 4
    val px = (exitPos ?: pos.toWorldPos()).toPixels(map)
    g.color = Color.RED
    g.fillOval(px.x - radius, px.y - radius, radius * 2, radius * 2)
    g.color = Color.BLACK
    g.drawOval(px.x - radius, px.y - radius, radius * 2, radius * 2)
}

fun PoiNode.draw(g: Graphics, map: MapMetadata, font: Font) {
    val radius = 3
    val px = pos.toWorldPos().toPixels(map)
    g.color = Color.WHITE
    g.fillOval(px.x - radius, px.y - radius, radius * 2, radius * 2)
    g.color = Color.BLACK
    g.drawOval(px.x - radius, px.y - radius, radius * 2, radius * 2)
}

fun StopNode.draw(g: Graphics, map: MapMetadata, font: Font) {
    val radius = 3
    val px = pos.toWorldPos().toPixels(map)
    g.color = Color.BLACK
    g.fillOval(px.x - radius, px.y - radius, radius * 2, radius * 2)
}

fun RoutesMetadata.draw(g: Graphics, map: MapMetadata, font: Font) {
    g.color = Color.MAGENTA
    for (path in paths) {
        var prev = nodes.getValue(path.first).pos.toWorldPos().toPixels(map)
        for (netherPos in path.path) {
            val pixelPos = netherPos.toWorldPos().toPixels(map)
            g.drawLine(prev.x, prev.y, pixelPos.x, pixelPos.y)
            prev = pixelPos
        }
    }
    for (node in nodes.values) {
        when (node) {
            is StopNode -> node.draw(g, map, font)
            is PoiNode -> node.draw(g, map, font)
            is GateNode -> node.draw(g, map, font)
        }
    }
}
