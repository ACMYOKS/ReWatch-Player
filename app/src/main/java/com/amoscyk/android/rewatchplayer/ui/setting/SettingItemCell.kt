package com.amoscyk.android.rewatchplayer.ui.setting

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.amoscyk.android.rewatchplayer.R
import com.amoscyk.android.rewatchplayer.util.dpToPx

open class SettingItemCell : LinearLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    protected val tvTitle: TextView = TextView(context, null, R.attr.textAppearanceBody1).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }
    protected val tvDescription: TextView = TextView(context, null, R.attr.textAppearanceCaption).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }
    protected val tvCurrentValue: TextView = TextView(context, null, R.attr.textAppearanceCaption).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setTextColor(ContextCompat.getColor(context, R.color.blue_800))
    }
    protected val childContainer: LinearLayout = LinearLayout(context).apply {
        layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        orientation = VERTICAL
        addView(tvTitle)
        addView(tvDescription)
        addView(tvCurrentValue)
    }

    protected var title = ""
    protected var description = ""
    protected var currentValue = ""

    init {
        initView()
    }

    private fun initView() {
        val outValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        setBackgroundResource(outValue.resourceId)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val hPadding = context.dpToPx(20f).toInt()
        val vPadding = context.dpToPx(14f).toInt()
        setPadding(hPadding, vPadding, hPadding, vPadding)
        isClickable = true
        isFocusable = true

        addView(childContainer)
        setTitle("")
        setDescription("")
        setCurrentValue("")
    }

    fun setTitle(s: CharSequence) {
        tvTitle.text = s
    }

    fun setDescription(s: CharSequence) {
        tvDescription.text = s
        setDescriptionVisible(s.isNotBlank())
    }

    fun setCurrentValue(s: CharSequence) {
        tvCurrentValue.text = s
        setCurrentValueVisible(s.isNotBlank())
    }

    fun setDescriptionVisible(isVisible: Boolean) {
        tvDescription.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    fun setCurrentValueVisible(isVisible: Boolean) {
        tvCurrentValue.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        tvTitle.isEnabled = enabled
        tvDescription.isEnabled = enabled
        tvCurrentValue.isEnabled = enabled
    }

}