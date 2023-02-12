package net.joshe.mcmapper.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import net.joshe.mcmapper.mapdata.*

enum class ClientStatus(val label: String) {
    NO_URL("No URL set"),
    READY("Ready"),
    LOADING("Loading"),
    ERROR("Error")
}

class ClientData(rootUrl: String, private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default) {
    private var client: Client? = null
    private var lastUrl = ""

    private val _status = MutableStateFlow(Pair(ClientStatus.NO_URL,""))
    private val _worldInfo = MutableStateFlow<Map<String, RootMetadata.WorldStub>>(emptyMap())
    private val _currentWorld = MutableStateFlow<WorldMetadata?>(null)
    private val _currentMap = MutableStateFlow<MapMetadata?>(null)

    val status = _status.asStateFlow()
    val worldInfo = _worldInfo.asStateFlow()
    val currentWorld = _currentWorld.asStateFlow()
    val currentMap = _currentMap.asStateFlow()

    init { setUrl(rootUrl) }

    fun setUrl(rootUrl: String) {
        if (Client.isUrlValid(rootUrl) && rootUrl != lastUrl) {
            lastUrl = rootUrl
            client = Client(rootUrl.removeSuffix("/") + "/data")
            _status.value = Pair(ClientStatus.READY, "")
            _worldInfo.value = emptyMap()
            _currentWorld.value = null
            _currentMap.value = null
        }
    }

    private suspend fun <T>tryLoad(desc: String, func: suspend () -> T) : T? {
        var result: T? = null
        _status.value = Pair(ClientStatus.LOADING, desc)
        try {
            result = withContext(ioDispatcher) { func() }
            _status.value = Pair(ClientStatus.READY, "")
        } catch (e: Throwable) {
            _status.value = Pair(ClientStatus.ERROR, e.toString())
        }
        return result
    }

    suspend fun loadRootData() {
        client?.let { client ->
            println("reloading root metadata")
            tryLoad("root metadata") { client.loadRootMetadata() }?.let { meta ->
                _worldInfo.value = meta.worlds
                meta.defaultWorld?.let { currentWorld.value ?: selectWorld(it) }
            }
        }
    }

    suspend fun reloadWorldCache(worldId: String?) {
        loadRootData()
        if (worldId == null)
            return

        client?.let { client ->
            println("reloading world \"${worldId}\"")
            tryLoad("world ${worldId}") { client.loadWorldMetadata(worldId) }?.let { meta ->
                if (currentWorld.value?.worldId == worldId) {
                    _currentWorld.value = meta
                    _currentMap.value = currentMap.value?.mapId?.let { meta.maps[it] } ?:
                            meta.maps[meta.defaultMap]
                }
            }
        }
    }

    suspend fun selectWorld(worldId: String) {
        if (currentWorld.value?.worldId == worldId)
            return

        client?.let { client ->
            println("selecting world \"${worldId}\"")
            tryLoad("world ${worldId}") { client.loadWorldMetadata(worldId) }?.let { meta ->
                _currentWorld.value = meta
                _currentMap.value = null
                selectMap(meta.defaultMap)
            }
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
        currentWorld.value?.worldId?.let {
            tryLoad("map tile ${tile.id}") { client?.loadTileImage(it, tile) }
        }
}
