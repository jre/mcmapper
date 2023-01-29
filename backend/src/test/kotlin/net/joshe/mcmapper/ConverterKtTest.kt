package net.joshe.mcmapper

import net.joshe.mcmapper.mapdata.TilePos
import net.joshe.mcmapper.mapdata.WorldPos
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ConverterKtTest {
    @Test fun gtp1() = assertEquals(TilePos(0, 0), getTilePos(WorldPos(-64, 63), 0))
    @Test fun gtp2() = assertEquals(TilePos(0, 0), getTilePos(WorldPos(63, -64), 0))
    @Test fun gtp3() = assertEquals(TilePos(-1, 1), getTilePos(WorldPos(-65, 64), 0))
    @Test fun gtp4() = assertEquals(TilePos(1, -1), getTilePos(WorldPos(64, -65), 0))
    @Test fun gtp5() = assertEquals(TilePos(0, 0), getTilePos(WorldPos(-64, 1983), 4))
    @Test fun gtp6() = assertEquals(TilePos(0, 0), getTilePos(WorldPos(1983, -64), 4))
    @Test fun gtp7() = assertEquals(TilePos(-1, 1), getTilePos(WorldPos(-65, 1984), 4))
    @Test fun gtp8() = assertEquals(TilePos(1, -1), getTilePos(WorldPos(1984, -65), 4))
}
