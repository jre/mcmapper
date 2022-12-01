package net.joshe.mcmapper.data

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import net.joshe.mcmapper.metadata.*

class MapState(baseUrl: String) {
    private val client = if (Client.isUrlValid(baseUrl)) Client(baseUrl) else null

    private val _worldInfo = mutableStateOf(emptyMap<String,RootMetadata.WorldStub>())
    private val _currentWorld = mutableStateOf<WorldMetadata?>(null)
    private val _currentMap = mutableStateOf<MapMetadata?>(null)

    val worldInfo: State<Map<String, RootMetadata.WorldStub>> = _worldInfo
    val currentWorld: State<WorldMetadata?> = _currentWorld
    val currentMap: State<MapMetadata?> = _currentMap

    // XXX how do I keep duplicate coroutines from being launched?

    suspend fun loadRootData() {
        client ?: return
        val meta = client.loadRootMetadata()
        _worldInfo.value = meta.worlds
        meta.defaultWorld?.let { currentWorld.value ?: selectWorld(it) }
    }

    suspend fun reloadWorldCache(worldId: String) {
        client ?: return
        println("reloading world \"${worldId}\"")
        loadRootData()
        val meta = client.loadWorldMetadata(worldId)
        if (currentWorld.value?.worldId == worldId) {
            _currentWorld.value = meta
            _currentMap.value = currentMap.value?.mapId?.let { meta.maps[it] } ?: meta.maps[meta.defaultMap]
        }
    }

    suspend fun selectWorld(worldId: String) {
        client ?: return
        if (currentWorld.value?.worldId == worldId)
            return

        println("selecting world \"${worldId}\"")
        val meta = client.loadWorldMetadata(worldId)
        _currentWorld.value = meta
        _currentMap.value = null
        selectMap(meta.defaultMap)
    }

    fun selectMap(mapId: String) {
        if (currentMap.value?.mapId == mapId)
            return
        println("selecting map \"${mapId}\"")

        currentWorld.value.let { world ->
            require(world != null && mapId in world.maps)
            _currentMap.value = world.maps[mapId]
        }
    }

    suspend fun loadTilePixmap(tile: TileMetadata) =
        currentWorld.value?.worldId?.let { client?.loadTileImage(it, tile) }
}
