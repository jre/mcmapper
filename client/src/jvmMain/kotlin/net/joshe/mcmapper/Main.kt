// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package net.joshe.mcmapper

import androidx.compose.runtime.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.flow.*
import net.joshe.mcmapper.data.Client
import net.joshe.mcmapper.ui.App
import net.joshe.mcmapper.ui.DisplayOptions
import java.util.prefs.Preferences
import kotlin.system.exitProcess

fun loadWindowSize(preferences: Preferences) : DpSize? {
    val node = preferences.node("windowSize")
    val size = DpSize(node.getInt("width", 0).dp, node.getInt("height", 0).dp)
    return if (size.width > 0.dp && size.height > 0.dp) size else null
}

fun saveWindowSize(preferences: Preferences, size: DpSize) {
    val node = preferences.node("windowSize")
    node.putInt("width", size.width.value.toInt())
    node.putInt("height", size.height.value.toInt())
}

fun main(args: Array<String>) {
    val prefs = Preferences.userRoot().node("mcmapper/client")
    val url = when (args.size) {
        0 -> System.getenv("MCMAPPER_URL")
        1 -> args[0]
        else -> null
    }
    if (url == null || !Client.isUrlValid(url)) {
        System.err.println("usage: http://mcmapper/url\nor set MCMAPPER_URL in the environment")
        exitProcess(1)
    }

    val options = DisplayOptions(
        // XXX it would be nice to query some os-specific api for light/dark theme here
        darkMode = prefs.mutableStateOf("darkMode", null),
        tileIds = prefs.mutableStateOf("showTileIds", false),
        pointers = prefs.mutableStateOf("showPointerIcons", true),
        banners = prefs.mutableStateOf("showBannerIcons", true),
        routes = prefs.mutableStateOf("showRoutes", false),
    )
    val savedWindowSize = loadWindowSize(prefs)
    var windowSizeUsed = false

    application {
        val rootWindowState = rememberWindowState()
        if (savedWindowSize != null && !windowSizeUsed) {
            rootWindowState.size = savedWindowSize
            windowSizeUsed = true
        }
        val windowSize = flow {
            println("root window size ${rootWindowState.size}")
            emit(rootWindowState.size)
        }.collectAsState(initial = rootWindowState.size)

        Window(
            title = "mcmapper",
            onCloseRequest = {
                if (rootWindowState.size != savedWindowSize)
                    saveWindowSize(prefs, rootWindowState.size)
                exitApplication()
            },
            state = rootWindowState,
        ) {
            App(rootUrl = url, windowSizeState = windowSize, options = options)
        }
    }
}
