package com.amoscyk.android.rewatchplayer.ui.player

import android.content.Context
import android.view.View
import com.amoscyk.android.rewatchplayer.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

object PlayerBottomSheetDialogBuilder {
    fun createDialog(context: Context,
                     layoutId: Int = R.layout.bottom_sheet_dialog_user_option,
                     f: ((contentView: View) -> Unit)? = null) = BottomSheetDialog(context).apply {
        val contentView = layoutInflater.inflate(layoutId, null, false)
        setContentView(contentView)
        f?.invoke(contentView)
        BottomSheetBehavior.from(contentView.parent as View).state = BottomSheetBehavior.STATE_EXPANDED
    }
}