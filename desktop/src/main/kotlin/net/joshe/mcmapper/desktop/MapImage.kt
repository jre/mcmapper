package net.joshe.mcmapper.desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.joshe.mcmapper.mapdata.*
import java.awt.*
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.swing.*
import net.joshe.mcmapper.common.ClientData

class MapImage(private val clientData: ClientData, private val opts: MapDisplayOptions) : Scrollable, JPanel() {
    private val images = mutableMapOf<TilePos, Image>()
    private val scope = CoroutineScope(Dispatchers.Main)
    private var cachedSize: Dimension? = null
    private val idFont = Font("Default", Font.BOLD, 12)
    private val labelFont = Font("Default", Font.PLAIN, 8)

    init {
        scope.launch { opts.darkMode.collect { repaint() } }
        scope.launch { opts.tileIds.collect { repaint() } }
        scope.launch { opts.pointers.collect { repaint() } }
        scope.launch { opts.banners.collect { repaint() } }
        scope.launch { opts.routes.collect { repaint() } }

        scope.launch {
            clientData.currentMap.collect { map ->
                images.clear()
                cachedSize = null
                if (map != null)
                    for (pos in map.minPos..map.maxPos)
                        map.tiles[pos]?.let { tile ->
                            scope.launch {
                                clientData.loadTilePixmap(tile)?.let { png ->
                                    println("read tile ${pos.x},${pos.z} as ${png.size} byte png")
                                    images[tile.pos] = ImageIO.read(ByteArrayInputStream(png))
                                    repaint()
                                }
                            }
                        }
                revalidate()
                repaint()
            }
        }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        clientData.currentMap.value?.let { map ->
            for (tile in map.tiles.values) {
                images[tile.pos]?.let { tile.draw(g, map, it, this) }
                if (opts.tileIds.value)
                    tile.drawId(g, idFont, map, opts.darkMode.value == true)
                tile.icons.forEach { icon ->
                    when (icon) {
                        is BannerIcon -> if (opts.banners.value) icon.draw(g, map)
                        is PointerIcon -> if (opts.pointers.value) icon.draw(g, map)
                    }
                }
            }
            if (map.showRoutes && opts.routes.value)
                clientData.currentWorld.value?.routes?.draw(g, map, labelFont)
        }
    }

    override fun getPreferredSize() : Dimension {
        if (cachedSize == null) {
            clientData.currentMap.value?.let { map ->
                val max = map.maxVisibleWorldPos
                val min = map.minVisibleWorldPos
                cachedSize = Dimension((max.x - min.x) / map.scaleFactor,
                    (max.z - min.z) / map.scaleFactor)
            }
        }
        return cachedSize ?: Dimension(mapTilePixels, mapTilePixels)
    }

    override fun getPreferredScrollableViewportSize() = preferredSize
    override fun getScrollableUnitIncrement(visibleRect: Rectangle?, orientation: Int, direction: Int) = 1
    override fun getScrollableBlockIncrement(visibleRect: Rectangle?, orientation: Int, direction: Int) = mapTilePixels
    override fun getScrollableTracksViewportWidth() = false
    override fun getScrollableTracksViewportHeight() = false
}
