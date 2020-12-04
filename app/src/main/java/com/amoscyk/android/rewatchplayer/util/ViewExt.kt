package com.amoscyk.android.rewatchplayer.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
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

fun Menu.setMenuItemTintColor(@ColorInt color: Int) {
    this.children.forEach {
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

fun Activity.getContentView(): View = window.findViewById<View>(android.R.id.content)

fun Activity.showSystemUI() {
    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
            or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
}

fun Activity.hideSystemUI() {
    window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN)
    window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
            or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
}

@SuppressLint("SourceLockedOrientationActivity")
fun Activity.toggleLockedRotation() {
    when (resources.configuration.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        }
        Configuration.ORIENTATION_PORTRAIT -> {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        }
    }
}

fun View.isTouchEventInsideTarget(ev: MotionEvent): Boolean {
    return ev.x in left.toFloat()..right.toFloat() && ev.y in top.toFloat()..bottom.toFloat()
}