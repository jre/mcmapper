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
                        DarkModeButton(options.darkMode)
                        ShowTileIdsButton(options.tileIds)
                        ShowPointersButton(options)
                        ShowRoutesButton(options.routes)
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
    MenuSheetButton(
        state = menuSheetState,
        items = mapState.worldInfo.value.entries.sortedBy{it.value.label}.map{Pair(it.key,it.value.label)},
        selectedKey = mapState.currentWorld.value?.worldId,
        noneSelected = "Select a world",
        onSelect = { selection ->
            scope.launch { mapState.clientData.selectWorld(selection) }
        })
}

@Composable
fun MapSelectionButton(menuSheetState: MenuSheetState, mapState: RememberedMapState) {
    val scope = rememberCoroutineScope()
    MenuSheetButton(
        state = menuSheetState,
        items = mapState.currentWorld.value?.maps?.entries?.sortedBy { it.value.label }?.map { Pair(it.key, it.value.label) }
            ?: emptyList(),
        selectedKey = mapState.currentMap.value?.mapId,
        noneSelected = "Select a map",
        onSelect = { selection ->
            scope.launch { mapState.clientData.selectMap(selection) }
        })
}

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
fun MapTilePixmap(mapState: RememberedMapState, tile: TileMetadata, modifier: Modifier = Modifier) {
    var pixmap: ByteArray? by remember(mapState.currentWorld.value, tile) { mutableStateOf(null) }
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
        LaunchedEffect(mapState.currentWorld) { pixmap = mapState.clientData.loadTilePixmap(tile) }
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
        for (z in mapMeta.minPos.z .. mapMeta.maxPos.z) {
            for (x in mapMeta.minPos.x ..mapMeta.maxPos.x) {
                mapMeta.tiles[TilePos(x=x, z=z)]?.let { tile ->
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
        if (mapMeta.showRoutes && options.routes.value) {
            for (node in worldMeta.routes.nodes.values)
            //if (NetherPos(node.x, node.z).toMapLayoutPos(mapMeta).let { it.x >= 0 && it.y >= 0 })
                MapRouteNode(node, modifier = Modifier.routeNodePosition(node))
            MapRoutePaths(worldMeta.routes, mapMeta, modifier = Modifier.routeGlobalOverlayPosition())
        }
    }
}
