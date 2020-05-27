package com.amoscyk.android.rewatchplayer.util

import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.annotation.ColorInt
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.core.view.children

fun Toolbar.setMenuItemTintColor(@ColorInt color: Int) {
    menu?.children?.forEach {
        it.icon?.setTint(color)
        it.title = SpannableString(it.title).apply { setSpan(ForegroundColorSpan(color), 0, length, 0) }
    }
}

fun Toolbar.setForegroundItemColor(@ColorInt color: Int) {
    setTitleTextColor(color)
    setSubtitleTextColor(color)
    val colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(color, BlendModeCompat.SRC_ATOP)
    collapseIcon?.colorFilter = colorFilter
    navigationIcon?.colorFilter = colorFilter
    overflowIcon?.colorFilter = colorFilter
    setMenuItemTintColor(color)
}