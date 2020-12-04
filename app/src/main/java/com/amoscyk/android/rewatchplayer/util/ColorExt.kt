package com.amoscyk.android.rewatchplayer.util

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

@ColorInt
fun Context.getColorFromAttr(@AttrRes attrColor: Int) =
    TypedValue().let {
        theme.resolveAttribute(attrColor, it, true)
        it.data
    }
