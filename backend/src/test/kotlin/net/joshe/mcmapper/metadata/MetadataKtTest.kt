package net.joshe.mcmapper.metadata

import kotlin.test.Test
import kotlin.test.assertEquals

internal class MetadataKtTest {
    @Test fun sf0() = assertEquals(1, scaleFactor(0))
    @Test fun sf1() = assertEquals(2, scaleFactor(1))
    @Test fun sf2() = assertEquals(4, scaleFactor(2))
    @Test fun sf3() = assertEquals(8, scaleFactor(3))
    @Test fun sf4() = assertEquals(16, scaleFactor(4))
}
