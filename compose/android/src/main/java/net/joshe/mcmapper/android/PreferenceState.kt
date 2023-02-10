package net.joshe.mcmapper.android

import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit

inline fun <reified T> SharedPreferences.getGeneric(key: String, defValue: T) : T {
    if (!contains(key))
        return defValue
    return when(T::class) {
        Boolean::class -> getBoolean(key, false)
        Long::class ->  getLong(key, 0)
        Int::class ->  getInt(key, 0)
        Float::class -> getFloat(key, 0f)
        String::class -> getString(key, "")
        else -> null
    } as T
}

fun <T> SharedPreferences.putGeneric(key: String, value: T) {
    edit(commit = true) {
        when (value) {
            is Boolean -> putBoolean(key, value)
            is Long -> putLong(key, value)
            is Int -> putInt(key, value)
            is Float -> putFloat(key, value)
            is String -> putString(key, value)
        }
    }
}

class PreferenceState<T>(
    private val preferences: SharedPreferences,
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

inline fun <reified T> SharedPreferences.mutableStateOf(key: String, defValue: T) : PreferenceState<T> =
    PreferenceState(this, key, mutableStateOf(this.getGeneric(key, defValue)))
