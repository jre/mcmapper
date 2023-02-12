// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package net.joshe.mcmapper

import androidx.compose.runtime.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import net.joshe.mcmapper.common.*
import net.joshe.mcmapper.ui.App
import net.joshe.mcmapper.ui.DisplayOptions
import java.util.prefs.Preferences
import kotlin.system.exitProcess

fun loadWindowSize(preferences: Preferences) : DpSize? {
    val size = DpSize(
        preferences.getInt(PrefKeys.WINDOW_WIDTH.key, 0).dp,
        preferences.getInt(PrefKeys.WINDOW_HEIGHT.key, 0).dp)
    return if (size.width > 0.dp && size.height > 0.dp) size else null
}

fun saveWindowSize(preferences: Preferences, size: DpSize) {
    preferences.putInt(PrefKeys.WINDOW_WIDTH.key, size.width.value.toInt())
    preferences.putInt(PrefKeys.WINDOW_HEIGHT.key, size.height.value.toInt())
}

fun main(args: Array<String>) {
    val prefs = clientPrefsNode()
    val url = when (args.size) {
        0 -> System.getenv("MCMAPPER_URL") ?: ""
        1 -> args[0]
        else -> {
            System.err.println("usage: http://mcmapper/url\nor set MCMAPPER_URL in the environment")
            exitProcess(1)
        }
    }

    val options = DisplayOptions(
        rootUrl = prefs.mutableStateOf(PrefKeys.URL.key, ""),
        // XXX it would be nice to query some os-specific api for light/dark theme here
        darkMode = prefs.mutableStateOf(PrefKeys.DARK.key, null),
        tileIds = prefs.mutableStateOf(PrefKeys.SHOW_IDS.key, false),
        pointers = prefs.mutableStateOf(PrefKeys.SHOW_POINTERS.key, true),
        banners = prefs.mutableStateOf(PrefKeys.SHOW_BANNERS.key, true),
        routes = prefs.mutableStateOf(PrefKeys.SHOW_ROUTES.key, false),
    )
    if (url != options.rootUrl.value && Client.isUrlValid(url))
        options.rootUrl.value = url
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
            App(windowSizeState = windowSize, options = options, ioDispatcher = Dispatchers.IO)
        }
    }
}
