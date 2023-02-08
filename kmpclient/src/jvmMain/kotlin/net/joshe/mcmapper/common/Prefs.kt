package net.joshe.mcmapper.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.prefs.Preferences

fun clientPrefsNode() : Preferences = Preferences.userRoot().node("mcmapper/client")

enum class PrefKeys(val key: String) {
    DARK("darkMode"),
    SHOW_IDS("showTileIds"),
    SHOW_POINTERS("showPointerIcons"),
    SHOW_BANNERS("showBannerIcons"),
    SHOW_ROUTES("showRoutes"),
    WINDOW_WIDTH("windowWidth"),
    WINDOW_HEIGHT("windowHeight"),
}

inline fun <reified T> Preferences.getGeneric(key: String, default: T) : T {
    if (get(key, null) == null)
        return default
    return when (T::class) {
        Boolean::class -> getBoolean(key, false)
        Long::class -> getLong(key, 0)
        Int::class -> getInt(key, 0)
        Double::class -> getDouble(key, 0.0)
        Float::class -> getFloat(key, 0f)
        String::class -> get(key, "")
        ByteArray::class -> getByteArray(key, byteArrayOf())
        else -> null
    } as T
}

fun <T> Preferences.putGeneric(key: String, value: T) {
    when (value) {
        is Boolean -> putBoolean(key, value)
        is Long -> putLong(key, value)
        is Int -> putInt(key, value)
        is Double -> putDouble(key, value)
        is Float -> putFloat(key, value)
        is String -> put(key, value)
        is ByteArray -> putByteArray(key, value)
    }
    //flush()
}

inline fun <reified T> Preferences.mutableStateFlowOf(scope: CoroutineScope,  key: String, def: T) =
    MutableStateFlow(getGeneric(key, def)).also { flow ->
        scope.launch { flow.collect { putGeneric(key, it) } }
    }
