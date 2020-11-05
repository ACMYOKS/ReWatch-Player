package com.amoscyk.android.rewatchplayer.ui.viewcontrol

data class SnackbarControl(
    val title: String,
    val action: Action? = null,
    val duration: Duration = Duration.SHORT
) {
    enum class Duration { SHORT, LONG, FOREVER }
    data class Action(val title: String, val action: () -> Unit)
}