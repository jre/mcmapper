package net.joshe.mcmapper.data

import androidx.compose.runtime.mutableStateOf
import net.joshe.mcmapper.ui.TilePos
import net.joshe.mcmapper.metadata.*

class State(baseUrl: String) {
    private val client = if (Client.isUrlValid(baseUrl)) Client(baseUrl) else null

    data class Selection(val worldId: String? = null, val mapId: String? = null, val params: MapMetadata? = null)

    val worlds = mutableStateOf(emptyMap<String,String>())
    val worldMaps = mutableStateOf(emptyMap<String,String>())
    val currentMap = mutableStateOf<Selection>(Selection())

    private val worldCache: MutableMap<String, WorldMetadata> = mutableMapOf()
    private val mapCache: MutableMap<Pair<String,String>, MapMetadata> = mutableMapOf()

    // XXX how do I keep duplicate coroutines from being launched?

    suspend fun loadRootData() {
        client ?: return
        val meta = client.loadRootMetadata()
        worlds.value = meta.worlds
        selectWorld(meta.defaultWorld)
    }

    suspend fun selectWorld(worldId: String) {
        client ?: return
        if (currentMap.value.worldId == worldId)
            return
        println("loading world \"${worldId}\"")
        currentMap.value = Selection(worldId=worldId)
        worldMaps.value = worldCache[worldId]?.maps ?: emptyMap()
        worldCache[worldId]?.let { selectMap(it.defaultMap) }
        if (worldId in worldCache)
            return

        val meta = client.loadWorldMetadata(worldId)
        worldCache[worldId] = meta
        if (currentMap.value.worldId == worldId) {
            worldMaps.value = meta.maps
            selectMap(meta.defaultMap)
        }
    }

    suspend fun selectMap(mapId: String) {
        client ?: return
        if (currentMap.value.mapId == mapId)
            return
        println("loading map \"${mapId}\"")
        val worldId = currentMap.value.worldId
        require(worldId is String && worldId.isNotEmpty())
        val params = mapCache[Pair(worldId, mapId)]
        val selection = Selection(worldId=worldId, mapId=mapId, params=params)
        currentMap.value = selection
        if (params != null)
            return

        val meta = client.loadMapMetadata(worldId, mapId)
        mapCache[Pair(worldId, mapId)] = meta
        if (currentMap.value == selection)
            currentMap.value = Selection(worldId=worldId, mapId=mapId, params=meta)
    }

    suspend fun loadRoutes(): RoutesMetadata? {
        client ?: return null
        val worldId = currentMap.value.worldId
        if (worldId.isNullOrEmpty())
            return null
        return client.loadWorldRoutes(worldId)
    }

    suspend fun loadTileMetadata(pos: TilePos) : TileMetadata? {
        client ?: return null
        val map = currentMap.value
        map.params ?: return null
        require(map.worldId != null && map.mapId != null)
        return client.loadTileMetadata(map.worldId, map.mapId, pos.x, pos.z)
    }

    suspend fun loadTilePixmap(tile: TileMetadata) : ByteArray? {
        client ?: return null
        val map = currentMap.value
        map.params ?: return null
        require(map.worldId != null && map.mapId != null)
        return client.loadTileImage(map.worldId, tile.id)
    }
}
