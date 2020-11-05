package com.amoscyk.android.rewatchplayer.ui.player

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.amoscyk.android.rewatchplayer.R
import kotlin.math.max
import kotlin.math.min

class PlaybackSpeedDialog2(context: Context) : AlertDialog(context) {

    private var mCustomView: View? = null
    private var mTvCurrentSpeed: TextView? = null
    private var mSeekBar: SeekBar? = null

    private var mCurrentMultiplier = 1f

    private var mPlaybackSpeedChangeListener: ((newSpeed: Float) -> Unit)? = null
    private var mAdvanceClickListener: DialogInterface.OnClickListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // set view before super.onCreate() since setupView is called on create in super class
        mCustomView =
            LayoutInflater.from(context).inflate(R.layout.dialog_playback_speed_option2, null)
        mSeekBar = mCustomView!!.findViewById(R.id.sb_speed)
        mTvCurrentSpeed = mCustomView!!.findViewById(R.id.tv_current_speed)
        mSeekBar!!.apply {
            // min = 0
            max = 19
            incrementProgressBy(1)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    updateCurrentSpeedText(progress)
                }
            })
            setPadding(0, 0, 0, 0)
        }
        setView(mCustomView)
        setButton(
            DialogInterface.BUTTON_POSITIVE,
            context.getString(R.string.confirm_text)
        ) { _, _ ->
            mCurrentMultiplier = toPlaybackValue(mSeekBar!!.progress)
            mPlaybackSpeedChangeListener?.invoke(mCurrentMultiplier)
        }
        setButton(
            DialogInterface.BUTTON_NEUTRAL,
            context.getString(R.string.player_dialog_speed_advance)
        ) { d, i ->
            mAdvanceClickListener?.onClick(d, i)
        }
        setButton(
            DialogInterface.BUTTON_NEGATIVE,
            context.getString(R.string.default_text)
        ) { _, _ -> }

        super.onCreate(savedInstanceState)

        getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener {
            mCurrentMultiplier = 1f
            setPlaybackMultiplier(mCurrentMultiplier)
        }
    }

    override fun show() {
        super.show()
        setPlaybackMultiplier(mCurrentMultiplier)
    }

    fun setOnPlaybackSpeedChangeListener(listener: ((newSpeed: Float) -> Unit)) {
        mPlaybackSpeedChangeListener = listener
    }

    fun setOnAdvanceClickListener(listener: DialogInterface.OnClickListener) {
        mAdvanceClickListener = listener
    }

    fun setPlaybackMultiplier(newValue: Float) {
        mCurrentMultiplier = min(max(newValue, minValue), maxValue)
        if (mCustomView != null) {
            mSeekBar?.progress = toSeekBarValue(mCurrentMultiplier)
            updateCurrentSpeedText(mSeekBar?.progress)
        }
    }

    private fun toPlaybackValue(seekBarValue: Int): Float {
        // SeekBar is zero based. Convert to 1.0 based
        return (seekBarValue + 1) / 10f
    }

    private fun toSeekBarValue(playbackValue: Float): Int {
        // SeekBar is zero based. Convert from 1.0 based
        return (playbackValue * 10 - 1).toInt()
    }

    private fun updateCurrentSpeedText(seekBarValue: Int?) {
        mTvCurrentSpeed?.text = seekBarValue?.let { "%.1fx".format(toPlaybackValue(it)) }
    }

    companion object {
        const val minValue = 0.1f
        const val maxValue = 2f
    }
}