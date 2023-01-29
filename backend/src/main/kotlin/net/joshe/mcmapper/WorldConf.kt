package net.joshe.mcmapper

import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.joshe.mcmapper.mapdata.*

fun readWorldsConf(path: File) : AllWorldsConf {
    val basePath = path.parentFile
    println("reading root config - ${path}")
    val stub: StubWorldsConf = Json.decodeFromString(path.readText())
    val configs: Map<String, WorldConf> = stub.worlds.mapValues { (key, world) ->
        val worldFile = File(world).ensureAbsolute(basePath)
        println("reading world \"${key}\" config - ${worldFile}")
        Json.decodeFromString(worldFile.readText())
    }
    val existingWorlds = configs.filterValues { File(it.path).exists() }
    return AllWorldsConf(
        worlds = existingWorlds,
        defaultWorld = if (stub.default in existingWorlds) stub.default else null,
    )
}

fun File.ensureAbsolute(base: File) = if (isAbsolute) this else File(base, path)

@Serializable
data class StubWorldsConf(
    val worlds: Map<String, String>,
    val default: String,
) {
    // XXX validate world keys here for url and filename safe chars
    init {
        require(default in worlds)
    }
}

data class AllWorldsConf(val worlds: Map<String, WorldConf>, val defaultWorld: String?)

@Serializable
data class WorldConf(
    val path: String,
    val label: String,
    val maps: Map<String, WorldMap>,
    val defaultMap: String,
    val nodes: Map<String, RouteNode>,
    val routes: List<List<String>>
) {
    // XXX validate map keys here for url and filename safe chars
    // XXX should I validate node keys too?
    @Serializable
    data class WorldMap(val scale: Int, val dimension: String, val label: String, val routes: Boolean = false)

    init { require(defaultMap in maps) }

    val routePaths = routes.map { routeSpec ->
        require(routeSpec[0] in nodes && routeSpec.last() in nodes)
        val points = mutableListOf<NetherPos>()
        routeSpec.forEach { item ->
            val prev = points.lastOrNull()
            points.add(
                if (item.startsWith("x=#"))
                    NetherPos(item.substring(3).toInt(), prev!!.z)
                else if (item.startsWith("z=#") || item.startsWith("y=#"))
                    NetherPos(prev!!.x, item.substring(3).toInt())
                else if (item.startsWith("x="))
                    NetherPos(nodes.getValue(item.substring(2)).pos.x, prev!!.z)
                else if (item.startsWith("z=") || item.startsWith("y="))
                    NetherPos(prev!!.x, nodes.getValue(item.substring(2)).pos.z)
                else
                    nodes.getValue(item).pos
            )
        }
        RoutePath(first = routeSpec[0], last = routeSpec.last(), path = points)
    }
}
