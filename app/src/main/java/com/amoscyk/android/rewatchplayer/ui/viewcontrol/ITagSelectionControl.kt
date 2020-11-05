package com.amoscyk.android.rewatchplayer.ui.viewcontrol

data class ITagSelectionControl(
    val availableTag: List<Int>,        // no need to include audio tag, always use the best one
    val selectedTag: Int
)