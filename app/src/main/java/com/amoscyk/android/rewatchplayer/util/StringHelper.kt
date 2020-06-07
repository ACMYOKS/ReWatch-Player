package com.amoscyk.android.rewatchplayer.util

import androidx.emoji.text.EmojiCompat
import com.amoscyk.android.rewatchplayer.ReWatchPlayerActivity
import com.amoscyk.android.rewatchplayer.ReWatchPlayerFragment
import com.amoscyk.android.rewatchplayer.rpApplication

fun CharSequence.withEmoji(): CharSequence {
    val emojiCompat = EmojiCompat.get()
    return if (emojiCompat.loadState == EmojiCompat.LOAD_STATE_SUCCEEDED) emojiCompat.process(this)
    else this
}

fun Long.formatReadableByteUnit(): String {
    var value = this.toDouble()
    if (value < 1000) {
        return "%.2fB".format(value)
    }
    value /= 1000
    if (value < 1000) {
        return "%.2fkB".format(value)
    }
    value /= 1000
    if (value < 1000) {
        return "%.2fMB".format(value)
    }
    value /= 1000
    if (value < 1000) {
        return "%.2fGB".format(value)
    }
    value /= 1000
    return "%.2fTB".format(value)
}