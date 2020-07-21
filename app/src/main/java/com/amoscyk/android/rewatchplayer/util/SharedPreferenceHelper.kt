package com.amoscyk.android.rewatchplayer.util

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import com.amoscyk.android.rewatchplayer.AppConstant

/**
 * Enum class to store keys for SharedPreferences
 * */
enum class PreferenceKey {
    ACCOUNT_NAME,
    LAST_OPEN_TAB_ID,
    LIBRARY_LIST_MODE,
    SEARCH_OPTION,
    PLAYER_ONLY_PLAY_WHEN_USING_WIFI,
    PLAYER_SKIP_FORWARD_TIME,
    PLAYER_SKIP_BACKWARD_TIME,
    PLAYER_ENABLE_PIP,
    PLAYER_PLAY_DOWNLOADED_IF_EXIST,
    ALLOW_VIDEO_STREAMING_ENV,
    ALLOW_PLAY_IN_BACKGROUND,
    ALLOW_DOWNLOAD_ENV
}

abstract class NullableSharedPreferencesLiveData<T>(
    val pref: SharedPreferences,
    val prefKey: PreferenceKey,
    val defValue: T?): LiveData<T?>() {

    private val mOnChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            runCatching {
                if (PreferenceKey.valueOf(key) == prefKey) {
                    value = getValue(prefKey, defValue)
                }
            }.onFailure {
                Log.e(AppConstant.TAG, it.message.orEmpty())
            }
        }

    abstract fun getValue(prefKey: PreferenceKey, defValue: T?): T?

    override fun onActive() {
        super.onActive()
        value = getValue(prefKey, defValue)
        pref.registerOnSharedPreferenceChangeListener(mOnChangeListener)
    }

    override fun onInactive() {
        pref.unregisterOnSharedPreferenceChangeListener(mOnChangeListener)
        super.onInactive()
    }
}

abstract class SharedPreferencesLiveData<T>(
    val pref: SharedPreferences,
    val prefKey: PreferenceKey,
    val defValue: T): LiveData<T>() {

    private val mOnChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            runCatching {
                if (PreferenceKey.valueOf(key) == prefKey) {
                    value = getValue(prefKey, defValue)
                }
            }.onFailure {
                Log.e(AppConstant.TAG, it.message.orEmpty())
            }
        }

    abstract fun getValue(prefKey: PreferenceKey, defValue: T): T

    override fun onActive() {
        super.onActive()
        value = getValue(prefKey, defValue)
        pref.registerOnSharedPreferenceChangeListener(mOnChangeListener)
    }

    override fun onInactive() {
        pref.unregisterOnSharedPreferenceChangeListener(mOnChangeListener)
        super.onInactive()
    }
}

class SPStringLiveData(pref: SharedPreferences, key: PreferenceKey, defValue: String?) :
    NullableSharedPreferencesLiveData<String>(pref, key, defValue) {
    override fun getValue(prefKey: PreferenceKey, defValue: String?): String? {
        return pref.getString(prefKey, defValue)
    }
}

class SPBooleanLiveData(pref: SharedPreferences, key: PreferenceKey, defValue: Boolean) :
        SharedPreferencesLiveData<Boolean>(pref, key, defValue) {
    override fun getValue(prefKey: PreferenceKey, defValue: Boolean): Boolean {
        return pref.getBoolean(prefKey, defValue)
    }
}

class SPIntLiveData(pref: SharedPreferences, key: PreferenceKey, defValue: Int) :
        SharedPreferencesLiveData<Int>(pref, key, defValue) {
    override fun getValue(prefKey: PreferenceKey, defValue: Int): Int {
        return pref.getInt(prefKey, defValue)
    }
}

class SPLongLiveData(pref: SharedPreferences, key: PreferenceKey, defValue: Long) :
        SharedPreferencesLiveData<Long>(pref, key, defValue) {
    override fun getValue(prefKey: PreferenceKey, defValue: Long): Long {
        return pref.getLong(prefKey, defValue)
    }
}

class SPFloatLiveData(pref: SharedPreferences, key: PreferenceKey, defValue: Float) :
        SharedPreferencesLiveData<Float>(pref, key, defValue) {
    override fun getValue(prefKey: PreferenceKey, defValue: Float): Float {
        return pref.getFloat(prefKey, defValue)
    }
}

val Context.appSharedPreference: SharedPreferences
    get() = this.getSharedPreferences("rewatch_player_preference", Context.MODE_PRIVATE)

fun SharedPreferences.getString(prefKey: PreferenceKey, defValue: String?): String? {
    return this.getString(prefKey.name, defValue)
}

fun SharedPreferences.getBoolean(prefKey: PreferenceKey, defValue: Boolean): Boolean {
    return this.getBoolean(prefKey.name, defValue)
}

fun SharedPreferences.getInt(prefKey: PreferenceKey, defValue: Int): Int {
    return this.getInt(prefKey.name, defValue)
}

fun SharedPreferences.getFloat(prefKey: PreferenceKey, defValue: Float): Float {
    return this.getFloat(prefKey.name, defValue)
}

fun SharedPreferences.getLong(prefKey: PreferenceKey, defValue: Long): Long {
    return this.getLong(prefKey.name, defValue)
}

fun SharedPreferences.Editor.putBoolean(
    prefKey: PreferenceKey, value: Boolean): SharedPreferences.Editor {
    return this.putBoolean(prefKey.name, value)
}

fun SharedPreferences.Editor.putString(
    prefKey: PreferenceKey, value: String?): SharedPreferences.Editor {
    return this.putString(prefKey.name, value)
}

fun SharedPreferences.Editor.putInt(
    prefKey: PreferenceKey, value: Int): SharedPreferences.Editor {
    return this.putInt(prefKey.name, value)
}

fun SharedPreferences.Editor.putFloat(
    prefKey: PreferenceKey, value: Float): SharedPreferences.Editor {
    return this.putFloat(prefKey.name, value)
}

fun SharedPreferences.Editor.putLong(
    prefKey: PreferenceKey, value: Long): SharedPreferences.Editor {
    return this.putLong(prefKey.name, value)
}

fun SharedPreferences.Editor.remove(
    prefKey: PreferenceKey): SharedPreferences.Editor {
    return this.remove(prefKey.name)
}