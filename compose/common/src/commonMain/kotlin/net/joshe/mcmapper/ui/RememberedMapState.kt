package net.joshe.mcmapper.ui

import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import net.joshe.mcmapper.common.ClientData
import net.joshe.mcmapper.common.ClientStatus
import net.joshe.mcmapper.mapdata.MapMetadata
import net.joshe.mcmapper.mapdata.RootMetadata
import net.joshe.mcmapper.mapdata.WorldMetadata

class RememberedMapState(
    val clientData: ClientData,
    val status: State<Pair<ClientStatus,String>>,
    val worldInfo: State<Map<String, RootMetadata.WorldStub>>,
    val currentWorld: State<WorldMetadata?>,
    val currentMap: State<MapMetadata?>,
)

@Composable
fun rememberMapState(rootUrl: MutableState<String>) : RememberedMapState {
    val scope = rememberCoroutineScope()
    val clientData = ClientData(rootUrl.value)
    val state = RememberedMapState(
        clientData = clientData,
        status = clientData.status.collectAsState(scope.coroutineContext),
        worldInfo = clientData.worldInfo.collectAsState(scope.coroutineContext),
        currentWorld = clientData.currentWorld.collectAsState(scope.coroutineContext),
        currentMap = clientData.currentMap.collectAsState(scope.coroutineContext),
    )

    if (state.clientData.worldInfo.value.isEmpty())
        scope.launch { state.clientData.loadRootData() }
    return state
}
