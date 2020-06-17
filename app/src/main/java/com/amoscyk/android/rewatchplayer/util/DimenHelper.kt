package com.amoscyk.android.rewatchplayer.util

import android.content.Context
import android.util.TypedValue
import androidx.fragment.app.Fragment

fun Context.dpToPx(dp: Float): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)

fun Context.spToPx(sp: Float): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)

fun Fragment.dpToPx(dp: Float): Float = requireActivity().dpToPx(dp)

fun Fragment.spToPx(sp: Float): Float = requireActivity().spToPx(sp)