package net.joshe.mcmapper.mapdata

import kotlin.test.Test
import kotlin.test.assertEquals

internal class MetadataKtTest {
    @Test fun sf0() = assertEquals(1, scaleFactor(0))
    @Test fun sf1() = assertEquals(2, scaleFactor(1))
    @Test fun sf2() = assertEquals(4, scaleFactor(2))
    @Test fun sf3() = assertEquals(8, scaleFactor(3))
    @Test fun sf4() = assertEquals(16, scaleFactor(4))

    @Test fun nptwp() = assertEquals(WorldPos(16, -24), NetherPos(2, -3).toWorldPos())

    val map0 = MapMetadata(scale=0, mapId="", label="", dimension="", minPos=TilePos(0,0), maxPos=TilePos(0,0), tiles= emptyMap())
    val map1 = MapMetadata(scale=1, mapId="", label="", dimension="", minPos=TilePos(0,0), maxPos=TilePos(0,0), tiles= emptyMap())
    val map2 = MapMetadata(scale=2, mapId="", label="", dimension="", minPos=TilePos(0,0), maxPos=TilePos(0,0), tiles= emptyMap())
    val map3 = MapMetadata(scale=3, mapId="", label="", dimension="", minPos=TilePos(0,0), maxPos=TilePos(0,0), tiles= emptyMap())
    val map4 = MapMetadata(scale=4, mapId="", label="", dimension="", minPos=TilePos(0,0), maxPos=TilePos(0,0), tiles= emptyMap())

    @Test fun twptl0() = assertEquals(WorldPos(-64, -64), TilePos(0,0).toWorldPosTopLeft(map0))
    @Test fun twptl1() = assertEquals(WorldPos(192, -320), TilePos(1,-1).toWorldPosTopLeft(map1))
    @Test fun twptl2() = assertEquals(WorldPos(960, -1088), TilePos(2,-2).toWorldPosTopLeft(map2))
    @Test fun twptl3() = assertEquals(WorldPos(3008, -3136), TilePos(3,-3).toWorldPosTopLeft(map3))
    @Test fun twptl4() = assertEquals(WorldPos(8128, -8256), TilePos(4,-4).toWorldPosTopLeft(map4))
}
