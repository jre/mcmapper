package net.joshe.mcmapper.ui

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap

actual fun ByteArray.decodeToImageBitmap() =
    BitmapFactory.decodeByteArray(this, 0, size).asImageBitmap()
