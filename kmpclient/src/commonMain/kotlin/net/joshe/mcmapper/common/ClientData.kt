package net.joshe.mcmapper.common

import kotlinx.coroutines.flow.*
import net.joshe.mcmapper.mapdata.*

class ClientData(rootUrl: String) {
    private var client: Client? = null

    private val _worldInfo = MutableStateFlow<Map<String, RootMetadata.WorldStub>>(emptyMap())
    private val _currentWorld = MutableStateFlow<WorldMetadata?>(null)
    private val _currentMap = MutableStateFlow<MapMetadata?>(null)

    val worldInfo = _worldInfo.asStateFlow()
    val currentWorld = _currentWorld.asStateFlow()
    val currentMap = _currentMap.asStateFlow()

    init { setUrl(rootUrl) }

    fun setUrl(rootUrl: String) {
        if (Client.isUrlValid(rootUrl)) {
            client = Client(rootUrl.removeSuffix("/") + "/data")
            _worldInfo.value = emptyMap()
            _currentWorld.value = null
            _currentMap.value = null
        }
    }

    suspend fun loadRootData() {
        client?.let { client ->
            println("reloading root metadata")
            val meta = client.loadRootMetadata()
            _worldInfo.value = meta.worlds
            meta.defaultWorld?.let { currentWorld.value ?: selectWorld(it) }
        }
    }

    suspend fun reloadWorldCache(worldId: String?) {
        loadRootData()
        if (worldId == null)
            return

        client?.let { client ->
            println("reloading world \"${worldId}\"")
            val meta = client.loadWorldMetadata(worldId)
            if (currentWorld.value?.worldId == worldId) {
                _currentWorld.value = meta
                _currentMap.value = currentMap.value?.mapId?.let { meta.maps[it] } ?: meta.maps[meta.defaultMap]
            }
        }
    }

    suspend fun selectWorld(worldId: String) {
        if (currentWorld.value?.worldId == worldId)
            return

        client?.let { client ->
            println("selecting world \"${worldId}\"")
            val meta = client.loadWorldMetadata(worldId)
            _currentWorld.value = meta
            _currentMap.value = null
            selectMap(meta.defaultMap)
        }
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
