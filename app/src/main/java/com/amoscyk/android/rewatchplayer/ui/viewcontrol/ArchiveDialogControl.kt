package com.amoscyk.android.rewatchplayer.ui.viewcontrol

data class ArchiveDialogControl(
    val availableTag: List<Int>,
    val selection: (vTag: Int?, aTag: Int?) -> Unit
)