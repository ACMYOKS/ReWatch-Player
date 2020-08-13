package com.amoscyk.android.rewatchplayer.util

object ActivityIdStack {
    private val activityIdStack = arrayListOf<String>()

    fun addId(id: String) {
        activityIdStack.add(id)
    }

    fun removeId(id: String) {
        activityIdStack.remove(id)
    }

    fun bringIdToTop(id: String) {
        if (activityIdStack.remove(id)) {
            activityIdStack.add(id)
        }
    }

    fun isActivityOnTop(id: String): Boolean {
        if (activityIdStack.size == 0) return false
        return activityIdStack.last() == id
    }

}