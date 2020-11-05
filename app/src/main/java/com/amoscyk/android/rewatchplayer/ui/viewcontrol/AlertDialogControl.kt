package com.amoscyk.android.rewatchplayer.ui.viewcontrol

import android.content.Context
import androidx.appcompat.app.AlertDialog

data class AlertDialogControl(
    val title: String?, val message: String?,
    val cancelable: Boolean,
    val positiveAction: Action? = null,
    val negativeAction: Action? = null,
    val neutralAction: Action? = null
) {
    data class Action(val title: String, val action: () -> Unit)

    fun getBuilder(context: Context): AlertDialog.Builder {
        val builder = AlertDialog.Builder(context)
        title?.let { builder.setTitle(it) }
        message?.let { builder.setMessage(it) }
        builder.setCancelable(cancelable)
        positiveAction?.let {
            builder.setPositiveButton(it.title) { _, _ -> it.action() }
        }
        negativeAction?.let {
            builder.setNegativeButton(it.title) { _, _ -> it.action() }
        }
        neutralAction?.let {
            builder.setNeutralButton(it.title) { _, _ -> it.action() }
        }
        return builder
    }
}