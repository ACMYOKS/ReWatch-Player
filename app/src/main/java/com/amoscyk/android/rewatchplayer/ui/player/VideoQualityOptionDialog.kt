package com.amoscyk.android.rewatchplayer.ui.player

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.util.YouTubeStreamFormatCode

class VideoQualityOptionDialog(context: Context) : AlertDialog(context) {

    private var mCustomView: View? = null
    private var mAudioSpinner: Spinner? = null
    private var mVideoSpinner: Spinner? = null
    private val mAudioTags = arrayListOf<Int>()
    private val mVideoTags = arrayListOf<Int>()
    private val mAudioStrings = arrayListOf<String>()
    private val mVideoStrings = arrayListOf<String>()

    private var mOnQualityOptionSelectedListener: OnQualityOptionSelectedListener? = null

    private var mPrevAudioPos = 0
    private var mPrevVideoPos = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        mCustomView = LayoutInflater.from(context).inflate(R.layout.dialog_archive_option, null, false)
        mAudioSpinner = mCustomView!!.findViewById(R.id.spinner_audio_quality)
        mVideoSpinner = mCustomView!!.findViewById(R.id.spinner_video_quality)
        refreshAudioSpinner()
        refreshVideoSpinner()
        setView(mCustomView)
        setTitle(R.string.player_option_resolution)
        setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.confirm_text))  { _, _ ->
            mOnQualityOptionSelectedListener?.onQualityOptionSelected(
                mVideoTags[mVideoSpinner!!.selectedItemPosition],
                mAudioTags[mAudioSpinner!!.selectedItemPosition]
            )
            mPrevAudioPos = mAudioSpinner!!.selectedItemPosition
            mPrevVideoPos = mVideoSpinner!!.selectedItemPosition
        }
        setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.cancel_text)) { _, _ ->
            cancel()
        }
        setOnCancelListener {
            mAudioSpinner?.setSelection(mPrevAudioPos)
            mVideoSpinner?.setSelection(mPrevVideoPos)
        }
        mAudioSpinner?.setSelection(mPrevAudioPos)
        mVideoSpinner?.setSelection(mPrevVideoPos)
        super.onCreate(savedInstanceState)
    }

    fun setVideoTags(tags: List<Int>) {
        mVideoTags.apply {
            clear()
            addAll(tags)
            mVideoStrings.clear()
            mVideoStrings.addAll(mapNotNull { YouTubeStreamFormatCode.FORMAT_CODES[it]?.resolution })
        }
        mPrevVideoPos = 0
        refreshVideoSpinner()
    }

    fun setAudioVTags(tags: List<Int>) {
        mAudioTags.apply {
            clear()
            addAll(tags)
            mAudioStrings.clear()
            mAudioStrings.addAll(mapNotNull { YouTubeStreamFormatCode.FORMAT_CODES[it]?.bitrate })
        }
        mPrevAudioPos = 0
        refreshAudioSpinner()
    }

    fun setCurrentVTag(vTag: Int) {
        mVideoTags.indexOf(vTag).let {
            if (it != -1) {
                mVideoSpinner?.setSelection(it)
                mPrevVideoPos = it
            }
        }
    }

    fun setCurrentATag(aTag: Int) {
        mAudioTags.indexOf(aTag).let {
            if (it != -1) {
                mAudioSpinner?.setSelection(it)
                mPrevAudioPos = it
            }
        }
    }

    fun setOnQualityOptionSelectedListener(listener: OnQualityOptionSelectedListener) {
        mOnQualityOptionSelectedListener = listener
    }

    private fun refreshAudioSpinner() {
        mAudioSpinner?.adapter = ArrayAdapter<String>(context,
            android.R.layout.simple_spinner_dropdown_item, mAudioStrings)
    }

    private fun refreshVideoSpinner() {
        mVideoSpinner?.adapter = ArrayAdapter<String>(context,
            android.R.layout.simple_spinner_dropdown_item, mVideoStrings)
    }

    interface OnQualityOptionSelectedListener {
        fun onQualityOptionSelected(vTag: Int, aTag: Int)
    }

}
