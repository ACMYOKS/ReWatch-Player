package com.amoscyk.android.rewatchplayer.ui.setting

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.SwitchCompat
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.util.dpToPx

typealias CheckStateObserver = ((isChecked: Boolean) -> Unit)

class SettingSwitchItemCell : SettingItemCell {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val switch: SwitchCompat = SwitchCompat(context).apply {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            marginStart = context.dpToPx(8f).toInt()
        }
    }

    private var textOn: CharSequence = context.getString(R.string.settings_on_text)
    private var textOff: CharSequence = context.getString(R.string.settings_off_text)
    private var checkStateObserver: CheckStateObserver? = null

    init {
        addView(switch)
        switch.setOnCheckedChangeListener { buttonView, isChecked ->
            setCurrentValue(if (isChecked) textOn else textOff)
            checkStateObserver?.invoke(isChecked)
        }
    }

    fun isChecked() = switch.isChecked

    fun toggleSelection() {
        switch.isChecked = !switch.isChecked
    }

    fun setCheckStateObserver(o: CheckStateObserver?) {
        checkStateObserver = o
    }

    fun setChecked(isChecked: Boolean) {
        if (switch.isChecked == isChecked) {
            setCurrentValue(if (isChecked) textOn else textOff)
            checkStateObserver?.invoke(isChecked)
        } else {
            switch.isChecked = isChecked
        }
    }

    fun setTextOn(s: CharSequence) {
        this.textOn = s
    }

    fun setTextOff(s: CharSequence) {
        this.textOff = s
    }

    fun setSwitchVisibility(visible: Boolean) {
        switch.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        switch.isEnabled = enabled
    }
}