package net.joshe.mcmapper.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.joshe.mcmapper.metadata.*

class Client(url: String) {
    companion object {
        fun isUrlValid(url: String) = URLBuilder(url).host.isNotEmpty()
    }
    private val baseUrl = Url(url)
    private val decoder = Json { ignoreUnknownKeys = true }

    private val client = HttpClient {
        expectSuccess = true
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.NONE //ALL
        }
    }

    suspend fun loadRootMetadata() : RootMetadata = decoder.decodeFromString(
        client.get(baseUrl) {
            url { appendPathSegments(WorldPaths.getRootMetadataPath()) }
        }.bodyAsText())

    suspend fun loadWorldMetadata(worldId: String) : WorldMetadata = decoder.decodeFromString(
        client.get(baseUrl) {
            url { appendPathSegments(WorldPaths.getWorldMetadataPath(worldId)) }
        }.bodyAsText())

    suspend fun loadWorldRoutes(worldId: String) : RoutesMetadata = decoder.decodeFromString(
        client.get(baseUrl) {
            url { appendPathSegments(WorldPaths.getWorldRoutesPath(worldId)) }
        }.bodyAsText())

    suspend fun loadMapMetadata(worldId: String, mapId: String) : MapMetadata = decoder.decodeFromString(
        client.get(baseUrl) {
            url { appendPathSegments(WorldPaths.getMapMetadataPath(worldId, mapId)) }
        }.bodyAsText())

    suspend fun loadTileMetadata(worldId: String, mapId: String, x: Int, z: Int) : TileMetadata? {
        val resp = client.get(baseUrl) {
            expectSuccess = false
            url {
                appendPathSegments(WorldPaths.getTileMetadataPath(worldId, mapId, Pair(x, z)))
            }
        }
        return if (resp.status.isSuccess())
            decoder.decodeFromString(resp.bodyAsText())
        else null
    }

    suspend fun loadTileImage(worldId: String, tileId: Int) : ByteArray? {
        val resp = client.get(baseUrl) {
            expectSuccess = false
            url {
                appendPathSegments(WorldPaths.getTileBitmapPath(worldId, tileId))
            }
        }
        return if (resp.status.isSuccess())
            resp.body()
        else null
    }
}
