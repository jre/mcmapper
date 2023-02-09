package net.joshe.mcmapper

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import net.joshe.mcmapper.common.getGeneric
import net.joshe.mcmapper.common.putGeneric
import java.util.prefs.Preferences

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
