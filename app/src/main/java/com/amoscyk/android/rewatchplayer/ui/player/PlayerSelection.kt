package com.amoscyk.android.rewatchplayer.ui.player

data class PlayerSelection<T: SelectableItemWithTitle>(
    var item: T,
    var selected: Boolean
)

interface SelectableItemWithTitle {
    fun getTitle(): String
}