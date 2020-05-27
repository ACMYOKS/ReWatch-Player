package com.amoscyk.android.rewatchplayer.util

import android.util.LruCache
import org.threeten.bp.Duration

object DateTimeHelper {
    private val cache_durationToDisplay = object : LruCache<String, String>(1024) {
        override fun sizeOf(key: String, value: String): Int {
            return value.length
        }
    }
    fun getDisplayString(durationString: String): String {
        cache_durationToDisplay[durationString]?.let { return it }
        runCatching {
            val totalS = Duration.parse(durationString).seconds
            val h = totalS / 3600
            val m = totalS % 3600 / 60
            val s = totalS % 60
            val str = (if (h > 0) "%02d:".format(h) else "") + "%02d:%02d".format(m, s)
            cache_durationToDisplay.put(durationString, str)
            return str
        }
        return "??:??:??"
    }
}