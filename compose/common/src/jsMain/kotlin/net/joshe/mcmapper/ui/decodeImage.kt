package net.joshe.mcmapper.ui

import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

actual fun ByteArray.decodeToImageBitmap() =
    Image.makeFromEncoded(this).toComposeImageBitmap()
