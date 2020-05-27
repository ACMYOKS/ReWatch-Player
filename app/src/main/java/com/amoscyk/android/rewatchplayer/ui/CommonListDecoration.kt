package com.amoscyk.android.rewatchplayer.ui

import android.graphics.Rect
import android.view.View
import androidx.annotation.Px
import androidx.recyclerview.widget.RecyclerView

class CommonListDecoration(
    @Px private val verticalSpace: Int,
    @Px private val horizontalSpace: Int
): RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        with (outRect) {
            if (parent.getChildAdapterPosition(view) == 0) {
                top = verticalSpace
            }
            left = horizontalSpace
            bottom = verticalSpace
            right = horizontalSpace
        }
    }

}