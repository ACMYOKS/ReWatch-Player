package com.amoscyk.android.rewatchplayer.ui

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.amoscyk.android.rewatchplayer.R

object ProgressDialogUtil {
    fun create(context: Context): AlertDialog =
        AlertDialog.Builder(context, R.style.AppProgressDialog)
            .setView(R.layout.progress_dialog_view)
            .setCancelable(false)
            .create()
}