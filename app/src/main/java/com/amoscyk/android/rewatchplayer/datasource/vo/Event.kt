package com.amoscyk.android.rewatchplayer.datasource.vo

import androidx.lifecycle.MutableLiveData
import java.util.concurrent.atomic.AtomicBoolean

class Event<out T>(private val content: T) {
    private val handled = AtomicBoolean(false)

    fun getContentIfNotHandled(handler: (content: T)-> Unit) {
        if (!handled.get()) {
            handled.set(true)
            handler(content)
        }
    }

    fun peekContent() = content
}

/* common function for showing loading UI when processing */
inline fun MutableLiveData<Event<Boolean>>.loading(action: () -> Unit) {
    value = Event(true)
    action()
    value = Event(false)
}