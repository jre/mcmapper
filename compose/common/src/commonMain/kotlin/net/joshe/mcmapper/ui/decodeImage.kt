package net.joshe.mcmapper.ui

import androidx.compose.ui.graphics.ImageBitmap

expect fun ByteArray.decodeToImageBitmap() : ImageBitmap
