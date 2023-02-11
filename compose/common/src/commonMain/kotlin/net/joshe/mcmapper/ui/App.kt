package net.joshe.mcmapper.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import net.joshe.mcmapper.mapdata.*

/*
https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#overview
https://developer.android.com/reference/kotlin/androidx/compose/material/package-summary#overview
https://foso.github.io/Jetpack-Compose-Playground/
https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/package-summary
https://developer.android.com/jetpack/compose/lists#lazy-grids
https://developer.android.com/jetpack/compose/graphics/images/loading
https://medium.com/falabellatechnology/jetpack-compose-canvas-8aee73eab393
https://betterprogramming.pub/custom-composable-with-jetpack-compose-912d8c53b810
https://avwie.github.io/mvi-architecture-in-compose-multiplatform
I imported this and it broke my project? https://github.com/alialbaali/Kamel#usage
https://www.netguru.com/blog/multiplatform-adaptive-ui

subclass lazylayout?
https://stackoverflow.com/questions/72076928/jetpack-compose-lazy-custom-layout
maybe start with a lazy grid first to see how it works
worth trying a canvas? what existing element has a viewport?
 */

/*
  TODO
    web version without wasm support
    handle window resizes in web
    automatically detect changed root metadata while still allowing manual reload
    fix scrolling
      you need to click inside the original viewport to drag it?
      you can't click outside the map bounds to drag
    don't clip routes to map tiles
    zooming
    persist options on web via cookies or browser local storage
    persist selected map
    determine dark mode based on time of day if unspecified
      expire manual dark mode after 12 hours
    show world coordinates under the mouse
    click on icons to see location
    click on route node to see routes ending there and distances
    show a single menu for selecting worlds and maps
    show a single menu for toggling map options
 */

data class DisplayOptions(
    val darkMode: MutableState<Boolean?>,
    val tileIds: MutableState<Boolean>,
    val pointers: MutableState<Boolean>,
    val banners: MutableState<Boolean>,
    val routes: MutableState<Boolean>,
)

@Composable
fun App(rootUrl: String,
        windowSizeState: State<DpSize>,
        options: DisplayOptions,
) {
    val mapState = rememberMapState(rootUrl)
    val scaffoldState = rememberScaffoldState()
    val menuSheetState = rememberMenuSheetState()
    val scope = rememberCoroutineScope()

    MaterialTheme(colors = if (options.darkMode.value == true) darkColors() else lightColors()) {
        MenuSheetLayout(state = menuSheetState) {
            Scaffold(
                scaffoldState = scaffoldState,
                bottomBar = {
                    BottomAppBar {
                        WorldSelectionButton(menuSheetState, mapState)
                        MapSelectionButton(menuSheetState, mapState)
                        OptionsButton(menuSheetState, options)
                        OutlinedButton(onClick = { mapState.currentWorld.value?.worldId?.let {
                            scope.launch { mapState.clientData.reloadWorldCache(it) }
                        }}) { Text(text = "Reload") }
                    }
                }) {
                MapGrid(
                    mapState = mapState,
                    windowSizeState = windowSizeState,
                    options = options)
            }
        }
    }
}

// https://stackoverflow.com/questions/67744381/jetpack-compose-scaffold-modal-bottom-sheet

@Composable
fun WorldSelectionButton(menuSheetState: MenuSheetState, mapState: RememberedMapState) {
    val scope = rememberCoroutineScope()
    val items = mapState.worldInfo.value.entries.sortedBy { it.value.label }

    MenuSheetButton(state = menuSheetState, text = "World") {
        if (items.isEmpty())
            Text(text = "No worlds loaded")
        else for ((itemId, item) in items)
            key(itemId) {
                MenuSheetRadioItem(state = menuSheetState,
                    selected = itemId == mapState.currentWorld.value?.worldId,
                    onClick = { scope.launch { mapState.clientData.selectWorld(itemId) } }
                ) {
                    Text(item.label)
                }
            }
    }
}

@Composable
fun MapSelectionButton(menuSheetState: MenuSheetState, mapState: RememberedMapState) {
    val scope = rememberCoroutineScope()
    val items = mapState.currentWorld.value?.maps?.entries?.sortedBy { it.value.label }

    MenuSheetButton(state = menuSheetState, text = "Maps") {
        if (items.isNullOrEmpty())
            Text(text = "No maps loaded")
        else for ((itemId, item) in items)
            key(itemId) {
                MenuSheetRadioItem(state = menuSheetState,
                    selected = itemId == mapState.currentMap.value?.mapId,
                    onClick = { scope.launch { mapState.clientData.selectMap(itemId) } }
                ) {
                    Text(item.label)
                }
            }
    }
}

@Composable
fun OptionsButton(menuSheetState: MenuSheetState, opts: DisplayOptions) {
    MenuSheetButton(state = menuSheetState, text = "Options") {
        MenuSheetCheckItem(state = menuSheetState, selected = opts.darkMode.value == true,
            onChange = { value -> opts.darkMode.value = value }) {
            Text("Dark mode")
        }
        MenuSheetCheckItem(state = menuSheetState, selected = opts.tileIds.value,
            onChange = { value -> opts.tileIds.value = value }) {
            Text("Show map IDs")
        }
        MenuSheetCheckItem(state = menuSheetState, selected = opts.pointers.value,
            onChange = { value -> opts.pointers.value = value }) {
            Text("Show pointers")
        }
        MenuSheetCheckItem(state = menuSheetState, selected = opts.banners.value,
            onChange = { value -> opts.banners.value = value }) {
            Text("Show banners")
        }
        MenuSheetCheckItem(state = menuSheetState, selected = opts.routes.value,
            onChange = { value -> opts.routes.value = value }) {
            Text("Show routes")
        }
    }
}

