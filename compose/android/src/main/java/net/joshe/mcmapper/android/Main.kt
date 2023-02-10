package net.joshe.mcmapper.android

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import net.joshe.mcmapper.common.PrefKeys
import net.joshe.mcmapper.ui.App
import net.joshe.mcmapper.ui.DisplayOptions

@Composable
fun ComposeApp(prefs: SharedPreferences) {
    val size = LocalConfiguration.current.let { cf ->
            remember { mutableStateOf(DpSize(cf.screenWidthDp.dp, cf.screenHeightDp.dp)) }
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
    App(windowSizeState = size, options = options, ioDispatcher = Dispatchers.IO)
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("Client", MODE_PRIVATE)
        setContent { ComposeApp(prefs) }
    }
}
