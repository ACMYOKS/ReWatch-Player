package com.amoscyk.android.rewatchplayer.ui.player

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.util.YouTubeStreamFormatCode

class ArchiveOptionDialog2(context: Context, private val itags: List<Int>) : AlertDialog(context) {

    private var mCustomView: View? = null
    private var mSpinner: Spinner? = null
    private var mListener: ((itag: Int) -> Unit?)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        mCustomView =
            LayoutInflater.from(context).inflate(R.layout.dialog_archive_option2, null, false)
        mSpinner = mCustomView!!.findViewById(R.id.spinner_quality)
        mSpinner!!.apply {
            adapter = ArrayAdapter<String>(context,
                android.R.layout.simple_spinner_dropdown_item, itags.map {
                    YouTubeStreamFormatCode.MUX_FORMAT_MAP[it]
                        ?: YouTubeStreamFormatCode.ADAPTIVE_VIDEO_FORMAT_MAP[it].orEmpty()
                }
            )
        }
        setCancelable(false)
        setView(mCustomView)
        setTitle(R.string.player_archive_option_title)
        setButton(
            DialogInterface.BUTTON_POSITIVE,
            context.getString(R.string.confirm_text)
        ) { _, _ ->
            mSpinner?.selectedItemPosition?.let {
                mListener?.invoke(itags[it])        // shift 1 position
            }
        }
        setButton(
            DialogInterface.BUTTON_NEGATIVE,
            context.getString(R.string.cancel_text)
        ) { _, _ -> }
        super.onCreate(savedInstanceState)
    }

    fun setListener(l: (itag: Int) -> Unit) {
        mListener = l
    }

}