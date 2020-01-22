package com.amoscyk.android.rewatchplayer.util

import android.util.Log

class TimeLogger(private val tag: String) {

    private var startTime = System.currentTimeMillis()
    private val messages = arrayListOf<Pair<Long, String>>()

    fun addKnot(message: String) {
        val currentTime = System.currentTimeMillis()
        messages.add(Pair(currentTime - startTime, message))
        startTime = currentTime
    }

    fun dumpToLog() {
        messages.forEach {
            Log.d(tag, "${it.second} : ${it.first}ms")
        }
        resetTimer()
    }

    fun resetTimer() {
        startTime = System.currentTimeMillis()
        messages.clear()
    }

}