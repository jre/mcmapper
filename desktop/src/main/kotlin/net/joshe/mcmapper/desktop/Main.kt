package net.joshe.mcmapper.desktop

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.FlatLightLaf
import javax.swing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.system.exitProcess
import net.joshe.mcmapper.common.Client
import net.joshe.mcmapper.common.PrefKeys
import net.joshe.mcmapper.common.clientPrefsNode
import net.joshe.mcmapper.common.mutableStateFlowOf
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.util.prefs.Preferences

data class MapDisplayOptions(
    val darkMode: MutableStateFlow<Boolean?>,
    val tileIds: MutableStateFlow<Boolean>,
    val pointers: MutableStateFlow<Boolean>,
    val banners: MutableStateFlow<Boolean>,
    val routes: MutableStateFlow<Boolean>,
)

fun JFrame.restoreWindowSize(preferences: Preferences) {
    val width = preferences.getInt(PrefKeys.WINDOW_WIDTH.key, 0)
    val height = preferences.getInt(PrefKeys.WINDOW_HEIGHT.key, 0)
    if (width > 0 && height > 0)
        setSize(width, height)
}

fun JFrame.persistWindowSize(preferences: Preferences) =
    addComponentListener(object : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent?) {
            preferences.putInt(PrefKeys.WINDOW_WIDTH.key, width)
            preferences.putInt(PrefKeys.WINDOW_HEIGHT.key, height)
        }
    })

fun main(args: Array<String>) {
    val prefs = clientPrefsNode()
    val url = when (args.size) {
        0 -> System.getenv("MCMAPPER_URL")
        1 -> args[0]
        else -> null
    }
    if (url == null || !Client.isUrlValid(url)) {
        System.err.println("usage: http://mcmapper/url\nor set MCMAPPER_URL in the environment")
        exitProcess(1)
    }

    val opts = CoroutineScope(Dispatchers.IO).let { scope ->
        MapDisplayOptions(
            // XXX it would be nice to query some os-specific api for light/dark theme here
            darkMode = prefs.mutableStateFlowOf(scope, PrefKeys.DARK.key, null),
            tileIds = prefs.mutableStateFlowOf(scope, PrefKeys.SHOW_IDS.key, false),
            pointers = prefs.mutableStateFlowOf(scope, PrefKeys.SHOW_POINTERS.key, true),
            banners = prefs.mutableStateFlowOf(scope, PrefKeys.SHOW_BANNERS.key, true),
            routes = prefs.mutableStateFlowOf(scope, PrefKeys.SHOW_ROUTES.key, false),
        )
    }

    if (opts.darkMode.value == true)
        FlatDarkLaf.setup()
    else
        FlatLightLaf.setup()
    CoroutineScope(Dispatchers.Main).launch {
        opts.darkMode.collect { dark ->
            if (dark != null) {
                UIManager.setLookAndFeel(if (dark) FlatDarkLaf() else FlatLightLaf())
                FlatLaf.updateUI()
            }
        }
    }

    SwingUtilities.invokeLater {
        val win = MapperWindow(url, opts)
        win.restoreWindowSize(prefs)
        //win.pack()
        win.isVisible = true
        win.persistWindowSize(prefs)
        win.load()
    }
}
