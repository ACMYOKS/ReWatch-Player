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

class PlaybackSpeedDialog(context: Context) : AlertDialog(context) {

    private var mCustomView: View? = null
    private var mTvCurrentSpeed: TextView? = null
    private var mSeekBar: SeekBar? = null

    private var mPlaybackSpeedChangeListener: OnPlaybackSpeedChangeListener? = null

    private var storedPlaybackMultiplier = 1f
    private var currentPlaybackMultiplier = 1f

    override fun onCreate(savedInstanceState: Bundle?) {
        // set view before super.onCreate() since setupView is called on create in super class
        mCustomView = LayoutInflater.from(context).inflate(R.layout.dialog_playback_speed_option, null)
        mSeekBar = mCustomView!!.findViewById(R.id.sb_speed)
        mTvCurrentSpeed = mCustomView!!.findViewById(R.id.tv_current_speed)
        mSeekBar!!.apply {
            max = 15
            incrementProgressBy(1)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {

                }

                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    currentPlaybackMultiplier =
                        min(max(((mSeekBar?.progress ?: 5) + 5) / 10f,
                            minValue
                        ),
                            maxValue
                        )
                    updateCurrentSpeedText()
                }
            })
            setPlaybackMultiplier(currentPlaybackMultiplier)
        }
        setView(mCustomView)
        setTitle(context.getString(R.string.player_dialog_speed_title))
        setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.confirm_text))  { _, _ ->
            mPlaybackSpeedChangeListener?.onPlaybackSpeedChange(currentPlaybackMultiplier)
            storedPlaybackMultiplier = currentPlaybackMultiplier
        }
        setButton(DialogInterface.BUTTON_NEUTRAL, context.getString(R.string.default_text)) { _, _ -> }
        setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.cancel_text)) { _, _ ->
            setPlaybackMultiplier(storedPlaybackMultiplier)
        }

        super.onCreate(savedInstanceState)

        getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener {
            setPlaybackMultiplier(1f)
        }
    }

    override fun show() {
        storedPlaybackMultiplier = currentPlaybackMultiplier
        setPlaybackMultiplier(currentPlaybackMultiplier)
        super.show()
    }

    fun setOnPlaybackSpeedChangeListener(listener: OnPlaybackSpeedChangeListener) {
        mPlaybackSpeedChangeListener = listener
    }

    fun setPlaybackMultiplier(newValue: Float) {
        currentPlaybackMultiplier = min(max(newValue, minValue), maxValue)
        mSeekBar?.progress = (currentPlaybackMultiplier * 10).toInt() - 5
        updateCurrentSpeedText()
    }

    private fun updateCurrentSpeedText() {
        mTvCurrentSpeed?.text = "%.1fx".format(currentPlaybackMultiplier)
    }

    interface OnPlaybackSpeedChangeListener {
        fun onPlaybackSpeedChange(newPlaybackSpeed: Float)
    }

    companion object {
        const val minValue = 0.5f
        const val maxValue = 2f
    }
}