package net.joshe.mcmapper.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.launch
import net.joshe.mcmapper.data.*
import net.joshe.mcmapper.data.State
import net.joshe.mcmapper.metadata.*
import org.jetbrains.skia.Image as SkiaImage

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
    detect changed tiles and bypass browser cache
      metadata too
    fix scrolling
      you need to click inside the original viewport to drag it?
      you can't click outside the map bounds to drag
    draw routes
    zooming
    merge mcmapper server code
    handle merging multiple sets of worlds on the webserver
    untangle State class
      move caching into composables
      move fetching functions into Client
      simplify State class and eliminate if possible
    persist options on web via cookies or browser local storage
    persist selected map
    determine dark mode based on time of day if unspecified
      expire manual dark mode after 12 hours
    show world coordinates under the mouse
    click on icons to see location
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
        windowSizeState: androidx.compose.runtime.State<DpSize>,
        options: DisplayOptions,
) {
    val baseUrl = rootUrl.removeSuffix("/") + "/data"
    val mapState = remember(baseUrl) { State(baseUrl) }
    val scaffoldState = rememberScaffoldState()
    val menuSheetState = rememberMenuSheetState()
    val scope = rememberCoroutineScope()

    if (mapState.worlds.value.isEmpty())
        scope.launch { mapState.loadRootData() }

    MaterialTheme(colors = if (options.darkMode.value == true) darkColors() else lightColors()) {
        MenuSheetLayout(state = menuSheetState) {
            Scaffold(
                scaffoldState = scaffoldState,
                bottomBar = {
                    BottomAppBar {
                        MenuSheetButton(
                            state = menuSheetState,
                            items = mapState.worlds.value,
                            selectedKey = mapState.currentMap.value.worldId,
                            noneSelected = "Select a world",
                            onSelect = { selection ->
                                scope.launch { mapState.selectWorld(selection) }
                            })
                        MenuSheetButton(
                            state = menuSheetState,
                            items = mapState.worldMaps.value,
                            selectedKey = mapState.currentMap.value.mapId,
                            noneSelected = "Select a map",
                            onSelect = { selection ->
                                scope.launch { mapState.selectMap(selection) }
                            })
                        DarkModeButton(options.darkMode)
                        ShowTileIdsButton(options.tileIds)
                        ShowPointersButton(options)
                        ShowRoutesButton(options.routes)
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
fun DarkModeButton(darkMode: MutableState<Boolean?>) {
    println("drawing dark mode button with state ${darkMode.value}")
    OutlinedButton(
        onClick = {
                println("clicked dark mode ${darkMode.value} -> ${darkMode.value != true}")
                darkMode.value = darkMode.value != true
        },
    ) {
        Text(text = if (darkMode.value == true) "Dark mode" else "Light mode")
    }
}

@Composable
fun ShowTileIdsButton(state: MutableState<Boolean>) {
    OutlinedButton(onClick = { state.value = !state.value }) {
        Text(text = if (state.value) "Hide Map IDs" else "Show Map IDs")
    }
}

@Composable
fun ShowPointersButton(options: DisplayOptions) {
    OutlinedButton(onClick = {
        options.banners.value = !options.pointers.value
        options.pointers.value = options.banners.value
    }) {
        Text(text = if (options.pointers.value) "Hide Pointers" else "Show Pointers")
    }
}

@Composable
fun ShowRoutesButton(state: MutableState<Boolean>) {
    OutlinedButton(onClick = { state.value = !state.value }) {
        Text(text = if (state.value) "Hide Routes" else "Show Routes")
    }
}
@Composable
fun MapTileBox(mapState: State, pos: TilePos, content: @Composable (TileMetadata) -> Unit) {
    var meta: TileMetadata? by remember(mapState.currentMap) { mutableStateOf(null) }
    meta?.also { content(it) } ?: LaunchedEffect(mapState.currentMap) { meta = mapState.loadTileMetadata(pos) }
}

@Composable
fun MapTilePixmap(mapState: State, tile: TileMetadata, modifier: Modifier = Modifier) {
    var pixmap: ByteArray? by remember(mapState.currentMap) { mutableStateOf(null) }
    val mod = modifier.requiredSize(width = mapTilePixels.dp, height = mapTilePixels.dp)

    if (pixmap != null)
        Image(
            contentDescription = "${tile.id}",
            bitmap = SkiaImage.makeFromEncoded(pixmap!!).toComposeImageBitmap(),
            modifier = mod)
    else {
        Image(
            contentDescription = "${tile.id}",
            painter = ColorPainter(color = Color.Transparent),
            modifier = mod)
        LaunchedEffect(mapState.currentMap) { pixmap = mapState.loadTilePixmap(tile) }
    }
}

@Composable
fun MapIcon(icon: TileMetadata.Icon, options: DisplayOptions, modifier: Modifier = Modifier) {
    if (when (icon) {
            is TileMetadata.Pointer -> options.pointers.value
            is TileMetadata.Banner -> options.banners.value
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
    mapState: State,
    options: DisplayOptions,
    windowSizeState: androidx.compose.runtime.State<DpSize>,
) {
    var routes by rememberSaveable(mapState.currentMap.value.worldId) {
        mutableStateOf<RoutesMetadata?>(null) }
    if (routes == null && !mapState.currentMap.value.worldId.isNullOrEmpty())
        LaunchedEffect(mapState.currentMap.value.worldId) { routes = mapState.loadRoutes() }
    val mapMeta = mapState.currentMap.value.params
    if (mapMeta == null) {
        Text("No map loaded")
        return
    }
    var pos by remember(mapMeta, windowSizeState) {
        mutableStateOf(windowSizeState.value.center - mapMeta.mapSize().center)
    }

    // XXX if I put the pointerInput modifier here then it stops working after I switch maps and back
    MapLayout(
        map = mapMeta,
        routes = if (mapMeta.showRoutes && options.routes.value) routes else null,
        modifier = Modifier.offset { pos.toIntOffset() },
    ) {
        for (z in mapMeta.minZ .. mapMeta.maxZ) {
            if (z == 0)
                continue
            for (x in mapMeta.minX..mapMeta.maxX) {
                if (x == 0)
                    continue
                    key(x, z) {
                        MapTileBox(mapState = mapState, pos = TilePos(x, z)) { tile ->
                            key(x, z) {
                                MapTilePixmap(mapState = mapState, tile = tile,
                                    modifier = Modifier
                                        .tilePosition(TilePos(x, z))
                                        .pointerInput(Unit) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                pos += dragAmount
                                            }
                                        })
                            }
                            if (options.tileIds.value)
                                MapId(tile, Modifier.tileIdPosition(TilePos(x, z)))
                            tile.icons.forEachIndexed { idx, icon ->
                                key(x, z, idx) {
                                    MapIcon(icon, options = options, modifier = Modifier.iconPosition(icon))
                                }
                            }
                        }
                    }
            }
        }
        if (mapMeta.showRoutes && options.routes.value)
            routes?.let { routes ->
                for (node in routes.nodes.values)
                    //if (NetherPos(node.x, node.z).toMapLayoutPos(mapMeta).let { it.x >= 0 && it.y >= 0 })
                    MapRouteNode(node, modifier = Modifier.routeNodePosition(node))
                MapRoutePaths(routes, mapMeta, modifier = Modifier.routeGlobalOverlayPosition())
            }
    }
}
