package com.amoscyk.android.rewatchplayer.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class CommonListDecoration(
    private val interItemSpace: Int
): RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        with (outRect) {
            if (parent.getChildAdapterPosition(view) == 0) {
                top = interItemSpace
            }
            left = interItemSpace
            bottom = interItemSpace
            right = interItemSpace
        }
    }

}