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
