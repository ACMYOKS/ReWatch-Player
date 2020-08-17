package com.amoscyk.android.rewatchplayer.ui

import android.view.View
import androidx.viewpager.widget.ViewPager


class NoPageTransformer : ViewPager.PageTransformer {
    override fun transformPage(view: View, position: Float) {
        if (position == 0f) {
            view.alpha = 1f
            view.visibility = View.VISIBLE
        } else {
            view.alpha = 0f
            view.visibility = View.GONE
        }
    }
}