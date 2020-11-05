package com.amoscyk.android.rewatchplayer.ui.player

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import com.amoscyk.android.rewatchplayer.R

class PlaybackSpeedAdvanceDialog(context: Context) : AlertDialog(context) {
    private var mCustomView: View? = null
    private var mTvCurrentSpeed: TextView? = null
    private var mBtnPlus1: AppCompatButton? = null
    private var mBtnMinus1: AppCompatButton? = null
    private var mBtnPlus01: AppCompatButton? = null
    private var mBtnMinus01: AppCompatButton? = null
    private var mBtnPlus005: AppCompatButton? = null
    private var mBtnMinus005: AppCompatButton? = null

    private var mCurrentMultiplier = 1f

    private var mPlaybackSpeedChangeListener: ((newSpeed: Float) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // set view before super.onCreate() since setupView is called on create in super class
        mCustomView =
            LayoutInflater.from(context)
                .inflate(R.layout.dialog_playback_speed_option_advance, null)

        mTvCurrentSpeed = mCustomView!!.findViewById(R.id.tv_current_speed)
        mBtnPlus1 = mCustomView!!.findViewById(R.id.btn_plus_1)
        mBtnMinus1 = mCustomView!!.findViewById(R.id.btn_minus_1)
        mBtnPlus01 = mCustomView!!.findViewById(R.id.btn_plus_0_1)
        mBtnMinus01 = mCustomView!!.findViewById(R.id.btn_minus_0_1)
        mBtnPlus005 = mCustomView!!.findViewById(R.id.btn_plus_0_0_5)
        mBtnMinus005 = mCustomView!!.findViewById(R.id.btn_minus_0_0_5)

        mBtnPlus1?.setOnClickListener { updateMultiplier(1f) }
        mBtnMinus1?.setOnClickListener { updateMultiplier(-1f) }
        mBtnPlus01?.setOnClickListener { updateMultiplier(.1f) }
        mBtnMinus01?.setOnClickListener { updateMultiplier(-.1f) }
        mBtnPlus005?.setOnClickListener { updateMultiplier(.05f) }
        mBtnMinus005?.setOnClickListener { updateMultiplier(-.05f) }

        setView(mCustomView)
        setButton(
            DialogInterface.BUTTON_POSITIVE,
            context.getString(R.string.confirm_text)
        ) { _, _ ->
            mPlaybackSpeedChangeListener?.invoke(mCurrentMultiplier)
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
        updateViews()
    }

    fun setPlaybackMultiplier(newValue: Float) {
        if (newValue in (minValue - threshold)..(maxValue + threshold)) {
            mCurrentMultiplier = newValue
            updateViews()
        }
    }

    fun setOnPlaybackSpeedChangeListener(listener: (newSpeed: Float) -> Unit) {
        mPlaybackSpeedChangeListener = listener
    }

    private fun updateMultiplier(delta: Float) {
        if (mCurrentMultiplier + delta in (minValue - threshold)..(maxValue + threshold)) {
            mCurrentMultiplier += delta
            updateViews()
        }
    }

    private fun updateViews() {
        if (mCustomView != null) {
            mTvCurrentSpeed?.text = "%.2fx".format(mCurrentMultiplier)
            mBtnPlus1?.isEnabled = mCurrentMultiplier + 1 <= maxValue + threshold
            mBtnMinus1?.isEnabled = mCurrentMultiplier - 1 >= minValue - threshold
            mBtnPlus01?.isEnabled = mCurrentMultiplier + 0.1 <= maxValue + threshold
            mBtnMinus01?.isEnabled = mCurrentMultiplier - 0.1 >= minValue - threshold
            mBtnPlus005?.isEnabled = mCurrentMultiplier + 0.05 <= maxValue + threshold
            mBtnMinus005?.isEnabled = mCurrentMultiplier - 0.05 >= minValue - threshold
        }
    }

    companion object {
        private const val threshold = 0.0001f
        const val minValue = 0.1f
        const val maxValue = 10f
    }
}