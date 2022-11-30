package net.joshe.mcmapper

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import java.util.prefs.Preferences

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

class PreferenceState<T>(
    private val preferences: Preferences,
    private val key: String,
    private val state: MutableState<T>,
) : MutableState<T> by state {
    override var value: T
        get() = state.value
        set(value) {
            state.value = value
            if (value != null)
                preferences.putGeneric(key, value)
        }
}

inline fun <reified T> Preferences.mutableStateOf(key: String, def: T) : PreferenceState<T> =
    PreferenceState(this, key, mutableStateOf(getGeneric(key, def)))
