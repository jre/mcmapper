package net.joshe.mcmapper

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import kotlinx.browser.document
import kotlinx.browser.window
import net.joshe.mcmapper.ui.App
import net.joshe.mcmapper.ui.DisplayOptions
import org.jetbrains.skiko.wasm.onWasmReady
import org.w3c.dom.HTMLCanvasElement

private const val composeCanvasId = "ComposeTarget"

fun resizeCanvas(windowSize: MutableState<DpSize>) {
    (document.getElementById(composeCanvasId) as? HTMLCanvasElement)?.let { canvas ->
        println("resizing html5 canvas ${canvas.width} x ${canvas.height}" +
                " -> ${window.innerWidth} x ${window.innerHeight}")
        windowSize.value = DpSize(window.innerWidth.dp, window.innerHeight.dp)
        canvas.width = window.innerWidth
        canvas.height = window.innerHeight
        canvas.style.width = ""
        canvas.style.height = ""
    }
}

object PrefersColorScheme {
    private const val darkQuery = "(prefers-color-scheme: dark)"
    private const val lightQuery = "(prefers-color-scheme: light)"

    fun listen(handler: (Boolean?) -> Unit) {
        if (window.matchMedia("(prefers-color-scheme)").matches)
            window.matchMedia(darkQuery).addEventListener("change", {
                println("color preference changed: ${it}")
                handler(prefersDark())
            })
    }

    fun prefersDark() : Boolean? {
        return if (window.matchMedia(darkQuery).matches) true
        else if (window.matchMedia(lightQuery).matches) false
        else null
    }
}

fun main() {
    val baseUrl = window.location.toString()
    val windowSize = mutableStateOf(DpSize(0.dp,0.dp))
    val options = DisplayOptions(
        darkMode = mutableStateOf(PrefersColorScheme.prefersDark()),
        tileIds = mutableStateOf(false),
        pointers = mutableStateOf(true),
        banners = mutableStateOf(true),
        routes = mutableStateOf(false),
    )

    //window.addEventListener("resize", { resizeCanvas(windowSize) })
    resizeCanvas(windowSize)

    PrefersColorScheme.listen {
        println("dark mode listener changed state ${options.darkMode.value} -> ${it}")
        options.darkMode.value = it
    }

    onWasmReady {
        Window {
            App(rootUrl = baseUrl, windowSizeState = windowSize, options = options)
        }
    }
}