@Composable
fun MapTilePixmap(mapState: RememberedMapState, tile: TileMetadata, modifier: Modifier = Modifier) {
    var pixmap: ByteArray? by remember(mapState.currentWorld.value, tile) { mutableStateOf(null) }
    val mod = modifier.requiredSize(width = mapTilePixels.dp, height = mapTilePixels.dp)

    pixmap.let { pixmapBytes ->
        if (pixmapBytes != null)
            Image(
                contentDescription = "${tile.id}",
                bitmap = pixmapBytes.decodeToImageBitmap(),
                modifier = mod)
        else {
            Image(
                contentDescription = "${tile.id}",
                painter = ColorPainter(color = Color.Transparent),
                modifier = mod)
            LaunchedEffect(mapState.currentWorld) { pixmap = mapState.clientData.loadTilePixmap(tile) }
        }
    }
}

@Composable
fun MapIcon(icon: Icon, options: DisplayOptions, modifier: Modifier = Modifier) {
    if (when (icon) {
            is PointerIcon -> options.pointers.value
            is BannerIcon -> options.banners.value
        })
        Image(
            painter = rememberVectorPainter(image = icon.getImage()),
            contentDescription = icon.toString(),
            modifier = modifier)
    // XXX icon label
}

@Composable
fun MapId(tile: TileMetadata, modifier: Modifier = Modifier) {
    // https://handstandsam.com/2021/08/09/jetpack-compose-text-shadows/
    Text(text = tile.id.toString(),
        modifier = modifier,
        color = MaterialTheme.colors.error,
        style = TextStyle.Default.copy(shadow = Shadow(
            color = MaterialTheme.colors.onBackground,
            //offset = Offset(4f, 4f),
            blurRadius = 1f
        )))
}

@Composable
fun MapRouteNode(node: RouteNode, modifier: Modifier = Modifier) {
    val img = remember { node.getImage() }
    if (img != null)
        Image(
            painter = rememberVectorPainter(image = img),
            contentDescription = node.label,
            modifier = modifier)
    // XXX label
}

@Composable
fun MapRoutePaths(routes: RoutesMetadata, map: MapMetadata, modifier: Modifier = Modifier) {
    val img = rememberVectorPainter(image = getRoutesImage(routes, map))
    Image(
        painter = img,
        contentDescription = null,
        modifier = modifier,
    )
}

// https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/layout/package-summary

operator fun DpOffset.plus(delta: Offset) = DpOffset(x + delta.x.dp, y + delta.y.dp)

fun DpOffset.toIntOffset() = IntOffset(x.value.toInt(), y.value.toInt())

// https://engineering.monstar-lab.com/en/post/2022/08/04/Compose-custom-layouts-part2/

@Composable
fun MapGrid(
    mapState: RememberedMapState,
    options: DisplayOptions,
    windowSizeState: State<DpSize>,
) {
    val worldMeta = mapState.currentWorld.value
    val mapMeta = mapState.currentMap.value
    println("drawing map grid with world=${worldMeta?.worldId} map=${mapMeta?.mapId}")
    if (worldMeta == null || mapMeta == null) {
        Text("No map loaded")
        return
    }
    var pos by remember(worldMeta.worldId, mapMeta.mapId, windowSizeState.value) {
        mutableStateOf(windowSizeState.value.center - mapMeta.mapSize().center)
    }

    // XXX if I put the pointerInput modifier here then it stops working after I switch maps and back
    MapLayout(
        map = mapMeta,
        routes = if (mapMeta.showRoutes && options.routes.value) worldMeta.routes else null,
        modifier = Modifier.offset { pos.toIntOffset() },
    ) {
        for (tilePos in mapMeta.minPos .. mapMeta.maxPos)
            mapMeta.tiles[tilePos]?.let { tile ->
                key(tilePos) {
                    MapTilePixmap(mapState = mapState, tile = tile,
                        modifier = Modifier
                            .tilePosition(tilePos)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    pos += dragAmount
                                }
                            })
                }
                if (options.tileIds.value)
                    MapId(tile, Modifier.tileIdPosition(tilePos))
                tile.icons.forEachIndexed { idx, icon ->
                    key(tilePos, idx) {
                        MapIcon(icon, options = options, modifier = Modifier.iconPosition(icon))
                    }
                }
            }
        if (mapMeta.showRoutes && options.routes.value) {
            for (node in worldMeta.routes.nodes.values)
            //if (NetherPos(node.x, node.z).toMapLayoutPos(mapMeta).let { it.x >= 0 && it.y >= 0 })
                MapRouteNode(node, modifier = Modifier.routeNodePosition(node))
            MapRoutePaths(worldMeta.routes, mapMeta, modifier = Modifier.routeGlobalOverlayPosition())
        }
    }
}
