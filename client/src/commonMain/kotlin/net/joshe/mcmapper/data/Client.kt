package net.joshe.mcmapper.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.utils.CacheControl
import io.ktor.http.*
import io.ktor.util.date.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.joshe.mcmapper.mapdata.*

class Client(url: String) {
    companion object {
        fun isUrlValid(url: String) = URLBuilder(url).host.isNotEmpty()
    }

    private fun HttpResponse.lastModifiedGMTDate() =
        headers[HttpHeaders.LastModified]?.fromHttpToGmtDate()?.truncateToSeconds()

    private val baseUrl = Url(url)
    private val decoder = Json { ignoreUnknownKeys = true }

    private var rootCache: Pair<GMTDate, RootMetadata>? = null
    private val worldCache = mutableMapOf<String, Pair<GMTDate,WorldMetadata>>()
    private val tileCache = mutableMapOf<Pair<String,Int>, Pair<GMTDate,ByteArray>>()

    private val client = HttpClient {
        install(Logging) {
            logger = Logger.SIMPLE
            level = LogLevel.NONE //ALL
        }
    }

    suspend fun loadRootMetadata() : RootMetadata {
        val cached = rootCache
        val resp = try {
            client.get(baseUrl) {
                expectSuccess = true
                url { appendPathSegments(WorldPaths.getRootMetadataPath()) }
                headers {
                    cached?.first.let { modified ->
                        if (modified == null)
                            append(HttpHeaders.CacheControl, CacheControl.NO_CACHE)
                        else
                            append(HttpHeaders.IfModifiedSince, modified.toHttpDate())
                    }
                }
            }
        } catch (e: ResponseException) {
            if (e.response.status == HttpStatusCode.NotModified && cached != null) {
                println("root metadata unchanged, returning from cache")
                return cached.second
            }
            throw e
        }
        val root = decoder.decodeFromString<RootMetadata>(resp.bodyAsText())
        rootCache = resp.lastModifiedGMTDate()?.let { Pair(it, root) }
        println("caching root metadata from ${rootCache?.first?.toHttpDate()}")
        return root
    }

    suspend fun loadWorldMetadata(worldId: String) : WorldMetadata {
        val modified = rootCache?.second?.worlds?.get(worldId)?.modified
        val cached = worldCache[worldId]
        if (modified != null && cached != null && modified <= cached.first) {
            println("found world metadata for ${worldId} in cache")
            return cached.second
        }
        println("fetching new world metadata for ${worldId}, ignoring cache from ${cached?.first?.toHttpDate()}")

        val resp = client.get(baseUrl) {
            url { appendPathSegments(WorldPaths.getWorldMetadataPath(worldId)) }
            headers { append(HttpHeaders.CacheControl, CacheControl.NO_CACHE) }
        }
        val world = decoder.decodeFromString<WorldMetadata>(resp.bodyAsText())

        resp.lastModifiedGMTDate()?.let { Pair(it, world) }.let { entry ->
            if (entry == null)
                worldCache.remove(worldId)
            else
                worldCache[worldId] = entry
        }
        println("caching world metadata for ${worldId} from ${worldCache[worldId]?.first?.toHttpDate()}")
        return world
    }

    suspend fun loadTileImage(worldId: String, tile: TileMetadata) : ByteArray? {
        var cached = requestCachedTileImage(worldId, tile)
        if (cached != null && tile.modified > cached.first && cached.first != GMTDate.START)
            cached = requestCachedTileImage(worldId, tile)
        return cached?.second
    }

    private suspend fun requestCachedTileImage(worldId: String, tile: TileMetadata) : Pair<GMTDate,ByteArray>? {
        val key = Pair(worldId, tile.id)
        val cached = tileCache[key]
        if (cached != null && tile.modified <= cached.first) {
            println("found tile ${tile.id} in cache from ${cached.first.toHttpDate()}")
            return cached
        }

        val resp = client.get(baseUrl) {
            expectSuccess = false
            url { appendPathSegments(WorldPaths.getTileBitmapPath(worldId, tile.id)) }
            if (cached != null)
                headers { append(HttpHeaders.IfModifiedSince, cached.first.toHttpDate()) }
        }
        if (resp.status == HttpStatusCode.NotModified && cached != null)
            return cached
        else if (!resp.status.isSuccess())
            return null
        val bytes: ByteArray = resp.body()

        resp.lastModifiedGMTDate()?.let { Pair(it, bytes) }.let { entry ->
            if (entry == null)
                tileCache.remove(key)
            else {
                tileCache[key] = entry
                return entry
            }
        }
        return Pair(GMTDate.START, bytes)
    }
}
