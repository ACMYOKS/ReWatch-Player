package com.amoscyk.android.rewatchplayer.ui

import android.app.DownloadManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.datasource.vo.cloud.UpdateResponse
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle

class AppUpdateDialog(context: Context,
                      private val updateResp: UpdateResponse,
                      private val onDownloadListener: (() -> Unit)?
) : AlertDialog(context) {
    override fun onCreate(savedInstanceState: Bundle?) {
        val customView = LayoutInflater.from(context).inflate(R.layout.app_update_dialog, null)
        val tvTitle = customView.findViewById<TextView>(R.id.tv_title)
        val tvInfo = customView.findViewById<TextView>(R.id.tv_info)
        val svContainer = customView.findViewById<ScrollView>(R.id.sv_container)
        val ivDone = customView.findViewById<ImageView>(R.id.iv_done)
        val target = updateResp.target
        if (target == null) {
            tvTitle.setText(R.string.update_no_update)
            svContainer.visibility = View.GONE
            ivDone.visibility = View.VISIBLE
            setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.update_confirm)) { _, _ -> }
        } else {
            tvTitle.setText(R.string.update_new_version)
            svContainer.visibility = View.VISIBLE
            ivDone.visibility = View.GONE
            val releaseInfo = target.releaseNote.joinToString("\n") { note ->
                note.name + ": \n" + note.details.joinToString("\n") { "- $it" }
            }
            val info = context.getString(R.string.update_info_template,
                target.version,
                runCatching {
                    Instant.parse(target.releaseDate).atZone(ZoneId.systemDefault()).format(
                        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM))
                }.getOrElse { target.releaseDate },
                releaseInfo)
            tvInfo.text = info
            setButton(DialogInterface.BUTTON_POSITIVE, context.getText(R.string.update_download)) { _, _ ->
                onDownloadListener?.invoke()
            }
            setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.update_not_now)) { _, _ -> }
        }
        setView(customView)
        setCancelable(false)
        super.onCreate(savedInstanceState)
    }

}