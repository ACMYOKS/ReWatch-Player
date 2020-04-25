package com.amoscyk.android.rewatchplayer.util

import android.content.Context
import android.content.SharedPreferences

/**
 * Enum class to store keys for SharedPreferences
 * */
enum class PreferenceKey(val keyName: String) {
    ACCOUNT_NAME("accountName"),
    LIBRARY_LIST_MODE("libraryListMode")
}

/*
object SharedPreferenceHelper {
    fun getAppSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("rewatch_player_preference", Context.MODE_PRIVATE)
    }
}
*/

val Context.appSharedPreference: SharedPreferences
    get() {
        return this.getSharedPreferences("rewatch_player_preference", Context.MODE_PRIVATE)
    }

fun SharedPreferences.getString(prefKey: PreferenceKey, defValue: String?): String? {
    return this.getString(prefKey.keyName, defValue)
}

fun SharedPreferences.getBoolean(prefKey: PreferenceKey, defValue: Boolean): Boolean {
    return this.getBoolean(prefKey.keyName, defValue)
}

fun SharedPreferences.getInt(prefKey: PreferenceKey, defValue: Int): Int {
    return this.getInt(prefKey.keyName, defValue)
}

fun SharedPreferences.getFloat(prefKey: PreferenceKey, defValue: Float): Float {
    return this.getFloat(prefKey.keyName, defValue)
}

fun SharedPreferences.getLong(prefKey: PreferenceKey, defValue: Long): Long {
    return this.getLong(prefKey.keyName, defValue)
}

fun SharedPreferences.Editor.putBoolean(
    prefKey: PreferenceKey, value: Boolean): SharedPreferences.Editor {
    return this.putBoolean(prefKey.keyName, value)
}

fun SharedPreferences.Editor.putString(
    prefKey: PreferenceKey, value: String?): SharedPreferences.Editor {
    return this.putString(prefKey.keyName, value)
}

fun SharedPreferences.Editor.putInt(
    prefKey: PreferenceKey, value: Int): SharedPreferences.Editor {
    return this.putInt(prefKey.keyName, value)
}

fun SharedPreferences.Editor.putFloat(
    prefKey: PreferenceKey, value: Float): SharedPreferences.Editor {
    return this.putFloat(prefKey.keyName, value)
}

fun SharedPreferences.Editor.putLong(
    prefKey: PreferenceKey, value: Long): SharedPreferences.Editor {
    return this.putLong(prefKey.keyName, value)
}

fun SharedPreferences.Editor.remove(
    prefKey: PreferenceKey): SharedPreferences.Editor {
    return this.remove(prefKey.keyName)
}